package com.g3.soundify_musicplayer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.ui.main.MainActivity;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;

/**
 * Service quản lý ExoPlayer và duy trì playback state toàn cục
 * Chạy như foreground service để có thể phát nhạc khi app ở background
 */
public class MediaPlaybackService extends Service {

    /**
     * Interface để giao tiếp hai chiều với MediaPlayerRepository
     * Service sẽ báo cáo trạng thái thực tế từ ExoPlayer về Repository
     */
    public interface PlaybackStateListener {
        /**
         * Báo cáo khi trạng thái phát/dừng thay đổi
         */
        void onPlaybackStateChanged(boolean isPlaying);

        /**
         * Báo cáo vị trí và duration hiện tại (gọi liên tục khi đang phát)
         */
        void onPositionChanged(long currentPosition, long duration);

        /**
         * Báo cáo khi bài hát thay đổi
         */
        void onSongChanged(Song song, User artist);

        /**
         * Báo cáo khi bài hát kết thúc (để tự động next)
         */
        void onSongCompleted();

        /**
         * Báo cáo khi có lỗi phát nhạc
         */
        void onPlayerError(String error);
    }
    
    private static final String CHANNEL_ID = "MediaPlaybackChannel";
    private static final int NOTIFICATION_ID = 1;
    
    // Binder để communicate với clients
    private final IBinder binder = new MediaPlaybackBinder();
    
    // ExoPlayer instance
    private ExoPlayer exoPlayer;

    // Main thread handler for ExoPlayer operations
    private Handler mainHandler;

    // Progress update handler
    private Handler progressHandler;
    private Runnable progressRunnable;

    // Current playback state
    private Song currentSong;
    private User currentArtist;
    private boolean isServiceStarted = false;

    // FIXED: Giao tiếp hai chiều - Repository sẽ lắng nghe Service
    private PlaybackStateListener playbackStateListener;
    
    public class MediaPlaybackBinder extends Binder {
        public MediaPlaybackService getService() {
            return MediaPlaybackService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        progressHandler = new Handler(Looper.getMainLooper());
        initializePlayer();
        createNotificationChannel();
    }
    
    /**
     * Khởi tạo ExoPlayer
     */
    private void initializePlayer() {
        exoPlayer = new ExoPlayer.Builder(this).build();
        
        // Lắng nghe các sự kiện từ ExoPlayer
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                boolean isPlaying = playbackState == Player.STATE_READY && exoPlayer.getPlayWhenReady();

                // Notify UI about state change
                if (playbackStateListener != null) {
                    playbackStateListener.onPlaybackStateChanged(isPlaying);
                }

                // Start/stop progress updates
                if (isPlaying) {
                    startProgressUpdates();
                } else {
                    stopProgressUpdates();
                }

                updateNotification();
            }

            @Override
            public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                boolean isPlaying = playWhenReady && exoPlayer.getPlaybackState() == Player.STATE_READY;

                if (playbackStateListener != null) {
                    playbackStateListener.onPlaybackStateChanged(isPlaying);
                }

                // Start/stop progress updates
                if (isPlaying) {
                    startProgressUpdates();
                } else {
                    stopProgressUpdates();
                }

