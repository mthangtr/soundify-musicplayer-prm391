package com.g3.soundify_musicplayer.ui.player;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Comment;
import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.model.NavigationContext;
import com.g3.soundify_musicplayer.data.repository.MusicPlayerRepository;
import com.g3.soundify_musicplayer.data.repository.SongDetailRepository;
import com.g3.soundify_musicplayer.service.MediaPlaybackService;
import com.g3.soundify_musicplayer.utils.AuthManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel THỐNG NHẤT cho cả MiniPlayer và FullPlayer
 * Kết hợp SongDetailRepository (song data) + MediaPlaybackService (playback)
 */
public class SongDetailViewModel extends AndroidViewModel {

    private final SongDetailRepository repository;
    private final AuthManager authManager;
    private final ExecutorService executor;

    // MediaPlaybackService integration
    private  MediaPlaybackService mediaService;
    private boolean isServiceBound = false;
    
    // LiveData cho UI
    private final MutableLiveData<Song> currentSong = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLiked = new MutableLiveData<>();
    private final MutableLiveData<Integer> likeCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> commentCount = new MutableLiveData<>();
    private final MutableLiveData<List<Playlist>> userPlaylists = new MutableLiveData<>();
    private final MutableLiveData<List<Long>> playlistsContainingSong = new MutableLiveData<>();
    private final MutableLiveData<List<Song>> relatedSongs = new MutableLiveData<>();
    private final MutableLiveData<List<Song>> moreSongsByArtist = new MutableLiveData<>();
    
    // Status LiveData
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Playback LiveData
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isVisible = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private final MutableLiveData<Long> currentPosition = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> duration = new MutableLiveData<>(0L);
    private final MutableLiveData<Boolean> isFollowing = new MutableLiveData<>(false);

    // Navigation context
    private NavigationContext currentNavigationContext;

    // Handler để update progress
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    
    public SongDetailViewModel(@NonNull Application application) {
        super(application);
        repository = new SongDetailRepository(application);
        authManager = new AuthManager(application);
        executor = Executors.newFixedThreadPool(2);

        // Initialize values
        isLoading.setValue(false);
        isLiked.setValue(false);
        likeCount.setValue(0);
        commentCount.setValue(0);

        // Bind đến MediaPlaybackService
        bindToService(application);
    }
    
    // ========== PUBLIC METHODS ==========
    
