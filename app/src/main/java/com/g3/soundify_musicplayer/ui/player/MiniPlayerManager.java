package com.g3.soundify_musicplayer.ui.player;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;

/**
 * Singleton class to manage the global state of the mini player.
 * This ensures the mini player state is consistent across all activities and fragments.
 * UI ONLY - No backend integration, uses mock data for demo purposes.
 */
public class MiniPlayerManager {
    
    private static MiniPlayerManager instance;
    
    // LiveData for global mini player state
    private final MutableLiveData<Song> currentSong = new MutableLiveData<>();
    private final MutableLiveData<User> currentArtist = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isVisible = new MutableLiveData<>(false);
    
    private MiniPlayerManager() {
        // Private constructor for singleton
    }
    
    public static synchronized MiniPlayerManager getInstance() {
        if (instance == null) {
            instance = new MiniPlayerManager();
        }
        return instance;
    }
    
    // Public methods to control mini player
    public void showMiniPlayer(Song song, User artist) {
        currentSong.setValue(song);
        currentArtist.setValue(artist);
        isVisible.setValue(true);
        isPlaying.setValue(false);
        progress.setValue(0);
    }
    
    public void hideMiniPlayer() {
        isVisible.setValue(false);
    }
    
    public void togglePlayPause() {
        Boolean currentState = isPlaying.getValue();
        boolean newState = currentState == null ? true : !currentState;
        isPlaying.setValue(newState);
    }
    
    public void updateProgress(int progressMs) {
        progress.setValue(progressMs);
    }
    
    public void playNextTrack() {
        // For demo purposes, create a mock next song
        Song nextSong = createMockNextSong();
        User nextArtist = createMockNextArtist();
        
        currentSong.setValue(nextSong);
        currentArtist.setValue(nextArtist);
        progress.setValue(0);
        isPlaying.setValue(true);
    }
    
    // LiveData getters
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
    
    public LiveData<Boolean> getIsVisible() {
        return isVisible;
    }
    
    // Mock data creation methods
    private Song createMockNextSong() {
        Song song = new Song();
        song.setId(System.currentTimeMillis()); // Random ID
        
        // Rotate through different mock songs
        String[] titles = {"Midnight Dreams", "Ocean Waves", "City Lights", "Forest Path", "Starry Night"};
        String[] genres = {"Ambient", "Chill", "Electronic", "Acoustic", "Lofi"};
        
        int index = (int) (System.currentTimeMillis() % titles.length);
        
        song.setTitle(titles[index]);
        song.setDescription("A beautiful " + genres[index].toLowerCase() + " track");
        song.setUploaderId(2L);
        song.setGenre(genres[index]);
        song.setDurationMs(180000 + (index * 15000)); // 3-4 minutes
        song.setPublic(true);
        song.setCreatedAt(System.currentTimeMillis() - (index * 86400000L));
        song.setAudioUrl("mock://audio/" + titles[index].toLowerCase().replace(" ", "_") + ".mp3");
        song.setCoverArtUrl("mock://images/" + titles[index].toLowerCase().replace(" ", "_") + "_cover.jpg");
        
        return song;
    }
    
    private User createMockNextArtist() {
        User artist = new User();
        
        // Rotate through different mock artists
        String[] usernames = {"luna_beats", "echo_sound", "wave_music", "dream_audio", "star_tunes"};
        String[] displayNames = {"Luna Martinez", "Echo Thompson", "Wave Studios", "Dream Audio", "Star Tunes"};
        
        int index = (int) (System.currentTimeMillis() % usernames.length);
        
        artist.setId(2L + index);
        artist.setUsername(usernames[index]);
        artist.setDisplayName(displayNames[index]);
        artist.setAvatarUrl("mock://avatar/" + usernames[index] + ".jpg");
        artist.setCreatedAt(System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)); // 30 days ago
        
        return artist;
    }
}
