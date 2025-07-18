package com.g3.soundify_musicplayer.ui.player;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;

/**
 * ViewModel for the Full Player Screen
 * Uses mock data for UI testing - NO BACKEND INTEGRATION
 */
public class FullPlayerViewModel extends AndroidViewModel {
    
    // LiveData for UI state
    private final MutableLiveData<Song> currentSong = new MutableLiveData<>();
    private final MutableLiveData<User> currentArtist = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> currentTime = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isLiked = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isFollowing = new MutableLiveData<>(false);
    
    // Handler for progress simulation
    private Handler progressHandler;
    private Runnable progressRunnable;

    public FullPlayerViewModel(@NonNull Application application) {
        super(application);
        progressHandler = new Handler(Looper.getMainLooper());
    }

    // Public methods for Fragment to call
    public void loadSong(long songId) {
        // Create mock song data
        Song mockSong = createMockSong(songId);
        User mockArtist = createMockArtist();
        
        currentSong.setValue(mockSong);
        currentArtist.setValue(mockArtist);
        
        // Set initial UI state
        isPlaying.setValue(false);
        progress.setValue(0);
        currentTime.setValue(0);
        isLiked.setValue(false);
        isFollowing.setValue(false);
    }

    public void togglePlayPause() {
        Boolean currentState = isPlaying.getValue();
        boolean newState = currentState == null ? true : !currentState;
        isPlaying.setValue(newState);
        
        // Mock progress simulation when playing
        if (newState) {
            startProgressSimulation();
        } else {
            stopProgressSimulation();
        }
    }

    public void toggleLike(long songId) {
        Boolean currentLikeState = isLiked.getValue();
        boolean newState = currentLikeState == null ? true : !currentLikeState;
        isLiked.setValue(newState);
    }

    public void toggleFollow(long artistId) {
        Boolean currentFollowState = isFollowing.getValue();
        boolean newState = currentFollowState == null ? true : !currentFollowState;
        isFollowing.setValue(newState);
    }

    public void addComment(long songId, String content) {
        // Mock comment addition - just show success in UI
        // No actual backend operation
    }

    public void seekTo(int position) {
        currentTime.setValue(position);
        // Calculate progress percentage
        Song song = currentSong.getValue();
        if (song != null && song.getDurationMs() != null && song.getDurationMs() > 0) {
            int progressPercent = (position * 100) / song.getDurationMs();
            progress.setValue(progressPercent);
        }
    }

    // Mock data creation methods
    private Song createMockSong(long songId) {
        Song song = new Song();
        song.setId(songId);
        song.setTitle("Beautiful Sunset");
        song.setDescription("A relaxing instrumental track perfect for evening listening");
        song.setUploaderId(1L);
        song.setGenre("Ambient");
        song.setDurationMs(225000); // 3:45
        song.setPublic(true);
        song.setCreatedAt(System.currentTimeMillis() - 86400000); // 1 day ago
        song.setAudioUrl("mock://audio/beautiful_sunset.mp3");
        song.setCoverArtUrl("mock://images/sunset_cover.jpg");
        return song;
    }

    private User createMockArtist() {
        User user = new User();
        user.setId(1L);
        user.setUsername("ambient_artist");
        user.setDisplayName("Ambient Artist");
        user.setEmail("artist@example.com");
        user.setBio("Creating peaceful soundscapes for relaxation and focus");
        user.setAvatarUrl("mock://images/artist_avatar.jpg");
        user.setCreatedAt(System.currentTimeMillis() - 2592000000L); // 30 days ago
        return user;
    }

    // Progress simulation for mock playback
    private void startProgressSimulation() {
        stopProgressSimulation(); // Stop any existing simulation
        
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                Integer currentTimeValue = currentTime.getValue();
                Song song = currentSong.getValue();
                
                if (currentTimeValue != null && song != null && song.getDurationMs() != null) {
                    int newTime = currentTimeValue + 1000; // Increment by 1 second
                    
                    if (newTime >= song.getDurationMs()) {
                        // Song finished
                        isPlaying.setValue(false);
                        currentTime.setValue(0);
                        progress.setValue(0);
                        return;
                    }
                    
                    currentTime.setValue(newTime);
                    int progressPercent = (newTime * 100) / song.getDurationMs();
                    progress.setValue(progressPercent);
                    
                    // Continue simulation if still playing
                    Boolean stillPlaying = isPlaying.getValue();
                    if (stillPlaying != null && stillPlaying) {
                        progressHandler.postDelayed(this, 1000);
                    }
                }
            }
        };
        
        progressHandler.postDelayed(progressRunnable, 1000);
    }

    private void stopProgressSimulation() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }

    // Getters for LiveData
    public LiveData<Song> getCurrentSong() {
        return currentSong;
    }

    public LiveData<User> getCurrentArtist() {
        return currentArtist;
    }

    public LiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }

    public LiveData<Integer> getProgress() {
        return progress;
    }

    public LiveData<Integer> getCurrentTime() {
        return currentTime;
    }

    public LiveData<Boolean> getIsLiked() {
        return isLiked;
    }

    public LiveData<Boolean> getIsFollowing() {
        return isFollowing;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopProgressSimulation();
    }
}
