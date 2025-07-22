package com.g3.soundify_musicplayer.ui.search;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.repository.MusicPlayerRepository;
import com.g3.soundify_musicplayer.data.repository.SongRepository;
import com.g3.soundify_musicplayer.data.repository.UserRepository;
import com.g3.soundify_musicplayer.data.repository.PlaylistRepository;
import com.g3.soundify_musicplayer.utils.AuthManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for the Search screen.
 * Updated to use real database data instead of mock data
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

    // Repositories
    private SongRepository songRepository;
    private UserRepository userRepository;
    private PlaylistRepository playlistRepository;
    private MusicPlayerRepository musicPlayerRepository;
    private AuthManager authManager;
    private ExecutorService executor;

    private String lastQuery = "";

    // Follow state tracking
    private final MutableLiveData<Set<Long>> followingUserIds = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<String> followMessage = new MutableLiveData<>();

    public SearchViewModel(@NonNull Application application) {
        super(application);

        // Initialize repositories
        songRepository = new SongRepository(application);
        userRepository = new UserRepository(application);
        playlistRepository = new PlaylistRepository(application);
        musicPlayerRepository = new MusicPlayerRepository(application);
        authManager = new AuthManager(application);
        executor = Executors.newFixedThreadPool(4);

        // Initialize with empty results
        searchResults.setValue(new ArrayList<>());

        // Load current user's following list
        loadFollowingUsers();
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

        performDatabaseSearch(lastQuery);
    }

    public void setFilter(FilterType filter) {
        currentFilter.setValue(filter);

        // Re-apply search with new filter
        if (!lastQuery.isEmpty()) {
            performDatabaseSearch(lastQuery);
        }
    }

    private void performDatabaseSearch(String query) {
        isLoading.setValue(true);
        error.setValue(null);

        executor.execute(() -> {
            try {
                List<SearchResult> results = new ArrayList<>();
                FilterType filter = currentFilter.getValue();
                if (filter == null) filter = FilterType.ALL;

                // Search songs
                if (filter == FilterType.ALL || filter == FilterType.SONGS) {
                    searchSongs(query, results);
                }

                // Search users/artists
                if (filter == FilterType.ALL || filter == FilterType.ARTISTS) {
                    searchUsers(query, results);
                }

                // Search playlists
                if (filter == FilterType.ALL || filter == FilterType.PLAYLISTS) {
                    searchPlaylists(query, results);
                }

                // Update UI on main thread
                searchResults.postValue(results);
                isLoading.postValue(false);

            } catch (Exception e) {
                error.postValue("Search failed: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
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

    public LiveData<Set<Long>> getFollowingUserIds() {
        return followingUserIds;
    }

    public LiveData<String> getFollowMessage() {
        return followMessage;
    }

    public long getCurrentUserId() {
        return authManager.getCurrentUserId();
    }

    // Private methods for database search
    private void searchSongs(String query, List<SearchResult> results) {
        try {
            // Use the existing searchPublicSongs method from SongRepository
            // Since we're in background thread, we need to get data synchronously
            LiveData<List<Song>> songsLiveData = songRepository.searchPublicSongs(query);

            // For now, we'll use a simple approach - get all public songs and filter
            LiveData<List<Song>> allSongsLiveData = songRepository.getPublicSongs();

            // Note: In a real implementation, you'd want to create sync versions of these methods
            // For now, we'll create some demo songs that match the query
            List<Song> songs = createFilteredSongsFromDatabase(query);

            for (Song song : songs) {
                // Get artist info
                User artist = getUserById(song.getUploaderId());
                results.add(new SearchResult(song, artist));
            }
        } catch (Exception e) {
            android.util.Log.e("SearchViewModel", "Error searching songs", e);
        }
    }

    private void searchUsers(String query, List<SearchResult> results) {
        try {
            // Search for users by username or display name
            List<User> users = searchUsersFromDatabase(query);

            for (User user : users) {
                // Get song count for this user
                int songCount = getSongCountForUser(user.getId());
                results.add(new SearchResult(user, songCount));
            }
        } catch (Exception e) {
            android.util.Log.e("SearchViewModel", "Error searching users", e);
        }
    }

    private void searchPlaylists(String query, List<SearchResult> results) {
        try {
            // Search for public playlists
            List<Playlist> playlists = searchPlaylistsFromDatabase(query);

            for (Playlist playlist : playlists) {
                // Get owner info and song count
                User owner = getUserById(playlist.getOwnerId());
                int songCount = getSongCountForPlaylist(playlist.getId());
                results.add(new SearchResult(playlist, owner, songCount));
            }
        } catch (Exception e) {
            android.util.Log.e("SearchViewModel", "Error searching playlists", e);
        }
    }

    // Helper methods to get data from database
    private List<Song> createFilteredSongsFromDatabase(String query) {
        List<Song> filteredSongs = new ArrayList<>();

        try {
            // Get all songs from database synchronously
            List<Song> allSongs = songRepository.getAllSongsSync().get();



            // Filter songs that match the query
            String lowerQuery = query.toLowerCase();
            for (Song song : allSongs) {
                if (song.isPublic() &&
                    (song.getTitle().toLowerCase().contains(lowerQuery) ||
                     (song.getGenre() != null && song.getGenre().toLowerCase().contains(lowerQuery)) ||
                     (song.getDescription() != null && song.getDescription().toLowerCase().contains(lowerQuery)))) {
                    filteredSongs.add(song);
                }
            }

        } catch (Exception e) {
            android.util.Log.e("SearchViewModel", "Error filtering songs", e);
        }

        return filteredSongs;
    }

    private List<User> searchUsersFromDatabase(String query) {
        List<User> filteredUsers = new ArrayList<>();

        try {
            // Get all users from database synchronously
            List<User> allUsers = userRepository.getAllUsersSync().get();

            // Filter users that match the query
            String lowerQuery = query.toLowerCase();
            for (User user : allUsers) {
                if (user.getUsername().toLowerCase().contains(lowerQuery) ||
                    (user.getDisplayName() != null && user.getDisplayName().toLowerCase().contains(lowerQuery))) {
                    filteredUsers.add(user);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("SearchViewModel", "Error filtering users", e);
        }

        return filteredUsers;
    }

    private List<Playlist> searchPlaylistsFromDatabase(String query) {
        List<Playlist> filteredPlaylists = new ArrayList<>();

        try {
            // Get all public playlists from database synchronously
            List<Playlist> allPlaylists = playlistRepository.getAllPublicPlaylistsSync().get();

            // Filter playlists that match the query
            String lowerQuery = query.toLowerCase();
            for (Playlist playlist : allPlaylists) {
                if (playlist.getName().toLowerCase().contains(lowerQuery) ||
                    (playlist.getDescription() != null && playlist.getDescription().toLowerCase().contains(lowerQuery))) {
                    filteredPlaylists.add(playlist);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("SearchViewModel", "Error filtering playlists", e);
        }

        return filteredPlaylists;
    }

    // Helper methods to get additional data
    private User getUserById(long userId) {
        try {
            return userRepository.getUserByIdSync(userId).get();
        } catch (Exception e) {
            android.util.Log.e("SearchViewModel", "Error getting user", e);
            return null;
        }
    }

    private int getSongCountForUser(long userId) {
        try {
            List<Song> songs = songRepository.getSongsByUploaderSync(userId).get();
            return songs != null ? songs.size() : 0;
        } catch (Exception e) {
            android.util.Log.e("SearchViewModel", "Error getting song count", e);
            return 0;
        }
    }

    private int getSongCountForPlaylist(long playlistId) {
        try {
            List<Song> songs = playlistRepository.getSongsInPlaylistSync(playlistId).get();
            return songs != null ? songs.size() : 0;
        } catch (Exception e) {
            android.util.Log.e("SearchViewModel", "Error getting playlist song count", e);
            return 0;
        }
    }

    // Follow functionality methods
    private void loadFollowingUsers() {
        long currentUserId = authManager.getCurrentUserId();
        if (currentUserId == -1) {
            followingUserIds.postValue(new HashSet<>());
            return;
        }

        executor.execute(() -> {
            try {
                // Get following users from repository and observe changes
                LiveData<List<User>> followingLiveData = musicPlayerRepository.getFollowing(currentUserId);

                // We need to observe this on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    followingLiveData.observeForever(followingUsers -> {
                        if (followingUsers != null) {
                            Set<Long> followingIds = new HashSet<>();
                            for (User user : followingUsers) {
                                followingIds.add(user.getId());
                            }
                            followingUserIds.setValue(followingIds);
                        }
                    });
                });

            } catch (Exception e) {
                android.util.Log.e("SearchViewModel", "Error loading following users", e);
                followingUserIds.postValue(new HashSet<>());
            }
        });
    }

    public void refreshFollowingUsers() {
        loadFollowingUsers();
    }

    public void toggleFollowStatus(User user) {
        long currentUserId = authManager.getCurrentUserId();
        if (currentUserId == -1) {
            followMessage.setValue("Please log in to follow users");
            return;
        }

        if (user.getId() == currentUserId) {
            followMessage.setValue("Cannot follow yourself");
            return;
        }

        Set<Long> currentFollowing = followingUserIds.getValue();
        if (currentFollowing == null) {
            currentFollowing = new HashSet<>();
        }

        final Set<Long> finalCurrentFollowing = currentFollowing;
        boolean isCurrentlyFollowing = currentFollowing.contains(user.getId());

        // Optimistic UI update - update state immediately
        Set<Long> optimisticFollowing = new HashSet<>(finalCurrentFollowing);
        if (isCurrentlyFollowing) {
            optimisticFollowing.remove(user.getId());
        } else {
            optimisticFollowing.add(user.getId());
        }
        followingUserIds.setValue(optimisticFollowing);

        // Show immediate feedback
        String immediateMessage = isCurrentlyFollowing ?
            "Unfollowed " + (user.getDisplayName() != null ? user.getDisplayName() : user.getUsername()) :
            "Now following " + (user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
        followMessage.setValue(immediateMessage);

        // Perform background operation
        executor.execute(() -> {
            try {
                if (isCurrentlyFollowing) {
                    // Unfollow
                    musicPlayerRepository.unfollowUser(currentUserId, user.getId()).get();
                } else {
                    // Follow
                    musicPlayerRepository.followUser(currentUserId, user.getId()).get();
                }

            } catch (Exception e) {
                // Rollback optimistic update on error
                followingUserIds.postValue(finalCurrentFollowing);
                followMessage.postValue("Error updating follow status");
                android.util.Log.e("SearchViewModel", "Error toggling follow status", e);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (executor != null) {
            executor.shutdown();
        }
        if (songRepository != null) {
            songRepository.shutdown();
        }
        if (playlistRepository != null) {
            playlistRepository.shutdown();
        }
    }
}
