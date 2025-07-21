package com.g3.soundify_musicplayer.ui.player;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.model.MediaPlayerState;
import com.g3.soundify_musicplayer.data.model.NavigationContext;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel cho Full Player Screen với context-aware navigation
 * Extends MiniPlayerViewModel để có tất cả functionality cơ bản
 * Thêm các features đặc biệt cho full screen experience
 */
public class FullPlayerViewModel extends MiniPlayerViewModel {

    // Additional UI state for full player
    private MutableLiveData<Boolean> isShuffleEnabled = new MutableLiveData<>();
    private MutableLiveData<MediaPlayerState.RepeatMode> repeatMode = new MutableLiveData<>();
    private MutableLiveData<String> positionText = new MutableLiveData<>();
    private MutableLiveData<String> contextActionText = new MutableLiveData<>();
    private MutableLiveData<Boolean> showLyrics = new MutableLiveData<>();
    private MutableLiveData<Boolean> showQueue = new MutableLiveData<>();

    // Navigation context
    private MutableLiveData<NavigationContext> currentContext = new MutableLiveData<>();

    // Backward compatibility LiveData
    private MutableLiveData<Boolean> isLiked = new MutableLiveData<>();
    private MutableLiveData<Boolean> isFollowing = new MutableLiveData<>();
    private MutableLiveData<Integer> currentTime = new MutableLiveData<>();

    // Additional executor for full player operations
    private ExecutorService fullPlayerExecutor;

    public FullPlayerViewModel(@NonNull Application application) {
        super(application);
        fullPlayerExecutor = Executors.newFixedThreadPool(2);

        // Initialize full player state
        isShuffleEnabled.setValue(false);
        repeatMode.setValue(MediaPlayerState.RepeatMode.OFF);
        positionText.setValue("");
        contextActionText.setValue("");
        showLyrics.setValue(false);
        showQueue.setValue(false);

        // Initialize backward compatibility state
        isLiked.setValue(false);
        isFollowing.setValue(false);
        currentTime.setValue(0);

        // Setup additional derived LiveData
        setupFullPlayerLiveData();
    }

    /**
     * Setup additional LiveData for full player
     */
    private void setupFullPlayerLiveData() {
        // Shuffle state from playback state
        LiveData<MediaPlayerState.CurrentPlaybackState> playbackState = mediaRepository.getCurrentPlaybackState();
        LiveData<MediaPlayerState.QueueInfo> queueInfo = mediaRepository.getQueueInfo();

        // Update shuffle and repeat from playback state
        Transformations.map(playbackState, state -> {
            if (state != null) {
                isShuffleEnabled.setValue(state.isShuffleEnabled());
                repeatMode.setValue(state.getRepeatMode());
            }
            return null;
        });

        // Update position text from queue info
        Transformations.map(queueInfo, info -> {
            if (info != null) {
                positionText.setValue(info.getPositionText());
                contextActionText.setValue(info.getContextActionText());
            }
            return null;
        });
    }

    // ========== CONTEXT-AWARE NAVIGATION METHODS ==========

    /**
     * Load song with navigation context
     */
    public void loadSongWithContext(long songId, NavigationContext context) {
        executor.execute(() -> {
            try {
                // Get song from repository
                Song song = mediaRepository.getSongByIdSync(songId).get();
                if (song != null) {
                    // Play song with context
                    mediaRepository.playSongWithContext(song, context).get();

                    // Update current context
                    currentContext.postValue(context);

                    // Update UI state
                    updateContextInfo(context);
                } else {
                    errorMessage.postValue("Không tìm thấy bài hát");
                }
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi load bài hát: " + e.getMessage());
            }
        });
    }

    /**
     * Update context info based on navigation context
     */
    private void updateContextInfo(NavigationContext context) {
        if (context == null) return;

        positionText.postValue(context.getPositionText());

        switch (context.getType()) {
            case FROM_PLAYLIST:
                contextActionText.postValue("View Playlist");
                break;
            case FROM_ARTIST:
                contextActionText.postValue("View Artist Profile");
                break;
            case FROM_SEARCH:
                contextActionText.postValue("Back to Search Results");
                break;
            case FROM_GENERAL:
                contextActionText.postValue("Back to Browse");
                break;
        }
    }

