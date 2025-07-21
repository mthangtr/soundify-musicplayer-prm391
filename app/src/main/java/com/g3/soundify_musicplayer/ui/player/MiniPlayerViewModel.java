package com.g3.soundify_musicplayer.ui.player;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.model.MediaPlayerState;
import com.g3.soundify_musicplayer.data.model.NavigationContext;
import com.g3.soundify_musicplayer.data.repository.MediaPlayerRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel cho Mini Player component với backend integration
 * Quản lý UI state cho mini player và handle user interactions
 * Supports context-aware navigation functionality
 */
public class MiniPlayerViewModel extends AndroidViewModel {

    protected MediaPlayerRepository mediaRepository;
    protected ExecutorService executor;

    // UI State LiveData
    private MutableLiveData<Boolean> isExpanded = new MutableLiveData<>();
    protected MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Derived LiveData from MediaPlayerRepository
    private LiveData<Song> currentSong;
    private LiveData<Boolean> isPlaying;
    private LiveData<Boolean> isPaused;
    private LiveData<Boolean> isLoading;
    private LiveData<String> formattedCurrentPosition;
    private LiveData<String> formattedDuration;
    private LiveData<Integer> progressPercentage;
    private LiveData<Boolean> hasPrevious;
    private LiveData<Boolean> hasNext;
    private LiveData<String> contextTitle;

    public MiniPlayerViewModel(@NonNull Application application) {
        super(application);
        mediaRepository = new MediaPlayerRepository(application);
        executor = Executors.newFixedThreadPool(2);

        // Initialize UI state
        isExpanded.setValue(false);

        // Set up derived LiveData
        setupDerivedLiveData();
    }

    /**
     * Setup derived LiveData from MediaPlayerRepository
     */
    private void setupDerivedLiveData() {
        LiveData<MediaPlayerState.CurrentPlaybackState> playbackState = mediaRepository.getCurrentPlaybackState();
        LiveData<MediaPlayerState.QueueInfo> queueInfo = mediaRepository.getQueueInfo();


        // Current song
        currentSong = Transformations.map(playbackState, state ->
            state != null ? state.getCurrentSong() : null
        );

        // Playback state booleans
        isPlaying = Transformations.map(playbackState, state ->
            state != null && state.isPlaying()
        );

        isPaused = Transformations.map(playbackState, state ->
            state != null && state.isPaused()
        );

        isLoading = Transformations.map(playbackState, state ->
            state != null && state.isLoading()
        );

        // Formatted time strings
        formattedCurrentPosition = Transformations.map(playbackState, state ->
            state != null ? state.getFormattedCurrentPosition() : "0:00"
        );

        formattedDuration = Transformations.map(playbackState, state ->
            state != null ? state.getFormattedDuration() : "0:00"
        );

        // Progress percentage
        progressPercentage = Transformations.map(playbackState, state ->
            state != null ? state.getProgressPercentage() : 0
        );

        // Navigation state
        hasPrevious = Transformations.map(queueInfo, info ->
            info != null && info.isHasPrevious()
        );

        hasNext = Transformations.map(queueInfo, info ->
            info != null && info.isHasNext()
        );

        contextTitle = Transformations.map(queueInfo, info ->
            info != null ? info.getQueueTitle() : ""
        );
    }

    // ========== PLAYBACK CONTROL METHODS ==========

    /**
     * Toggle play/pause
     */
    public void togglePlayPause() {
        executor.execute(() -> {
            try {
                Boolean result = mediaRepository.togglePlayPause().get();
                if (!result) {
                    errorMessage.postValue("Không thể thực hiện play/pause");
                }
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi play/pause: " + e.getMessage());
            }
        });
    }

    /**
     * Play specific song with context
     */
    public void playSong(Song song, NavigationContext context) {
        executor.execute(() -> {
            try {
                Boolean result = mediaRepository.playSongWithContext(song, context).get();
                if (result) {
                    // Show mini player when song starts
                    mediaRepository.showPlayer();
                } else {
                    errorMessage.postValue("Không thể phát bài hát");
                }
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi phát bài hát: " + e.getMessage());
            }
        });
    }

    /**
     * Play next song
     */
    public void playNext() {
        executor.execute(() -> {
            try {
                Boolean result = mediaRepository.playNext().get();
                if (!result) {
                    errorMessage.postValue("Không có bài hát tiếp theo");
                }
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi chuyển bài tiếp theo: " + e.getMessage());
            }
        });
    }

    /**
     * Play previous song
     */
    public void playPrevious() {
        executor.execute(() -> {
            try {
                Boolean result = mediaRepository.playPrevious().get();
                if (!result) {
                    errorMessage.postValue("Không có bài hát trước đó");
                }
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi chuyển bài trước đó: " + e.getMessage());
            }
        });
    }

    /**
     * Seek to position (percentage 0-100)
     */
    public void seekToPercentage(int percentage) {
        executor.execute(() -> {
            try {
                MediaPlayerState.CurrentPlaybackState state = mediaRepository.getCurrentPlaybackState().getValue();
                if (state != null && state.getDuration() > 0) {
                    long targetPosition = (state.getDuration() * percentage) / 100;
                    Boolean result = mediaRepository.seekTo(targetPosition).get();
                    if (!result) {
                        errorMessage.postValue("Không thể seek đến vị trí này");
                    }
                }
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi seek: " + e.getMessage());
            }
        });
    }

    // ========== UI STATE MANAGEMENT ==========

    /**
     * Expand mini player to full player
     */
    public void expandPlayer() {
        isExpanded.setValue(true);
    }

    /**
     * Collapse full player to mini player
     */
    public void collapsePlayer() {
        isExpanded.setValue(false);
    }

    /**
     * Hide mini player completely
     */
    public void hidePlayer() {
        mediaRepository.hidePlayer();
    }

    /**
     * Hide mini player (alias for backward compatibility)
     */
    public void hideMiniPlayer() {
        hidePlayer();
    }

    /**
     * Show mini player
     */
    public void showPlayer() {
        mediaRepository.showPlayer();
    }

    // ========== GETTERS FOR LIVEDATA ==========

    public LiveData<Song> getCurrentSong() { return currentSong; }
    public LiveData<Boolean> getIsPlaying() { return isPlaying; }
    public LiveData<Boolean> getIsPaused() { return isPaused; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getFormattedCurrentPosition() { return formattedCurrentPosition; }
    public LiveData<String> getFormattedDuration() { return formattedDuration; }
    public LiveData<Integer> getProgressPercentage() { return progressPercentage; }
    public LiveData<Boolean> getHasPrevious() { return hasPrevious; }
    public LiveData<Boolean> getHasNext() { return hasNext; }
    public LiveData<String> getContextTitle() { return contextTitle; }

    public LiveData<Boolean> getIsExpanded() { return isExpanded; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsPlayerVisible() { return mediaRepository.getIsPlayerVisible(); }

    // Backward compatibility aliases
    public LiveData<Boolean> getIsVisible() { return getIsPlayerVisible(); }
    public LiveData<Integer> getProgress() { return getProgressPercentage(); }

    // Mock getCurrentArtist for backward compatibility
    public LiveData<com.g3.soundify_musicplayer.data.entity.User> getCurrentArtist() {
        // Return a derived LiveData that extracts artist info from current song
        return Transformations.map(currentSong, song -> {
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

    @Override
    protected void onCleared() {
        super.onCleared();
        if (executor != null) {
            executor.shutdown();
        }
        if (mediaRepository != null) {
            mediaRepository.shutdown();
        }
    }
}
