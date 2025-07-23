package com.g3.soundify_musicplayer.ui.library;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.dto.PlaylistWithSongCount;
import com.g3.soundify_musicplayer.data.dto.SongWithUploaderInfo;
import com.g3.soundify_musicplayer.data.repository.SongRepository;
import com.g3.soundify_musicplayer.data.repository.PlaylistRepository;
import com.g3.soundify_musicplayer.data.repository.MusicPlayerRepository;
import com.g3.soundify_musicplayer.utils.AuthManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for Library Screen
 * Updated to use real database data instead of mock data
 */
public class LibraryViewModel extends AndroidViewModel {

    // LiveData for each tab
    private LiveData<List<Song>> mySongs;
    private MutableLiveData<List<SongWithUploaderInfo>> mySongsWithUploaderInfo = new MutableLiveData<>();
    private LiveData<List<Playlist>> myPlaylists;
    private MutableLiveData<List<PlaylistWithSongCount>> myPlaylistsWithSongCount = new MutableLiveData<>();
    private LiveData<List<Song>> likedSongs;

    // Repositories
    private SongRepository songRepository;
    private PlaylistRepository playlistRepository;
    private MusicPlayerRepository musicPlayerRepository;
    private AuthManager authManager;

    // Executor for background tasks
    private ExecutorService executor = Executors.newFixedThreadPool(2);

    // Tab constants (match existing refreshTab method)
    public static final int TAB_MY_SONGS = 0;
    public static final int TAB_MY_PLAYLISTS = 1;
    public static final int TAB_LIKED_SONGS = 2;

    // LiveData for UI states
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<String> successMessage = new MutableLiveData<>();

    public LibraryViewModel(@NonNull Application application) {
        super(application);

        // Initialize repositories
        songRepository = new SongRepository(application);
        playlistRepository = new PlaylistRepository(application);
        musicPlayerRepository = new MusicPlayerRepository(application);
        authManager = new AuthManager(application);

        // Initialize data
        loadLibraryData();
    }

    // Getters for LiveData
    public LiveData<List<Song>> getMySongs() {
        return mySongs;
    }

    public LiveData<List<SongWithUploaderInfo>> getMySongsWithUploaderInfo() {
        return mySongsWithUploaderInfo;
    }

    public LiveData<List<Playlist>> getMyPlaylists() {
        return myPlaylists;
    }

    public LiveData<List<PlaylistWithSongCount>> getMyPlaylistsWithSongCount() {
        return myPlaylistsWithSongCount;
    }

    public LiveData<List<Song>> getLikedSongs() {
        return likedSongs;
    }

    /**
     * Load real library data for all tabs
     */
    public void loadLibraryData() {
        long currentUserId = authManager.getCurrentUserId();
        if (currentUserId != -1) {
            // Load my songs (songs I uploaded)
            mySongs = songRepository.getSongsByUploader(currentUserId);

            // Load my songs with uploader info
            loadMySongsWithUploaderInfo(currentUserId);

            // Load my playlists (playlists I created)
            myPlaylists = playlistRepository.getPlaylistsByOwner(currentUserId);

            // Load my playlists with song count
            loadPlaylistsWithSongCount(currentUserId);

            // Load liked songs (songs I liked)
            likedSongs = musicPlayerRepository.getLikedSongsByUser(currentUserId);
        } else {
            // User not logged in, show empty lists
            mySongs = new MutableLiveData<>(new ArrayList<>());
            mySongsWithUploaderInfo.setValue(new ArrayList<>());
            myPlaylists = new MutableLiveData<>(new ArrayList<>());
            likedSongs = new MutableLiveData<>(new ArrayList<>());
        }
    }

    /**
     * Refresh data for a specific tab
     */
    public void refreshTab(int tabIndex) {
        long currentUserId = authManager.getCurrentUserId();
        if (currentUserId == -1) return;

        switch (tabIndex) {
            case 0: // My Songs
                mySongs = songRepository.getSongsByUploader(currentUserId);
                loadMySongsWithUploaderInfo(currentUserId);
                break;
            case 1: // My Playlists
                myPlaylists = playlistRepository.getPlaylistsByOwner(currentUserId);
                loadPlaylistsWithSongCount(currentUserId);
                break;
            case 2: // Liked Songs
                likedSongs = musicPlayerRepository.getLikedSongsByUser(currentUserId);
                break;
        }
    }

    /**
     * Create new playlist
     */
    public void createPlaylist(String playlistName) {
        createPlaylist(playlistName, "", true); // Default: empty description, public
    }

