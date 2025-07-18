package com.g3.soundify_musicplayer.ui.playlist;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.repository.PlaylistRepository;
import com.g3.soundify_musicplayer.data.repository.SongRepository;
import com.g3.soundify_musicplayer.utils.AuthManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
// Note: Using manual filtering instead of streams for Android compatibility

/**
 * ViewModel for Select Songs Screen
 * Manages song loading, filtering, searching, and selection state
 */
public class SelectSongsViewModel extends AndroidViewModel {
    
    public enum FilterType {
        ALL_SONGS,
        MY_SONGS,
        PUBLIC_SONGS
    }
    
    private final SongRepository songRepository;
    private final PlaylistRepository playlistRepository;
    private final AuthManager authManager;
    
    // LiveData for UI state
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    
    // Filter and search state
    private final MutableLiveData<FilterType> currentFilter = new MutableLiveData<>(FilterType.ALL_SONGS);
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    
    // Song data
    private final LiveData<List<Song>> allSongs;
    private final MediatorLiveData<List<Song>> filteredSongs = new MediatorLiveData<>();
    private final MutableLiveData<Set<Long>> existingSongIds = new MutableLiveData<>(new HashSet<>());
    
    // Selection state
    private final MutableLiveData<Set<Long>> selectedSongIds = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<Integer> selectionCount = new MutableLiveData<>(0);
    
    // Current playlist ID
    private long currentPlaylistId = -1;
    
    public SelectSongsViewModel(@NonNull Application application) {
        super(application);
        this.songRepository = new SongRepository(application);
        this.playlistRepository = new PlaylistRepository(application);
        this.authManager = new AuthManager(application);
        
        // Load all songs
        this.allSongs = songRepository.getAllSongs();
        
        // Setup filtered songs mediator
        setupFilteredSongsMediator();
    }
    
    /**
     * Initialize for specific playlist
     */
    public void initializeForPlaylist(long playlistId) {
        this.currentPlaylistId = playlistId;
        loadExistingSongs();
    }
    
    /**
     * Load songs already in the playlist to exclude them
     */
    private void loadExistingSongs() {
        if (currentPlaylistId == -1) {
            return;
        }
        
        Future<List<Song>> future = playlistRepository.getSongsInPlaylistSync(currentPlaylistId);
        try {
            List<Song> existingSongs = future.get();
            Set<Long> existingIds = new HashSet<>();
            if (existingSongs != null) {
                for (Song song : existingSongs) {
                    existingIds.add(song.getId());
                }
            }
            existingSongIds.setValue(existingIds);
        } catch (ExecutionException | InterruptedException e) {
            // Handle error silently, continue with empty set
            existingSongIds.setValue(new HashSet<>());
        }
    }
    
    /**
     * Setup mediator for filtered songs
     */
    private void setupFilteredSongsMediator() {
        filteredSongs.addSource(allSongs, songs -> updateFilteredSongs());
        filteredSongs.addSource(currentFilter, filter -> updateFilteredSongs());
        filteredSongs.addSource(searchQuery, query -> updateFilteredSongs());
        filteredSongs.addSource(existingSongIds, ids -> updateFilteredSongs());
    }
    
    /**
     * Update filtered songs based on current filter, search, and existing songs
     */
    private void updateFilteredSongs() {
        List<Song> songs = allSongs.getValue();
        FilterType filter = currentFilter.getValue();
        String query = searchQuery.getValue();
        Set<Long> existing = existingSongIds.getValue();
        
        if (songs == null) {
            filteredSongs.setValue(new ArrayList<>());
            return;
        }
        
        List<Song> filtered = new ArrayList<>(songs);
        
        // Exclude existing songs in playlist
        if (existing != null && !existing.isEmpty()) {
            List<Song> temp = new ArrayList<>();
            for (Song song : filtered) {
                if (!existing.contains(song.getId())) {
                    temp.add(song);
                }
            }
            filtered = temp;
        }
        
        // Apply filter
        if (filter != null) {
            long currentUserId = authManager.getCurrentUserId();
            switch (filter) {
                case MY_SONGS:
                    if (currentUserId != -1) {
                        List<Song> temp = new ArrayList<>();
                        for (Song song : filtered) {
                            if (song.getUploaderId() == currentUserId) {
                                temp.add(song);
                            }
                        }
                        filtered = temp;
                    } else {
                        filtered = new ArrayList<>(); // No user logged in
                    }
                    break;
                case PUBLIC_SONGS:
                    List<Song> temp = new ArrayList<>();
                    for (Song song : filtered) {
                        if (song.isPublic()) {
                            temp.add(song);
                        }
                    }
                    filtered = temp;
                    break;
                case ALL_SONGS:
                default:
                    // Show all songs (already filtered by existing)
                    break;
            }
        }
        
        // Apply search query
        if (query != null && !query.trim().isEmpty()) {
            String searchTerm = query.trim().toLowerCase();
            List<Song> temp = new ArrayList<>();
            for (Song song : filtered) {
                String title = song.getTitle() != null ? song.getTitle().toLowerCase() : "";
                String genre = song.getGenre() != null ? song.getGenre().toLowerCase() : "";
                if (title.contains(searchTerm) || genre.contains(searchTerm)) {
                    temp.add(song);
                }
            }
            filtered = temp;
        }
        
        filteredSongs.setValue(filtered);
    }
    