    /**
     * Handle context action button click
     */
    public void onContextActionClick() {
        NavigationContext context = currentContext.getValue();
        if (context == null) return;

        // This would trigger navigation back to the source
        // Implementation depends on your navigation architecture
        switch (context.getType()) {
            case FROM_PLAYLIST:
                // Navigate to playlist detail with context.getContextId()
                break;
            case FROM_ARTIST:
                // Navigate to artist profile with context.getContextId()
                break;
            case FROM_SEARCH:
                // Navigate back to search with context.getSearchQuery()
                break;
            case FROM_GENERAL:
                // Navigate back to home/browse
                break;
        }
    }

    // ========== ENHANCED PLAYBACK CONTROL METHODS ==========

    /**
     * Toggle shuffle mode
     */
    public void toggleShuffle() {
        fullPlayerExecutor.execute(() -> {
            try {
                Boolean result = mediaRepository.toggleShuffle().get();
                if (result) {
                    MediaPlayerState.CurrentPlaybackState state = mediaRepository.getCurrentPlaybackState().getValue();
                    if (state != null) {
                        isShuffleEnabled.postValue(state.isShuffleEnabled());
                    }
                }
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi toggle shuffle: " + e.getMessage());
            }
        });
    }

    /**
     * Cycle repeat mode
     */
    public void cycleRepeatMode() {
        fullPlayerExecutor.execute(() -> {
            try {
                MediaPlayerState.RepeatMode newMode = mediaRepository.cycleRepeatMode().get();
                repeatMode.postValue(newMode);
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi cycle repeat mode: " + e.getMessage());
            }
        });
    }

    // ========== UI STATE MANAGEMENT ==========

    /**
     * Toggle lyrics display
     */
    public void toggleLyrics() {
        Boolean current = showLyrics.getValue();
        showLyrics.setValue(current == null ? true : !current);
    }

    /**
     * Toggle queue display
     */
    public void toggleQueue() {
        Boolean current = showQueue.getValue();
        showQueue.setValue(current == null ? true : !current);
    }

    /**
     * Seek to specific position in milliseconds
     */
    public void seekTo(long positionMs) {
        fullPlayerExecutor.execute(() -> {
            try {
                Boolean result = mediaRepository.seekTo(positionMs).get();
                if (!result) {
                    errorMessage.postValue("Không thể seek đến vị trí này");
                }
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi seek: " + e.getMessage());
            }
        });
    }

    // ========== GETTERS FOR LIVEDATA ==========

    // Full player specific LiveData
    public LiveData<Boolean> getIsShuffleEnabled() { return isShuffleEnabled; }
    public LiveData<MediaPlayerState.RepeatMode> getRepeatMode() { return repeatMode; }
    public LiveData<String> getPositionText() { return positionText; }
    public LiveData<String> getContextActionText() { return contextActionText; }
    public LiveData<Boolean> getShowLyrics() { return showLyrics; }
    public LiveData<Boolean> getShowQueue() { return showQueue; }
    public LiveData<NavigationContext> getCurrentContext() { return currentContext; }

    // Inherited from MiniPlayerViewModel:
    // getCurrentSong(), getIsPlaying(), getIsPaused(), getIsLoading()
    // getFormattedCurrentPosition(), getFormattedDuration(), getProgressPercentage()
    // getHasPrevious(), getHasNext(), getContextTitle()
    // getIsExpanded(), getErrorMessage(), getIsPlayerVisible()

    // ========== UTILITY METHODS ==========

