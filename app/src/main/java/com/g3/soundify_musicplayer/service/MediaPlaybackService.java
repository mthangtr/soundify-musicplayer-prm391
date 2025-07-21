package com.g3.soundify_musicplayer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.Activity.MainActivity;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;

/**
 * Service quản lý ExoPlayer và duy trì playback state toàn cục
 * Chạy như foreground service để có thể phát nhạc khi app ở background
 */
public class MediaPlaybackService extends Service {
    
    private static final String CHANNEL_ID = "MediaPlaybackChannel";
    private static final int NOTIFICATION_ID = 1;
    
    // Binder để communicate với clients
    private final IBinder binder = new MediaPlaybackBinder();
    
    // ExoPlayer instance
    private ExoPlayer exoPlayer;
    
    // Current playback state
    private Song currentSong;
    private User currentArtist;
    private boolean isServiceStarted = false;
    
    // Listener interface cho UI updates
    public interface PlaybackStateListener {
        void onSongChanged(Song song, User artist);
        void onPlaybackStateChanged(boolean isPlaying);
        void onProgressChanged(long currentPosition, long duration);
    }
    
    private PlaybackStateListener playbackStateListener;
    
    public class MediaPlaybackBinder extends Binder {
        public MediaPlaybackService getService() {
            return MediaPlaybackService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
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
                // Thông báo UI về thay đổi trạng thái
                if (playbackStateListener != null) {
                    boolean isPlaying = playbackState == Player.STATE_READY && exoPlayer.getPlayWhenReady();
                    playbackStateListener.onPlaybackStateChanged(isPlaying);
                }
                
                // Cập nhật notification
                updateNotification();
            }
            
            @Override
            public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                if (playbackStateListener != null) {
                    playbackStateListener.onPlaybackStateChanged(playWhenReady && exoPlayer.getPlaybackState() == Player.STATE_READY);
                }
                updateNotification();
            }
        });
    }
    
    /**
     * Phát bài hát mới - LUÔN restart từ đầu
     */
    public void playSong(Song song, User artist) {
        if (song == null || song.getAudioUrl() == null) {
            android.util.Log.d("MediaPlaybackService", "Invalid song or URL");
            return;
        }

        boolean isSameSong = currentSong != null && currentSong.getId() == song.getId();
        currentSong = song;
        currentArtist = artist;

        android.util.Log.d("MediaPlaybackService", "Playing song: " + song.getTitle() +
            " (Same song: " + isSameSong + ")");

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
                android.util.Log.d("MediaPlaybackService", "Calling onSongChanged listener");
                playbackStateListener.onSongChanged(song, artist);
            } else {
                android.util.Log.w("MediaPlaybackService", "playbackStateListener is NULL!");
            }

            android.util.Log.d("MediaPlaybackService", "Song setup completed - playing from start");
        } catch (Exception e) {
            android.util.Log.e("MediaPlaybackService", "Error playing song", e);
        }
    }
    
    /**
     * Play/Pause toggle
     */
    public void togglePlayPause() {
        if (exoPlayer.isPlaying()) {
            exoPlayer.pause();
        } else {
            exoPlayer.play();
        }
    }

    /**
     * Kiểm tra xem có đang phát bài hát này không
     */
    public boolean isPlayingSong(long songId) {
        return currentSong != null && currentSong.getId() == songId && exoPlayer.isPlaying();
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
     */
    public void pause() {
        exoPlayer.pause();
    }
    
    /**
     * Resume playback
     */
    public void play() {
        exoPlayer.play();
    }
    
    /**
     * Seek đến vị trí cụ thể
     */
    public void seekTo(long positionMs) {
        try {
            long duration = exoPlayer.getDuration();
            android.util.Log.d("MediaPlaybackService", "Seeking to: " + positionMs + "ms (Duration: " + duration + "ms)");

            // Đảm bảo position hợp lệ
            if (positionMs >= 0 && (duration <= 0 || positionMs <= duration)) {
                boolean wasPlaying = exoPlayer.isPlaying();
                exoPlayer.seekTo(positionMs);

                // Đảm bảo playback tiếp tục nếu đang phát
                if (wasPlaying) {
                    exoPlayer.setPlayWhenReady(true);
                }

                android.util.Log.d("MediaPlaybackService", "Seek completed, playing: " + exoPlayer.isPlaying());
            } else {
                android.util.Log.w("MediaPlaybackService", "Invalid seek position: " + positionMs);
            }
        } catch (Exception e) {
            android.util.Log.e("MediaPlaybackService", "Error during seek", e);
        }
    }
    
    /**
     * Lấy vị trí hiện tại
     */
    public long getCurrentPosition() {
        return exoPlayer.getCurrentPosition();
    }
    
    /**
     * Lấy tổng thời lượng
     */
    public long getDuration() {
        return exoPlayer.getDuration();
    }
    
    /**
     * Kiểm tra có đang phát không
     */
    public boolean isPlaying() {
        return exoPlayer.isPlaying();
    }
    
    // XÓA duplicate methods - đã có ở lines 170-179
    
    /**
     * Set listener cho UI updates
     */
    public void setPlaybackStateListener(PlaybackStateListener listener) {
        this.playbackStateListener = listener;
    }
    
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
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
        }
        isServiceStarted = false;
    }
}
