package com.g3.soundify_musicplayer.ui.search;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * ViewModel for the Search screen.
 * Provides mock data and handles search functionality for demo purposes.
 * UI ONLY - No backend integration.
 */
public class SearchViewModel extends AndroidViewModel {

    public enum FilterType {
        ALL, SONGS, ARTISTS, PLAYLISTS
    }

    private final MutableLiveData<List<SearchResult>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<FilterType> currentFilter = new MutableLiveData<>(FilterType.ALL);
    private final MutableLiveData<String> currentQuery = new MutableLiveData<>("");

    private List<SearchResult> allResults;
    private String lastQuery = "";

    public SearchViewModel(@NonNull Application application) {
        super(application);
        initializeMockData();
    }

    // Public methods for Fragment to call
    public void search(String query) {
        if (query == null) query = "";
        
        currentQuery.setValue(query);
        lastQuery = query.trim();
        
        if (lastQuery.isEmpty()) {
            searchResults.setValue(new ArrayList<>());
            return;
        }
        
        isLoading.setValue(true);
        
        // Simulate search delay
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Thread.sleep(300); // Simulate network delay
                
                List<SearchResult> filteredResults = filterResults(lastQuery, currentFilter.getValue());
                
                searchResults.postValue(filteredResults);
                isLoading.postValue(false);
                
            } catch (InterruptedException e) {
                error.postValue("Search failed");
                isLoading.postValue(false);
            }
        });
    }

    public void setFilter(FilterType filter) {
        currentFilter.setValue(filter);
        
        // Re-apply search with new filter
        if (!lastQuery.isEmpty()) {
            search(lastQuery);
        }
    }

    // LiveData getters
    public LiveData<List<SearchResult>> getSearchResults() {
        return searchResults;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<FilterType> getCurrentFilter() {
        return currentFilter;
    }

    public LiveData<String> getCurrentQuery() {
        return currentQuery;
    }

    // Private methods
    private void initializeMockData() {
        allResults = new ArrayList<>();
        
        // Create mock songs with artists
        allResults.addAll(createMockSongs());
        
        // Create mock artists
        allResults.addAll(createMockArtists());
        
        // Create mock playlists
        allResults.addAll(createMockPlaylists());
    }

    private List<SearchResult> createMockSongs() {
        List<SearchResult> songs = new ArrayList<>();
        
        // Mock artists
        User artist1 = createMockUser(1L, "luna_beats", "Luna Martinez");
        User artist2 = createMockUser(2L, "echo_sound", "Echo Thompson");
        User artist3 = createMockUser(3L, "wave_music", "Wave Studios");
        User artist4 = createMockUser(4L, "dream_audio", "Dream Audio");
        User artist5 = createMockUser(5L, "star_tunes", "Star Tunes");

        // Mock songs
        songs.add(new SearchResult(createMockSong(1L, "Beautiful Sunset", "Ambient", 225000, artist1.getId()), artist1));
        songs.add(new SearchResult(createMockSong(2L, "Ocean Waves", "Chill", 195000, artist2.getId()), artist2));
        songs.add(new SearchResult(createMockSong(3L, "City Lights", "Electronic", 210000, artist3.getId()), artist3));
        songs.add(new SearchResult(createMockSong(4L, "Forest Path", "Acoustic", 180000, artist4.getId()), artist4));
        songs.add(new SearchResult(createMockSong(5L, "Starry Night", "Lofi", 240000, artist5.getId()), artist5));
        songs.add(new SearchResult(createMockSong(6L, "Morning Coffee", "Jazz", 165000, artist1.getId()), artist1));
        songs.add(new SearchResult(createMockSong(7L, "Rainy Day", "Ambient", 200000, artist2.getId()), artist2));
        songs.add(new SearchResult(createMockSong(8L, "Summer Breeze", "Pop", 185000, artist3.getId()), artist3));
        
        return songs;
    }

    private List<SearchResult> createMockArtists() {
        List<SearchResult> artists = new ArrayList<>();
        
        artists.add(new SearchResult(createMockUser(1L, "luna_beats", "Luna Martinez"), 12));
        artists.add(new SearchResult(createMockUser(2L, "echo_sound", "Echo Thompson"), 8));
        artists.add(new SearchResult(createMockUser(3L, "wave_music", "Wave Studios"), 15));
        artists.add(new SearchResult(createMockUser(4L, "dream_audio", "Dream Audio"), 6));
        artists.add(new SearchResult(createMockUser(5L, "star_tunes", "Star Tunes"), 20));
        artists.add(new SearchResult(createMockUser(6L, "melody_maker", "Melody Maker"), 9));
        
        return artists;
    }

    private List<SearchResult> createMockPlaylists() {
        List<SearchResult> playlists = new ArrayList<>();
        
        User owner1 = createMockUser(1L, "luna_beats", "Luna Martinez");
        User owner2 = createMockUser(2L, "echo_sound", "Echo Thompson");
        User owner3 = createMockUser(3L, "wave_music", "Wave Studios");
        
        playlists.add(new SearchResult(createMockPlaylist(1L, "Chill Vibes", "Relaxing songs for any time", owner1.getId()), owner1, 25));
        playlists.add(new SearchResult(createMockPlaylist(2L, "Study Focus", "Perfect background music for studying", owner2.getId()), owner2, 18));
        playlists.add(new SearchResult(createMockPlaylist(3L, "Morning Energy", "Start your day with these upbeat tracks", owner3.getId()), owner3, 30));
        playlists.add(new SearchResult(createMockPlaylist(4L, "Night Drive", "Late night driving playlist", owner1.getId()), owner1, 22));
        playlists.add(new SearchResult(createMockPlaylist(5L, "Workout Beats", "High energy songs for your workout", owner2.getId()), owner2, 35));
        
        return playlists;
    }

    private List<SearchResult> filterResults(String query, FilterType filter) {
        List<SearchResult> filtered = new ArrayList<>();
        
        for (SearchResult result : allResults) {
            // Apply text filter
            if (!result.matchesQuery(query)) {
                continue;
            }
            
            // Apply type filter
            switch (filter) {
                case ALL:
                    filtered.add(result);
                    break;
                case SONGS:
                    if (result.getType() == SearchResult.Type.SONG) {
                        filtered.add(result);
                    }
                    break;
                case ARTISTS:
                    if (result.getType() == SearchResult.Type.ARTIST) {
                        filtered.add(result);
                    }
                    break;
                case PLAYLISTS:
                    if (result.getType() == SearchResult.Type.PLAYLIST) {
                        filtered.add(result);
                    }
                    break;
            }
        }
        
        return filtered;
    }

    // Helper methods to create mock data
    private Song createMockSong(long id, String title, String genre, int durationMs, long uploaderId) {
        Song song = new Song();
        song.setId(id);
        song.setTitle(title);
        song.setDescription("A beautiful " + genre.toLowerCase() + " track");
        song.setUploaderId(uploaderId);
        song.setGenre(genre);
        song.setDurationMs(durationMs);
        song.setPublic(true);
        song.setCreatedAt(System.currentTimeMillis() - (id * 86400000L));
        song.setAudioUrl("mock://audio/" + title.toLowerCase().replace(" ", "_") + ".mp3");
        song.setCoverArtUrl("mock://images/" + title.toLowerCase().replace(" ", "_") + "_cover.jpg");
        return song;
    }

    private User createMockUser(long id, String username, String displayName) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setBio("Music creator and artist");
        user.setAvatarUrl("mock://avatar/" + username + ".jpg");
        user.setCreatedAt(System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000));
        return user;
    }

    private Playlist createMockPlaylist(long id, String name, String description, long ownerId) {
        Playlist playlist = new Playlist(ownerId, name);
        playlist.setId(id);
        playlist.setDescription(description);
        playlist.setPublic(true);
        playlist.setCreatedAt(System.currentTimeMillis() - (id * 86400000L));
        return playlist;
    }
}