                updateNotification();
            }
        });
    }
    
    /**
     * Phát bài hát mới - LUÔN restart từ đầu
     * Đảm bảo tất cả ExoPlayer operations chạy trên main thread
     */
    public void playSong(Song song, User artist) {
        if (song == null || song.getAudioUrl() == null) {
            return;
        }

        boolean isSameSong = currentSong != null && currentSong.getId() == song.getId();
        currentSong = song;
        currentArtist = artist;

        // Đảm bảo tất cả ExoPlayer operations chạy trên main thread
        mainHandler.post(() -> {
            try {
                // Tạo MediaItem từ URL
                MediaItem mediaItem = MediaItem.fromUri(song.getAudioUrl());

                // QUAN TRỌNG: Luôn stop và reset trước khi phát bài mới/cũ
                exoPlayer.stop();
                exoPlayer.clearMediaItems();

                // Set media item và bắt đầu phát từ đầu
                exoPlayer.setMediaItem(mediaItem);
                exoPlayer.prepare();
                exoPlayer.seekTo(0); // Đảm bảo bắt đầu từ đầu
                exoPlayer.setPlayWhenReady(true);

                // Start foreground service với notification (chỉ khi cần)
                startForegroundService();

                // Thông báo UI về bài hát mới (luôn gọi để refresh UI)
                if (playbackStateListener != null) {
                    playbackStateListener.onSongChanged(song, artist);
                } else {
                    android.util.Log.w("MediaPlaybackService", "playbackStateListener is NULL!");
                }
            } catch (Exception e) {
                android.util.Log.e("MediaPlaybackService", "Error playing song", e);
            }
        });
    }
    
    /**
     * Play/Pause toggle
     * Đảm bảo ExoPlayer operations chạy trên main thread
     */
    public void togglePlayPause() {
        mainHandler.post(() -> {
            if (exoPlayer.isPlaying()) {
                exoPlayer.pause();
            } else {
                exoPlayer.play();
            }
        });
    }

    /**
     * Kiểm tra xem có đang phát bài hát này không
     * Thread-safe method
     */
    public boolean isPlayingSong(long songId) {
        if (currentSong == null || currentSong.getId() != songId) {
            return false;
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            return exoPlayer.isPlaying();
        } else {
            return false; // Safe default for background threads
        }
    }

    /**
     * Kiểm tra xem bài hát này có phải là current song không (dù đang phát hay pause)
     */
    public boolean isCurrentSong(long songId) {
        return currentSong != null && currentSong.getId() == songId;
    }

    /**
     * Lấy current song
     */
    public Song getCurrentSong() {
        return currentSong;
    }

    /**
     * Lấy current artist
     */
    public User getCurrentArtist() {
        return currentArtist;
    }
    
    /**
     * Pause playback
     * Đảm bảo ExoPlayer operations chạy trên main thread
     */
    public void pause() {
        mainHandler.post(() -> exoPlayer.pause());
    }

    /**
     * Resume playback
     * Đảm bảo ExoPlayer operations chạy trên main thread
     */
    public void play() {
        mainHandler.post(() -> exoPlayer.play());
    }

    /**
     * Stop playback
     * Đảm bảo ExoPlayer operations chạy trên main thread
     */
    public void stop() {
        mainHandler.post(() -> exoPlayer.stop());
    }
    
    /**
     * Seek đến vị trí cụ thể
     * Đảm bảo ExoPlayer operations chạy trên main thread
     */
    public void seekTo(long positionMs) {
        mainHandler.post(() -> {
            try {
                long duration = exoPlayer.getDuration();

                // Đảm bảo position hợp lệ
                if (positionMs >= 0 && (duration <= 0 || positionMs <= duration)) {
                    boolean wasPlaying = exoPlayer.isPlaying();
                    exoPlayer.seekTo(positionMs);

                    // Đảm bảo playback tiếp tục nếu đang phát
                    if (wasPlaying) {
                        exoPlayer.setPlayWhenReady(true);
                    }
                } else {
                    android.util.Log.w("MediaPlaybackService", "Invalid seek position: " + positionMs);
                }
            } catch (Exception e) {
                android.util.Log.e("MediaPlaybackService", "Error during seek", e);
            }
        });
    }
    
    /**
     * Lấy vị trí hiện tại
     * Thread-safe getter
     */
    public long getCurrentPosition() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return exoPlayer.getCurrentPosition();
        } else {
            // Return cached value or 0 if called from background thread
            return 0;
        }
    }

    /**
     * Lấy tổng thời lượng
     * Thread-safe getter
     */
    public long getDuration() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return exoPlayer.getDuration();
        } else {
            // Return cached value or 0 if called from background thread
            return 0;
        }
    }

    /**
     * Kiểm tra có đang phát không
     * Thread-safe getter
     */
    public boolean isPlaying() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return exoPlayer.isPlaying();
        } else {
            // Return false if called from background thread
            return false;
        }
    }
    
    // XÓA duplicate methods - đã có ở lines 170-179
    
    // REMOVED: Duplicate setPlaybackStateListener() method - using enhanced version below
    
    /**
     * Start foreground service với notification
     */
    private void startForegroundService() {
        try {
            if (!isServiceStarted) {
                Notification notification = createNotification();
                startForeground(NOTIFICATION_ID, notification);
                isServiceStarted = true;
                android.util.Log.d("MediaPlaybackService", "Foreground service started");
            } else {
                updateNotification();
            }
        } catch (Exception e) {
            android.util.Log.e("MediaPlaybackService", "Error starting foreground service", e);
            // Fallback: chỉ update notification mà không start foreground
            updateNotification();
        }
    }
    
    /**
     * Tạo notification channel (Android 8.0+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Hiển thị thông tin bài hát đang phát");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Tạo notification cho media playback
     */
    private Notification createNotification() {
        // Đảm bảo notification channel được tạo trước
        createNotificationChannel();

        // Intent để mở app khi tap notification
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = currentSong != null ? currentSong.getTitle() : "Đang phát nhạc";
        String artist = currentArtist != null ? currentArtist.getDisplayName() : "Soundify Music Player";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }
    
    /**
     * Cập nhật notification
     */
    private void updateNotification() {
        if (isServiceStarted) {
            Notification notification = createNotification();
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, notification);
            }
        }
    }
    
    /**
     * Start progress updates
     */
    private void startProgressUpdates() {
        stopProgressUpdates(); // Stop any existing updates

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (exoPlayer != null && playbackStateListener != null) {
                    long currentPosition = exoPlayer.getCurrentPosition();
                    long duration = exoPlayer.getDuration();
                    playbackStateListener.onPositionChanged(currentPosition, duration);
                }

                // Schedule next update
                if (progressRunnable != null) {
                    progressHandler.postDelayed(this, 1000); // Update every second
                }
            }
        };

        progressHandler.post(progressRunnable);
    }

    /**
     * Stop progress updates
     */
    private void stopProgressUpdates() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }

    // ========== GIAO TIẾP HAI CHIỀU - PUBLIC METHODS ==========

    /**
     * Đăng ký listener để nhận trạng thái thực tế từ ExoPlayer
     * QUAN TRỌNG: Đây là cầu nối để Repository nhận được trạng thái thật
     */
    public void setPlaybackStateListener(PlaybackStateListener listener) {
        this.playbackStateListener = listener;
        android.util.Log.d("MediaPlaybackService", "✅ PlaybackStateListener registered: " +
            (listener != null ? listener.getClass().getSimpleName() : "NULL"));

        // Ngay lập tức báo cáo trạng thái hiện tại nếu có - THREAD-SAFE
        if (listener != null && exoPlayer != null) {
            // Đảm bảo ExoPlayer access từ Main Thread
            mainHandler.post(() -> {
                if (exoPlayer != null) {
                    listener.onPlaybackStateChanged(exoPlayer.isPlaying());
                    if (currentSong != null) {
                        listener.onSongChanged(currentSong, currentArtist);
                    }
                    listener.onPositionChanged(exoPlayer.getCurrentPosition(), exoPlayer.getDuration());
                }
            });
        }
    }

    /**
     * Resume playback - THREAD-SAFE
     * resume(): Chỉ gọi player.play() để tiếp tục từ vị trí hiện tại
     * playSong(): Bắt đầu bài hát mới từ đầu
     * FIXED: Đảm bảo ExoPlayer operations chạy trên Main Thread
     */
    public void resume() {
        // Đảm bảo ExoPlayer operations chạy trên Main Thread
        mainHandler.post(() -> {
            if (exoPlayer != null) {
                exoPlayer.play();
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopProgressUpdates();
        if (exoPlayer != null) {
            exoPlayer.release();
        }
        isServiceStarted = false;
    }
}
