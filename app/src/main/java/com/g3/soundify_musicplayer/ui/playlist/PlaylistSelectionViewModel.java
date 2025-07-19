package com.g3.soundify_musicplayer.ui.playlist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Playlist;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for Playlist Selection Screen
 * UI ONLY - Uses mock data for demonstration
 */
public class PlaylistSelectionViewModel extends AndroidViewModel {

    // LiveData for UI state
    private final MutableLiveData<List<Playlist>> playlists = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public PlaylistSelectionViewModel(@NonNull Application application) {
        super(application);
    }

    // Public methods for Activity to call
    public void loadPlaylists() {
        isLoading.setValue(true);
        
        // Simulate loading delay
        new Thread(() -> {
            try {
                Thread.sleep(500); // Simulate network delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Create mock playlist data
            List<Playlist> mockPlaylists = createMockPlaylists();
            
            // Update UI on main thread
            playlists.postValue(mockPlaylists);
            isLoading.postValue(false);
        }).start();
    }

    public void addSongToPlaylist(long songId, long playlistId) {
        // Mock implementation - in real app this would call repository
        // For demo purposes, we just simulate the action
    }

    // Getters for LiveData
    public MutableLiveData<List<Playlist>> getPlaylists() {
        return playlists;
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    // Mock data creation
    private List<Playlist> createMockPlaylists() {
        List<Playlist> mockPlaylists = new ArrayList<>();
        
        // Create sample playlists
        mockPlaylists.add(createMockPlaylist(1L, "My Favorites", "My personal favorite songs"));
        mockPlaylists.add(createMockPlaylist(2L, "Workout Mix", "High energy songs for workouts"));
        mockPlaylists.add(createMockPlaylist(3L, "Chill Vibes", "Relaxing music for downtime"));
        mockPlaylists.add(createMockPlaylist(4L, "Road Trip", "Perfect songs for long drives"));
        mockPlaylists.add(createMockPlaylist(5L, "Study Focus", "Instrumental and ambient music"));
        mockPlaylists.add(createMockPlaylist(6L, "Party Hits", "Upbeat songs for celebrations"));
        mockPlaylists.add(createMockPlaylist(7L, "Late Night", "Mellow tunes for evening listening"));
        mockPlaylists.add(createMockPlaylist(8L, "Throwback", "Classic hits from the past"));
        
        return mockPlaylists;
    }

    private Playlist createMockPlaylist(long id, String name, String description) {
        Playlist playlist = new Playlist(1L, name); // Owner ID = 1 (current user)
        playlist.setId(id);
        playlist.setDescription(description);
        playlist.setPublic(true);
        playlist.setCreatedAt(System.currentTimeMillis() - (id * 86400000L)); // Different creation times
        return playlist;
    }
}
