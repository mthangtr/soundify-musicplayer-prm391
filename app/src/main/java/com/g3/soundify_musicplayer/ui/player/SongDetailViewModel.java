package com.g3.soundify_musicplayer.ui.player;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.model.MediaPlayerState;

import com.g3.soundify_musicplayer.data.repository.MediaPlayerRepository;
import com.g3.soundify_musicplayer.data.repository.MusicPlayerRepository;
import com.g3.soundify_musicplayer.data.repository.SongDetailRepository;
import com.g3.soundify_musicplayer.data.repository.SongRepository;

import com.g3.soundify_musicplayer.utils.AuthManager;
import com.g3.soundify_musicplayer.utils.RepositoryManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel THỐNG NHẤT cho cả MiniPlayer và FullPlayer
 * Kết hợp SongDetailRepository (song data) + MediaPlaybackService (playback)
 */
public class SongDetailViewModel extends AndroidViewModel {

    private final SongDetailRepository repository;
    private final MediaPlayerRepository mediaPlayerRepository;
    private final AuthManager authManager;
    private final ExecutorService executor;
    private final MutableLiveData<Song> currentSong = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLiked = new MutableLiveData<>();
    private final MutableLiveData<Integer> likeCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> commentCount = new MutableLiveData<>();
    private final MutableLiveData<List<Playlist>> userPlaylists = new MutableLiveData<>();
    private final MutableLiveData<List<Long>> playlistsContainingSong = new MutableLiveData<>();
    private final MutableLiveData<List<Song>> relatedSongs = new MutableLiveData<>();
    private final MutableLiveData<List<Song>> moreSongsByArtist = new MutableLiveData<>();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<User> currentArtist = new MutableLiveData<>();

    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private final MutableLiveData<Long> currentPosition = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> duration = new MutableLiveData<>(0L);
    private final MutableLiveData<Boolean> isFollowing = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSongOwner = new MutableLiveData<>(false);
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    public SongDetailViewModel(@NonNull Application application,
                               SongDetailRepository repository,
                               MediaPlayerRepository mediaPlayerRepository) {
        super(application);
        this.repository = repository;
        this.mediaPlayerRepository = mediaPlayerRepository;
        this.authManager = new AuthManager(application);
        this.executor = Executors.newFixedThreadPool(2);

        isLoading.setValue(false);
        isLiked.setValue(false);
        likeCount.setValue(0);
        commentCount.setValue(0);

        setupMediaPlayerObservers();
    }

    @Deprecated
    public SongDetailViewModel(@NonNull Application application) {
        this(application,
                RepositoryManager.getInstance(application).getSongDetailRepository(),
                RepositoryManager.getInstance(application).getMediaPlayerRepository());
    }