    /**
     * Set current filter
     */
    public void setFilter(FilterType filter) {
        currentFilter.setValue(filter);
    }
    
    /**
     * Set search query
     */
    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }
    
    /**
     * Toggle song selection
     */
    public void toggleSongSelection(long songId) {
        Set<Long> selected = selectedSongIds.getValue();
        if (selected == null) {
            selected = new HashSet<>();
        } else {
            selected = new HashSet<>(selected); // Create new set to trigger observer
        }
        
        if (selected.contains(songId)) {
            selected.remove(songId);
        } else {
            selected.add(songId);
        }
        
        selectedSongIds.setValue(selected);
        selectionCount.setValue(selected.size());
    }
    
    /**
     * Check if song is selected
     */
    public boolean isSongSelected(long songId) {
        Set<Long> selected = selectedSongIds.getValue();
        return selected != null && selected.contains(songId);
    }
    
    /**
     * Clear all selections
     */
    public void clearSelection() {
        selectedSongIds.setValue(new HashSet<>());
        selectionCount.setValue(0);
    }
    
    /**
     * Get selected songs
     */
    public List<Song> getSelectedSongs() {
        Set<Long> selected = selectedSongIds.getValue();
        List<Song> filtered = filteredSongs.getValue();
        
        if (selected == null || selected.isEmpty() || filtered == null) {
            return new ArrayList<>();
        }
        
        List<Song> selectedSongs = new ArrayList<>();
        for (Song song : filtered) {
            if (selected.contains(song.getId())) {
                selectedSongs.add(song);
            }
        }
        return selectedSongs;
    }
    
    /**
     * Add selected songs to playlist
     */
    public void addSelectedSongsToPlaylist() {
        if (currentPlaylistId == -1) {
            errorMessage.setValue("Invalid playlist");
            return;
        }
        
        List<Song> selectedSongs = getSelectedSongs();
        if (selectedSongs.isEmpty()) {
            errorMessage.setValue("No songs selected");
            return;
        }
        
        isLoading.setValue(true);
        
        // Add songs to playlist in background
        new Thread(() -> {
            try {
                int addedCount = 0;
                for (Song song : selectedSongs) {
                    Future<Void> future = playlistRepository.addSongToPlaylist(currentPlaylistId, song.getId());
                    future.get();
                    addedCount++;
                }
                
                // Post success message on main thread
                String message = addedCount == 1 ? 
                    "1 song added to playlist" : 
                    addedCount + " songs added to playlist";
                successMessage.postValue(message);
                
            } catch (Exception e) {
                errorMessage.postValue("Error adding songs to playlist");
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }
    
    /**
     * Get selection count string
     */
    public String getSelectionCountString() {
        Integer count = selectionCount.getValue();
        if (count == null || count == 0) {
            return "None selected";
        } else if (count == 1) {
            return "1 selected";
        } else {
            return count + " selected";
        }
    }
    
    /**
     * Clear error message
     */
    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }
    
    /**
     * Clear success message
     */
    public void clearSuccessMessage() {
        successMessage.setValue(null);
    }
    
    // Getters for LiveData
    public LiveData<List<Song>> getFilteredSongs() {
        return filteredSongs;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }
    
    public LiveData<FilterType> getCurrentFilter() {
        return currentFilter;
    }
    
    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }
    
    public LiveData<Set<Long>> getSelectedSongIds() {
        return selectedSongIds;
    }
    
    public LiveData<Integer> getSelectionCount() {
        return selectionCount;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up resources
        songRepository.shutdown();
        playlistRepository.shutdown();
    }
}