    /**
     * Load song detail data
     */
    public void loadSongDetail(long songId, long userId) {
        isLoading.setValue(true);
        
        executor.execute(() -> {
            try {
                // Get comprehensive song detail data
                SongDetailRepository.SongDetailData data = repository.getSongDetailData(songId, userId).get();
                
                if (data != null) {
                    // Update UI data
                    currentSong.postValue(data.song);
                    isLiked.postValue(data.isLiked);
                    likeCount.postValue(data.likeCount);
                    commentCount.postValue(data.commentCount);
                    playlistsContainingSong.postValue(data.playlistIds);
                    
                    // Load related content
                    loadRelatedSongs(data.song.getGenre(), songId);
                    loadMoreSongsByArtist(data.song.getUploaderId(), songId);
                    loadUserPlaylists(userId);
                    
                } else {
                    errorMessage.postValue("Không thể tải thông tin bài hát");
                }
                
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi tải dữ liệu: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }
    
    /**
     * Toggle song like status
     */
    public void toggleLike(long songId, long userId) {
        executor.execute(() -> {
            try {
                Boolean newLikeStatus = repository.toggleSongLike(songId, userId).get();
                isLiked.postValue(newLikeStatus);
                
                // Update like count
                MusicPlayerRepository.SongLikeInfo likeInfo = repository.getSongLikeInfo(songId, userId).get();
                likeCount.postValue(likeInfo.likeCount);
                
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi thực hiện like: " + e.getMessage());
            }
        });
    }
    
    /**
     * Add comment to song
     */
    @SuppressWarnings("unused") // Method có thể được sử dụng trong tương lai
    public void addComment(long songId, long userId, String content) {
        if (content == null || content.trim().isEmpty()) {
            errorMessage.setValue("Nội dung comment không được để trống");
            return;
        }

        executor.execute(() -> {
            try {
                Long commentId = repository.addComment(songId, userId, content.trim()).get();
                if (commentId != null && commentId > 0) {
                    // Update comment count
                    refreshCommentCount(songId);
                } else {
                    errorMessage.postValue("Không thể thêm comment");
                }

            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi thêm comment: " + e.getMessage());
            }
        });
    }

    /**
     * Refresh comment count for a song (useful when comments are added/deleted from other screens)
     */
    public void refreshCommentCount(long songId) {
        executor.execute(() -> {
            try {
                Integer newCount = repository.getCommentCountBySong(songId).get();
                commentCount.postValue(newCount);
            } catch (Exception e) {
                android.util.Log.e("SongDetailViewModel", "Error refreshing comment count", e);
            }
        });
    }
    
    /**
     * Add song to selected playlists
     */
    public void addSongToPlaylists(long songId, List<Long> selectedPlaylistIds) {
        if (selectedPlaylistIds == null || selectedPlaylistIds.isEmpty()) {
            errorMessage.setValue("Vui lòng chọn ít nhất một playlist");
            return;
        }
        
        executor.execute(() -> {
            try {
                repository.addSongToMultiplePlaylists(songId, selectedPlaylistIds).get();
                
                // Update playlists containing song
                List<Long> updatedPlaylistIds = repository.getPlaylistIdsContainingSong(songId, getCurrentUserId()).get();
                playlistsContainingSong.postValue(updatedPlaylistIds);
                
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi thêm vào playlist: " + e.getMessage());
            }
        });
    }
    
    /**
     * Create new playlist and add song to it
     */
    @SuppressWarnings("unused") // Method có thể được sử dụng trong tương lai
    public void createPlaylistWithSong(String playlistName, String description, boolean isPublic, long ownerId, long songId) {
        if (playlistName == null || playlistName.trim().isEmpty()) {
            errorMessage.setValue("Tên playlist không được để trống");
            return;
        }
        
        executor.execute(() -> {
            try {
                Long playlistId = repository.createPlaylistWithSong(
                    playlistName.trim(), 
                    description != null ? description.trim() : "", 
                    isPublic, 
                    ownerId, 
                    songId
                ).get();
                
                if (playlistId != null && playlistId > 0) {
                    // Reload user playlists and playlists containing song
                    loadUserPlaylists(ownerId);
                    List<Long> updatedPlaylistIds = repository.getPlaylistIdsContainingSong(songId, ownerId).get();
                    playlistsContainingSong.postValue(updatedPlaylistIds);
                } else {
                    errorMessage.postValue("Không thể tạo playlist");
                }
                
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi tạo playlist: " + e.getMessage());
            }
        });
    }
    
    // ========== PRIVATE HELPER METHODS ==========
    
    private void loadRelatedSongs(String genre, long excludeSongId) {
        executor.execute(() -> {
            try {
                List<Song> songs = repository.getRelatedSongsByGenre(genre, excludeSongId, 10).get();
                relatedSongs.postValue(songs);
            } catch (Exception e) {
                // Silent fail for related songs
            }
        });
    }
    
    private void loadMoreSongsByArtist(long uploaderId, long excludeSongId) {
        executor.execute(() -> {
            try {
                List<Song> songs = repository.getMoreSongsByUploader(uploaderId, excludeSongId, 10).get();
                moreSongsByArtist.postValue(songs);
            } catch (Exception e) {
                // Silent fail for more songs
            }
        });
    }
    
    private void loadUserPlaylists(long userId) {
        executor.execute(() -> {
            try {
                List<Playlist> playlists = repository.getUserPlaylistsForAddSong(userId).get();
                userPlaylists.postValue(playlists);
            } catch (Exception e) {
                // Silent fail for playlists
            }
        });
    }
    
    private long getCurrentUserId() {
        if (authManager != null) {
            return authManager.getCurrentUserId();
        }
        return 1L; // Fallback user ID
    }
    
    // ========== GETTERS FOR LIVEDATA ==========
    
    public LiveData<Song> getCurrentSong() { return currentSong; }
    public LiveData<Boolean> getIsLiked() { return isLiked; }
    @SuppressWarnings("unused") public LiveData<Integer> getLikeCount() { return likeCount; }
    @SuppressWarnings("unused") public LiveData<Integer> getCommentCount() { return commentCount; }
    @SuppressWarnings("unused") public LiveData<List<Playlist>> getUserPlaylists() { return userPlaylists; }
    @SuppressWarnings("unused") public LiveData<List<Long>> getPlaylistsContainingSong() { return playlistsContainingSong; }
    @SuppressWarnings("unused") public LiveData<List<Song>> getRelatedSongs() { return relatedSongs; }
    @SuppressWarnings("unused") public LiveData<List<Song>> getMoreSongsByArtist() { return moreSongsByArtist; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    
    // Get LiveData from repository
    public LiveData<List<Comment>> getCommentsBySong(long songId) {
        return repository.getCommentsBySong(songId);
    }
    
    @SuppressWarnings("unused") // Method có thể được sử dụng trong tương lai
    public LiveData<List<User>> getUsersWhoLikedSong(long songId) {
        return repository.getUsersWhoLikedSong(songId);
    }

    // ========== MEDIA PLAYBACK METHODS ==========

    /**
     * Bind đến MediaPlaybackService
     */
    private void bindToService(Application application) {
        Intent intent = new Intent(application, MediaPlaybackService.class);
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * ServiceConnection để kết nối với MediaPlaybackService
     */
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlaybackService.MediaPlaybackBinder binder =
                (MediaPlaybackService.MediaPlaybackBinder) service;
            mediaService = binder.getService();
            isServiceBound = true;

            // Set up listener cho service
            mediaService.setPlaybackStateListener(new MediaPlaybackService.PlaybackStateListener() {
                @Override
                public void onSongChanged(Song song, User artist) {
                    android.util.Log.d("SongDetailViewModel", "onSongChanged called - Song: " +
                        (song != null ? song.getTitle() : "NULL") + ", Artist: " +
                        (artist != null ? artist.getDisplayName() : "NULL"));

                    currentSong.postValue(song);
                    setCurrentArtist(artist); // Set artist data
                    isVisible.postValue(true);

                    android.util.Log.d("SongDetailViewModel", "MiniPlayer visibility set to TRUE");
                }

                @Override
                public void onPlaybackStateChanged(boolean playing) {
                    // XÓA LOG SPAM - chỉ log khi cần debug
                    isPlaying.postValue(playing);
                    if (playing) {
                        startProgressUpdates();
                    } else {
                        stopProgressUpdates();
                    }
                }

                @Override
                public void onProgressChanged(long currentPos, long dur) {
                    currentPosition.postValue(currentPos);
                    duration.postValue(dur);

                    // Tính progress percentage cho ProgressBar
                    if (dur > 0) {
                        int progressPercent = (int) ((currentPos * 100) / dur);
                        progress.postValue(progressPercent);
                        // XÓA LOG SPAM - chỉ log khi cần debug
                    }
                }
            });

            // Sync current state từ service
            syncStateFromService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mediaService = null;
            isServiceBound = false;
        }
    };
    
    /**
     * Phát bài hát mới
     */
    public void playSong(Song song, User artist) {
        playSong(song, artist, null);
    }

    /**
     * Phát bài hát mới với navigation context
     */
    public void playSong(Song song, User artist, NavigationContext context) {
        playSongForced(song, artist, context);
    }

    /**
     * Phát bài hát - LUÔN restart từ đầu (dù cùng bài hay khác bài)
     * Đây là method chính được gọi từ UI khi user click vào bài hát
     */
    public void playSongForced(Song song, User artist, NavigationContext context) {
        if (mediaService != null && song != null) {
            currentNavigationContext = context;

            // LUÔN gọi playSong để restart từ đầu
            mediaService.playSong(song, artist);
            currentSong.postValue(song);
            setCurrentArtist(artist);
            isVisible.postValue(true);

            android.util.Log.d("SongDetailViewModel", "playSongForced - Song: " + song.getTitle() +
                ", Artist: " + (artist != null ? artist.getDisplayName() : "NULL") +
                " - Will restart from beginning");
        } else {
            errorMessage.postValue("Service chưa sẵn sàng hoặc bài hát không hợp lệ");
        }
    }

    /**
     * Toggle play/pause
     */
    public void togglePlayPause() {
        if (mediaService != null) {
            mediaService.togglePlayPause();
        } else {
            errorMessage.postValue("Service chưa sẵn sàng");
        }
    }

    /**
     * Seek đến vị trí cụ thể (percentage 0-100)
     */
    public void seekToPercentage(int percentage) {
        if (mediaService != null) {
            long dur = mediaService.getDuration();
            if (dur > 0 && percentage >= 0 && percentage <= 100) {
                long targetPosition = (dur * percentage) / 100;
                mediaService.seekTo(targetPosition);
            }
        }
    }

    /**
     * Toggle follow status
     */
    public void toggleFollow() {
        Boolean currentFollowing = isFollowing.getValue();
        boolean newFollowing = currentFollowing == null || !currentFollowing;
        isFollowing.postValue(newFollowing);
    }

    /**
     * Sync state từ service
     */
    private void syncStateFromService() {
        if (mediaService != null) {
            Song song = mediaService.getCurrentSong();
            User artist = mediaService.getCurrentArtist();

            android.util.Log.d("SongDetailViewModel", "syncStateFromService - Song: " +
                (song != null ? song.getTitle() : "NULL") + ", Artist: " +
                (artist != null ? artist.getDisplayName() : "NULL"));

            if (song != null) {
                currentSong.postValue(song);
                setCurrentArtist(artist); // Sync artist data
                isVisible.postValue(true);
                isPlaying.postValue(mediaService.isPlaying());

                // SỬA LỖI: Đồng bộ progress và position hiện tại
                long currentPos = mediaService.getCurrentPosition();
                long dur = mediaService.getDuration();
                currentPosition.postValue(currentPos);
                duration.postValue(dur);

                if (dur > 0) {
                    int progressPercent = (int) ((currentPos * 100) / dur);
                    progress.postValue(progressPercent);
                    // XÓA LOG SPAM - chỉ log khi cần debug
                }

                // XÓA LOG SPAM - chỉ log khi cần debug

                if (mediaService.isPlaying()) {
                    startProgressUpdates();
                }
            }
        }
    }

    /**
     * Progress updates
     */
    private void startProgressUpdates() {
        stopProgressUpdates(); // Đảm bảo cleanup trước khi tạo mới

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                // Kiểm tra service và playing state
                if (mediaService != null && mediaService.isPlaying()) {
                    try {
                        long currentPos = mediaService.getCurrentPosition();
                        long dur = mediaService.getDuration();
                        currentPosition.postValue(currentPos);
                        duration.postValue(dur);
                        if (dur > 0) {
                            int progressPercent = (int) ((currentPos * 100) / dur);
                            progress.postValue(progressPercent);
                        }
                        // Chỉ schedule next update nếu vẫn đang phát
                        if (mediaService.isPlaying()) {
                            progressHandler.postDelayed(this, 500);
                        }
                    } catch (Exception e) {
                        android.util.Log.w("SongDetailViewModel", "Error updating progress: " + e.getMessage());
                        stopProgressUpdates(); // Stop nếu có lỗi để tránh leak
                    }
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdates() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }

    /**
     * Play next song (simple implementation)
     */
    public void playNext() {
        // TODO: Implement queue functionality later
        errorMessage.postValue("Chức năng next song sẽ được thêm sau");
    }

    /**
     * Hide mini player
     */
    public void hideMiniPlayer() {
        isVisible.postValue(false);
        if (mediaService != null) {
            mediaService.pause();
        }
    }

    // Current artist LiveData (thêm field riêng để tránh memory leak)
    private final MutableLiveData<User> currentArtist = new MutableLiveData<>();

    /**
     * Get current artist (compatibility method)
     */
    public LiveData<User> getCurrentArtist() {
        return currentArtist;
    }

    /**
     * Set current artist (internal method)
     */
    private void setCurrentArtist(User artist) {
        currentArtist.postValue(artist);
    }

    /**
     * Toggle like without parameters (compatibility method)
     */
    public void toggleLike() {
        Song song = currentSong.getValue();
        if (song != null) {
            // Call the existing toggleLike with parameters
            toggleLike(song.getId(), 1L); // Use default userId = 1
        } else {
            errorMessage.postValue("No song selected to like");
        }
    }

    // Getters cho playback LiveData
    public LiveData<Boolean> getIsPlaying() { return isPlaying; }
    public LiveData<Boolean> getIsVisible() { return isVisible; }
    public LiveData<Integer> getProgress() { return progress; }
    public LiveData<Long> getCurrentPosition() { return currentPosition; }
    public LiveData<Long> getDuration() { return duration; }
    public LiveData<Boolean> getIsFollowing() { return isFollowing; }
    public NavigationContext getCurrentNavigationContext() { return currentNavigationContext; }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopProgressUpdates();
        if (isServiceBound) {
            getApplication().unbindService(serviceConnection);
            isServiceBound = false;
        }
        if (repository != null) {
            repository.shutdown();
        }
        if (executor != null) {
            executor.shutdown();
        }
    }
}