    /**
     * Load song detail data
     */
    public void loadSongDetail(long songId, long userId) {
        isLoading.postValue(true); // FIXED: Use postValue() to avoid IllegalStateException

        executor.execute(() -> {
            try {
                SongDetailRepository.SongDetailData data = repository.getSongDetailData(songId, userId).get();
                if (data != null) {
                    // Update UI data
                    currentSong.postValue(data.song);
                    isLiked.postValue(data.isLiked);
                    likeCount.postValue(data.likeCount);
                    commentCount.postValue(data.commentCount);
                    playlistsContainingSong.postValue(data.playlistIds);

                    loadArtistInfo(data.song.getUploaderId());

                    // Load related content
                    loadRelatedSongs(data.song.getGenre(), songId);
                    loadMoreSongsByArtist(data.song.getUploaderId(), songId);
                    loadUserPlaylists(userId);

                    // Check song ownership
                    checkSongOwnership(songId, userId);
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
                android.util.Log.e("SongDetailViewModel", "❌ Error in toggleLike", e);
                errorMessage.postValue("Lỗi khi thực hiện like: " + e.getMessage());
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
            errorMessage.postValue("Vui lòng chọn ít nhất một playlist"); // FIXED: Use postValue()
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

    private void loadArtistInfo(long uploaderId) {
        if (executor == null || executor.isShutdown()) {
            android.util.Log.w("SongDetailViewModel", "Executor shutdown, skipping loadArtistInfo");
            return;
        }

        executor.execute(() -> {
            try {
                User artist = repository.getUserById(uploaderId).get();
                if (artist != null) {
                    setCurrentArtist(artist);
                } else {
                }
            } catch (Exception e) {
                android.util.Log.e("SongDetailViewModel", "🎤 Error loading artist info for uploaderId: " + uploaderId, e);
            }
        });
    }

    private void loadUserPlaylists(long userId) {
        executor.execute(() -> {
            try {
                List<Playlist> playlists = repository.getUserPlaylistsForAddSong(userId).get();
                userPlaylists.postValue(playlists);
            } catch (Exception e) {
            }
        });
    }

    private long getCurrentUserId() {
        if (authManager != null) {
            return authManager.getCurrentUserId();
        }
        android.util.Log.w("SongDetailViewModel", "AuthManager is null, using fallback userId: 1");
        return 1L; // Fallback user ID
    }

    // ========== GETTERS FOR LIVEDATA ==========

    public LiveData<Song> getCurrentSong() {
        return currentSong;
    }

    public LiveData<Boolean> getIsLiked() {
        return isLiked;
    }

    @SuppressWarnings("unused")
    public LiveData<Integer> getLikeCount() {
        return likeCount;
    }

    @SuppressWarnings("unused")
    public LiveData<Integer> getCommentCount() {
        return commentCount;
    }

    @SuppressWarnings("unused")
    public LiveData<List<Playlist>> getUserPlaylists() {
        return userPlaylists;
    }

    @SuppressWarnings("unused")
    public LiveData<List<Long>> getPlaylistsContainingSong() {
        return playlistsContainingSong;
    }

    @SuppressWarnings("unused")
    public LiveData<List<Song>> getRelatedSongs() {
        return relatedSongs;
    }

    @SuppressWarnings("unused")
    public LiveData<List<Song>> getMoreSongsByArtist() {
        return moreSongsByArtist;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    @SuppressWarnings("unused") // Method có thể được sử dụng trong tương lai
    public LiveData<List<User>> getUsersWhoLikedSong(long songId) {
        return repository.getUsersWhoLikedSong(songId);
    }

    public LiveData<MediaPlayerState.QueueInfo> getQueueInfo() {
        return mediaPlayerRepository.getQueueInfo();
    }

    public void playFromView(List<Song> songs, String viewTitle, int startIndex) {
        // gọi replaceListAndPlay() của MediaPlayerRepository để thay thế queue và phát nhạc
        mediaPlayerRepository.replaceListAndPlay(songs, viewTitle, startIndex);

        // cập nhật UI để cho biết bài đang chọn chính là bài bắt đầu
        Song selectedSong = songs.get(startIndex);

        // load thông tin bài hát hiện tại
        loadSongDetail(selectedSong.getId(), 1L);
    }

    /**
     * Toggle play/pause
     */
    public void togglePlayPause() {
        executor.execute(() -> {
            try {
                mediaPlayerRepository.togglePlayPause().get();
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi toggle play/pause: " + e.getMessage());
            }
        });
    }

    /**
     * Seek đến vị trí cụ thể (percentage 0-100)
     */
    public void seekToPercentage(int percentage) {
        executor.execute(() -> {
            try {
                // Lấy duration từ MediaPlayerRepository state
                MediaPlayerState.CurrentPlaybackState state = mediaPlayerRepository.getCurrentPlaybackState().getValue();
                if (state != null && state.getDuration() > 0 && percentage >= 0 && percentage <= 100) {
                    long targetPosition = (state.getDuration() * percentage) / 100;
                    mediaPlayerRepository.seekTo(targetPosition).get();
                }
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi seek: " + e.getMessage());
            }
        });
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
     * Check if current user is the owner of the song
     */
    public void checkSongOwnership(long songId, long userId) {
        executor.execute(() -> {
            try {
                Boolean isOwner = repository.isSongOwner(songId, userId).get();
                isSongOwner.postValue(isOwner);
                android.util.Log.d("SongDetailViewModel", "Song ownership check - SongId: " + songId + ", UserId: " + userId + ", IsOwner: " + isOwner);
            } catch (Exception e) {
                android.util.Log.e("SongDetailViewModel", "Error checking song ownership", e);
                isSongOwner.postValue(false);
            }
        });
    }

    /**
     * Update song information (only for song owner)
     */
    public void updateSongInfo(long songId, long userId, String title, String description, String genre, boolean isPublic, String coverArtUrl) {
        if (title == null || title.trim().isEmpty()) {
            errorMessage.postValue("Song title cannot be empty");
            return;
        }

        if (title.trim().length() > 100) {
            errorMessage.postValue("Song title is too long");
            return;
        }

        if (description != null && description.length() > 500) {
            errorMessage.postValue("Song description is too long");
            return;
        }

        android.util.Log.d("SongDetailViewModel", "Updating song info - SongId: " + songId + ", Title: " + title);

        executor.execute(() -> {
            try {
                Boolean success = repository.updateSongInfo(songId, userId, title.trim(), description, genre, isPublic, coverArtUrl).get();

                if (success) {
                    android.util.Log.d("SongDetailViewModel", "Successfully updated song info");
                    // Reload song data to refresh UI
                    loadSongDetail(songId, userId);
                    successMessage.postValue("Song updated successfully");
                } else {
                    android.util.Log.e("SongDetailViewModel", "Failed to update song info");
                    errorMessage.postValue("Error updating song");
                }

            } catch (Exception e) {
                android.util.Log.e("SongDetailViewModel", "Error updating song info", e);
                errorMessage.postValue("Error updating song: " + e.getMessage());
            }
        });
    }

    /**
     * Delete song (only for song owner)
     */
    public void deleteSong(long songId, long userId) {
        android.util.Log.d("SongDetailViewModel", "Deleting song - SongId: " + songId + ", UserId: " + userId);

        executor.execute(() -> {
            try {
                Boolean success = repository.deleteSongByOwner(songId, userId).get();

                if (success) {
                    android.util.Log.d("SongDetailViewModel", "Successfully deleted song");
                    successMessage.postValue("Song deleted successfully");
                    // Note: Fragment should handle navigation away from deleted song
                } else {
                    android.util.Log.e("SongDetailViewModel", "Failed to delete song");
                    errorMessage.postValue("Error deleting song");
                }

            } catch (Exception e) {
                android.util.Log.e("SongDetailViewModel", "Error deleting song", e);
                errorMessage.postValue("Error deleting song: " + e.getMessage());
            }
        });
    }

    /**
     * Clear success message
     */
    public void clearSuccessMessage() {
        successMessage.setValue(null);
    }

    /**
     * Setup observers để sync state từ MediaPlayerRepository
     */
    private void setupMediaPlayerObservers() {
        // Observer cho playback state changes
        mediaPlayerRepository.getCurrentPlaybackState().observeForever(state -> {
            if (state != null) {
                // Update song info
                if (state.getCurrentSong() != null) {
                    Song newSong = state.getCurrentSong();
                    Song currentSongValue = currentSong.getValue();
                    if (currentSongValue == null || (newSong.getId() != currentSongValue.getId())) {
                        currentSong.postValue(newSong);
                        loadArtistInfo(newSong.getUploaderId());

                        // ✅ TRACK RECENTLY PLAYED: Track when song changes (including next/previous)
                        trackRecentlyPlayedForCurrentSong(newSong.getId());
                    }
                }

                if (state.getCurrentArtist() != null) {
                    setCurrentArtist(state.getCurrentArtist());
                }

                // Update playback state
                isPlaying.postValue(state.isPlaying());
                currentPosition.postValue(state.getCurrentPosition());
                duration.postValue(state.getDuration());

                // Update progress
                if (state.getDuration() > 0) {
                    int progressPercent = (int) ((state.getCurrentPosition() * 100) / state.getDuration());
                    progress.postValue(progressPercent);
                }

                // Start/stop progress updates based on playing state
                if (state.isPlaying()) {
                    startProgressUpdates();
                } else {
                    stopProgressUpdates();
                }
            }
        });
    }

    /**
     * ✅ FIXED: Thread-safe progress updates with proper lifecycle management
     */
    private void startProgressUpdates() {
        stopProgressUpdates(); // Ensure cleanup before creating new one

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    // ✅ CRITICAL: Check if ViewModel is still active
                    if (progressRunnable == null) {
                        // ViewModel was cleared, stop updates
                        return;
                    }

                    MediaPlayerState.CurrentPlaybackState state = mediaPlayerRepository.getCurrentPlaybackState().getValue();
                    if (state != null && state.isPlaying()) {
                        long currentPos = state.getCurrentPosition();
                        long dur = state.getDuration();

                        // ✅ SAFE: Only update if values are valid
                        if (currentPos >= 0 && dur > 0) {
                            currentPosition.postValue(currentPos);
                            duration.postValue(dur);
                            int progressPercent = (int) ((currentPos * 100) / dur);
                            progress.postValue(progressPercent);
                        }

                        // ✅ CRITICAL: Only schedule next update if still playing AND runnable exists
                        if (state.isPlaying() && progressRunnable != null) {
                            progressHandler.postDelayed(this, 500);
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.w("SongDetailViewModel", "Error updating progress: " + e.getMessage());
                    stopProgressUpdates(); // Stop on any error to prevent crash
                }
            }
        };

        // ✅ SAFE: Only start if handler is valid
        if (progressHandler != null) {
            progressHandler.post(progressRunnable);
        }
    }

    /**
     * ✅ FIXED: Robust cleanup to prevent thread leaks
     */
    private void stopProgressUpdates() {
        if (progressRunnable != null && progressHandler != null) {
            progressHandler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }

    /**
     * Play next song in queue
     */
    public void playNext() {
        executor.execute(() -> {
            try {
                mediaPlayerRepository.playNext().get();
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi chuyển bài tiếp theo: " + e.getMessage());
            }
        });
    }

    /**
     * Play previous song in queue
     */
    public void playPrevious() {
        executor.execute(() -> {
            try {
                android.util.Log.d("SongDetailViewModel", "Playing previous song=====================");
                mediaPlayerRepository.playPrevious().get();
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi chuyển bài trước đó: " + e.getMessage());
            }
        });
    }


    /**
     * Hide mini player
     */
    public void hideMiniPlayer() {
        mediaPlayerRepository.hidePlayer();
        executor.execute(() -> {
            try {
                mediaPlayerRepository.pause().get();
            } catch (Exception e) {
                android.util.Log.w("SongDetailViewModel", "Error pausing: " + e.getMessage());
            }
        });
    }


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
        long currentUserId = getCurrentUserId();
        assert song != null;
        toggleLike(song.getId(), currentUserId);
    }

    /**
     * Track recently played for current song (called when song changes via next/previous)
     */
    private void trackRecentlyPlayedForCurrentSong(long songId) {
        long currentUserId = getCurrentUserId();
        if (currentUserId != -1) {
            // Track in background thread
            executor.execute(() -> {
                try {
                    // Use SongRepository to track recently played
                    SongRepository songRepository = new SongRepository(getApplication());
                    songRepository.trackRecentlyPlayed(currentUserId, songId);
                    android.util.Log.d("SongDetailViewModel", "✅ Tracked recently played for song: " + songId + " (via next/previous)");
                } catch (Exception e) {
                    android.util.Log.e("SongDetailViewModel", "❌ Error tracking recently played", e);
                }
            });
        } else {
            android.util.Log.w("SongDetailViewModel", "Cannot track recently played - user not logged in");
        }
    }

    // Getters cho playback LiveData
    public LiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }

    public LiveData<Boolean> getIsSongOwner() {
        return isSongOwner;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public LiveData<Boolean> getIsVisible() {
        return mediaPlayerRepository.getIsPlayerVisible();
    }

    public LiveData<Integer> getProgress() {
        return progress;
    }

    public LiveData<Long> getCurrentPosition() {
        return currentPosition;
    }

    public LiveData<Long> getDuration() {
        return duration;
    }

    public LiveData<Boolean> getIsFollowing() {
        return isFollowing;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        try {
            stopProgressUpdates();
            if (repository != null) {
                repository.shutdown();
            }
            if (executor != null) {
                executor.shutdown();
            }
        } catch (Exception e) {
            android.util.Log.e("SongDetailViewModel", "Error during ViewModel cleanup", e);
        }
    }

    public void pauseUpdates() {
        stopProgressUpdates();
    }

    public void resumeUpdates() {
        MediaPlayerState.CurrentPlaybackState state = mediaPlayerRepository.getCurrentPlaybackState().getValue();
        if (state != null && state.isPlaying()) {
            startProgressUpdates();
        }
    }
}