    /**
     * Create new playlist with full details
     */
    public void createPlaylist(String playlistName, String description, boolean isPublic) {
        // Validation
        if (playlistName == null || playlistName.trim().isEmpty()) {
            errorMessage.setValue("Playlist name cannot be empty");
            return;
        }

        long currentUserId = authManager.getCurrentUserId();
        if (currentUserId == -1) {
            errorMessage.setValue("Please login first");
            return;
        }

        isLoading.setValue(true);
        errorMessage.setValue(null);

        // Create playlist in background thread
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Create playlist entity
                Playlist playlist = new Playlist(currentUserId, playlistName.trim());
                playlist.setDescription(description != null ? description.trim() : "");
                playlist.setPublic(isPublic);

                // Insert into database
                java.util.concurrent.Future<Long> future = playlistRepository.insert(playlist);
                Long playlistId = future.get();

                if (playlistId != null && playlistId > 0) {
                    // Success - refresh playlist data
                    refreshTab(1); // Refresh My Playlists tab
                    loadPlaylistsWithSongCount(currentUserId); // Refresh playlists with song count
                    successMessage.postValue("Playlist '" + playlistName + "' created successfully");
                } else {
                    errorMessage.postValue("Failed to create playlist");
                }

            } catch (Exception e) {
                android.util.Log.e("LibraryViewModel", "Error creating playlist", e);
                errorMessage.postValue("Error creating playlist: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    /**
     * Load playlists with song count for current user
     */
    private void loadPlaylistsWithSongCount(long userId) {
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                java.util.concurrent.Future<java.util.List<PlaylistWithSongCount>> future =
                    playlistRepository.getPlaylistsByOwnerWithSongCount(userId);
                java.util.List<PlaylistWithSongCount> playlists = future.get();
                myPlaylistsWithSongCount.postValue(playlists);
            } catch (Exception e) {
                android.util.Log.e("LibraryViewModel", "Error loading playlists with song count", e);
                myPlaylistsWithSongCount.postValue(new java.util.ArrayList<>());
            }
        });
    }

    // Getters for LiveData
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
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

    /**
     * Get current user ID
     */
    public long getCurrentUserId() {
        return authManager.getCurrentUserId();
    }

    /**
     * Update playlist information
     */
    public void updatePlaylist(long playlistId, String name, String description, boolean isPublic) {
        if (name == null || name.trim().isEmpty()) {
            errorMessage.setValue("Playlist name cannot be empty");
            return;
        }

        if (name.trim().length() > 100) {
            errorMessage.setValue("Playlist name is too long");
            return;
        }

        if (description != null && description.length() > 500) {
            errorMessage.setValue("Playlist description is too long");
            return;
        }

        android.util.Log.d("LibraryViewModel", "Updating playlist: " + name);
        isLoading.setValue(true);

        executor.execute(() -> {
            try {
                // Get current playlist
                Playlist playlist = playlistRepository.getPlaylistByIdSync(playlistId).get();
                if (playlist == null) {
                    errorMessage.postValue("Playlist not found");
                    isLoading.postValue(false);
                    return;
                }

                // Check ownership
                long currentUserId = authManager.getCurrentUserId();
                if (playlist.getOwnerId() != currentUserId) {
                    errorMessage.postValue("You can only edit your own playlists");
                    isLoading.postValue(false);
                    return;
                }

                // Update playlist
                playlist.setName(name.trim());
                playlist.setDescription(description);
                playlist.setPublic(isPublic);

                playlistRepository.update(playlist).get();

                android.util.Log.d("LibraryViewModel", "Successfully updated playlist");
                successMessage.postValue("Playlist updated successfully");

                // Refresh data
                refreshTab(TAB_MY_PLAYLISTS);

            } catch (Exception e) {
                android.util.Log.e("LibraryViewModel", "Error updating playlist", e);
                errorMessage.postValue("Error updating playlist: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    /**
     * Delete playlist
     */
    public void deletePlaylist(long playlistId) {
        android.util.Log.d("LibraryViewModel", "Deleting playlist: " + playlistId);
        isLoading.setValue(true);

        executor.execute(() -> {
            try {
                // Get current playlist
                Playlist playlist = playlistRepository.getPlaylistByIdSync(playlistId).get();
                if (playlist == null) {
                    errorMessage.postValue("Playlist not found");
                    isLoading.postValue(false);
                    return;
                }

                // Check ownership
                long currentUserId = authManager.getCurrentUserId();
                if (playlist.getOwnerId() != currentUserId) {
                    errorMessage.postValue("You can only delete your own playlists");
                    isLoading.postValue(false);
                    return;
                }

                // Delete playlist
                playlistRepository.delete(playlist).get();

                android.util.Log.d("LibraryViewModel", "Successfully deleted playlist");
                successMessage.postValue("Playlist deleted successfully");

                // Refresh data
                refreshTab(TAB_MY_PLAYLISTS);

            } catch (Exception e) {
                android.util.Log.e("LibraryViewModel", "Error deleting playlist", e);
                errorMessage.postValue("Error deleting playlist: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (songRepository != null) {
            songRepository.shutdown();
        }
        if (playlistRepository != null) {
            playlistRepository.shutdown();
        }
        if (musicPlayerRepository != null) {
            musicPlayerRepository.shutdown();
        }
    }

    /**
     * Load my songs with uploader information
     */
    private void loadMySongsWithUploaderInfo(long currentUserId) {
        // Use executor to run in background thread
        java.util.concurrent.Executor executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Get songs with uploader info from repository
                java.util.concurrent.Future<java.util.List<SongWithUploaderInfo>> future =
                    songRepository.getSongsByUploaderWithInfoSync(currentUserId);
                java.util.List<SongWithUploaderInfo> songsWithInfo = future.get();

                // Post to main thread
                mySongsWithUploaderInfo.postValue(songsWithInfo);
            } catch (Exception e) {
                e.printStackTrace();
                // Post empty list on error
                mySongsWithUploaderInfo.postValue(new java.util.ArrayList<>());
            }
        });
    }

}