    /**
     * Get repeat mode icon resource name
     */
    public String getRepeatModeIcon() {
        MediaPlayerState.RepeatMode mode = repeatMode.getValue();
        if (mode == null) return "ic_repeat_off";

        switch (mode) {
            case OFF:
                return "ic_repeat_off";
            case ALL:
                return "ic_repeat";
            case ONE:
                return "ic_repeat_one";
            default:
                return "ic_repeat_off";
        }
    }

    /**
     * Get repeat mode description
     */
    public String getRepeatModeDescription() {
        MediaPlayerState.RepeatMode mode = repeatMode.getValue();
        if (mode == null) return "Repeat Off";

        switch (mode) {
            case OFF:
                return "Repeat Off";
            case ALL:
                return "Repeat All";
            case ONE:
                return "Repeat One";
            default:
                return "Repeat Off";
        }
    }

    // ========== BACKWARD COMPATIBILITY METHODS ==========

    /**
     * Load song (backward compatibility)
     */
    public void loadSong(long songId) {
        fullPlayerExecutor.execute(() -> {
            try {
                Song song = mediaRepository.getSongByIdSync(songId).get();
                if (song != null) {
                    // Create a simple context for general browsing
                    List<Long> songIds = new java.util.ArrayList<>();
                    songIds.add(songId);
                    NavigationContext context = NavigationContext.fromGeneral("Current Song", songIds, 0);

                    // Play song with context
                    mediaRepository.playSongWithContext(song, context).get();
                    currentContext.postValue(context);
                } else {
                    errorMessage.postValue("Không tìm thấy bài hát");
                }
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi load bài hát: " + e.getMessage());
            }
        });
    }

    /**
     * Toggle like (backward compatibility)
     */
    public void toggleLike(long songId) {
        fullPlayerExecutor.execute(() -> {
            try {
                // Use the media repository directly
                Song currentSong = getCurrentSong().getValue();
                if (currentSong != null) {
                    Boolean result = mediaRepository.toggleSongLike(currentSong.getId(), getCurrentUserId()).get();
                    isLiked.postValue(result);
                }
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi toggle like: " + e.getMessage());
            }
        });
    }

    /**
     * Toggle follow (backward compatibility)
     */
    public void toggleFollow(long artistId) {
        fullPlayerExecutor.execute(() -> {
            try {
                // Mock implementation for demo
                Boolean currentState = isFollowing.getValue();
                boolean newState = currentState == null ? true : !currentState;
                isFollowing.postValue(newState);
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi toggle follow: " + e.getMessage());
            }
        });
    }

    /**
     * Add comment (backward compatibility)
     */
    public void addComment(long songId, String content) {
        fullPlayerExecutor.execute(() -> {
            try {
                // Use the media repository directly
                Long commentId = mediaRepository.addComment(songId, getCurrentUserId(), content).get();
                if (commentId == null || commentId <= 0) {
                    errorMessage.postValue("Không thể thêm comment");
                }
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi thêm comment: " + e.getMessage());
            }
        });
    }

    /**
     * Get current user ID (mock implementation)
     */
    private long getCurrentUserId() {
        return 1L; // Mock user ID
    }

    // ========== BACKWARD COMPATIBILITY GETTERS ==========

    public LiveData<com.g3.soundify_musicplayer.data.entity.User> getCurrentArtist() {
        // Return a derived LiveData that extracts artist info from current song
        return Transformations.map(getCurrentSong(), song -> {
            if (song != null) {
                // Create a mock User object from song's uploader info
                com.g3.soundify_musicplayer.data.entity.User user = new com.g3.soundify_musicplayer.data.entity.User();
                user.setId(song.getUploaderId());
                user.setDisplayName("Artist"); // Mock name
                return user;
            }
            return null;
        });
    }

    public LiveData<Boolean> getIsLiked() { return isLiked; }
    public LiveData<Boolean> getIsFollowing() { return isFollowing; }
    public LiveData<Integer> getProgress() { return getProgressPercentage(); }
    public LiveData<Integer> getCurrentTime() { return currentTime; }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (fullPlayerExecutor != null) {
            fullPlayerExecutor.shutdown();
        }
    }
}
