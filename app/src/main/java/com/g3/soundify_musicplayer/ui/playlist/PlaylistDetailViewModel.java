package com.g3.soundify_musicplayer.ui.playlist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.repository.PlaylistRepository;
import com.g3.soundify_musicplayer.data.repository.UserRepository;
import com.g3.soundify_musicplayer.utils.AuthManager;
import com.g3.soundify_musicplayer.utils.TimeUtils;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ViewModel for Playlist Detail Screen
 * Manages playlist data, songs list, and user interactions
 */
public class PlaylistDetailViewModel extends AndroidViewModel {
    
    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;
    private final AuthManager authManager;
    private final ExecutorService executor;
    
    // LiveData for UI state
    private final MutableLiveData<Playlist> currentPlaylist = new MutableLiveData<>();
    private final MutableLiveData<User> playlistOwner = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isOwner = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    
    // Playlist stats
    private final MutableLiveData<Integer> songCount = new MutableLiveData<>(0);
    private final MutableLiveData<String> totalDuration = new MutableLiveData<>("0:00");
    
    // Current playlist ID
    private long currentPlaylistId = -1;
    
    public PlaylistDetailViewModel(@NonNull Application application) {
        super(application);
        this.playlistRepository = new PlaylistRepository(application);
        this.userRepository = new UserRepository(application);
        this.authManager = new AuthManager(application);
        this.executor = Executors.newFixedThreadPool(2);
    }
    
    /**
     * Load playlist data by ID
     */
    public void loadPlaylist(long playlistId) {
        if (currentPlaylistId == playlistId) {
            return; // Already loaded
        }

        currentPlaylistId = playlistId;
        isLoading.setValue(true);

        // Load playlist data
        LiveData<Playlist> playlistLiveData = playlistRepository.getPlaylistById(playlistId);
        playlistLiveData.observeForever(playlist -> {
            if (playlist != null) {
                currentPlaylist.setValue(playlist);
                loadPlaylistOwner(playlist.getOwnerId());
                checkOwnership(playlist.getOwnerId());
            } else {
                errorMessage.setValue("Playlist not found");
            }
            isLoading.setValue(false);
        });
    }
    
    /**
     * Load playlist owner information
     */
    private void loadPlaylistOwner(long ownerId) {
        Future<User> future = userRepository.getUserByIdSync(ownerId);
        try {
            User owner = future.get();
            playlistOwner.setValue(owner);
        } catch (ExecutionException | InterruptedException e) {
            // Handle error silently, owner info is not critical
        }
    }
    
    /**
     * Check if current user is the playlist owner
     */
    private void checkOwnership(long ownerId) {
        long currentUserId = authManager.getCurrentUserId();
        isOwner.setValue(currentUserId != -1 && currentUserId == ownerId);
    }
    
    /**
     * Get songs in playlist with LiveData
     */
    public LiveData<List<Song>> getSongsInPlaylist() {
        if (currentPlaylistId != -1) {
            return playlistRepository.getSongsInPlaylist(currentPlaylistId);
        }

        return new MutableLiveData<>();
    }
    
    /**
     * Update playlist stats when songs change
     */
    public void updatePlaylistStats(List<Song> songs) {
        if (songs == null) {
            songCount.setValue(0);
            totalDuration.setValue("0:00");
            return;
        }
        
        songCount.setValue(songs.size());
        
        // Calculate total duration
        long totalMs = 0;
        for (Song song : songs) {
            if (song.getDurationMs() != null) {
                totalMs += song.getDurationMs();
            }
        }
        
        totalDuration.setValue(TimeUtils.formatDuration((int) totalMs));
    }
    
    /**
     * Remove song from playlist
     */
    public void removeSongFromPlaylist(long songId, String songTitle) {
        if (currentPlaylistId == -1) {
            errorMessage.setValue("Invalid playlist");
            return;
        }
        
        isLoading.setValue(true);
        
        Future<Void> future = playlistRepository.removeSongFromPlaylist(currentPlaylistId, songId);
        try {
            future.get();
            successMessage.setValue("\"" + songTitle + "\" removed from playlist");
        } catch (ExecutionException | InterruptedException e) {
            errorMessage.setValue("Error removing song from playlist");
        } finally {
            isLoading.setValue(false);
        }
    }
    
    /**
     * Update song position in playlist
     */
    public void updateSongPosition(long songId, int newPosition) {
        if (currentPlaylistId == -1) {
            errorMessage.setValue("Invalid playlist");
            return;
        }
        
        Future<Void> future = playlistRepository.updateSongPosition(currentPlaylistId, songId, newPosition);
        try {
            future.get();
            successMessage.setValue("Song position updated");
        } catch (ExecutionException | InterruptedException e) {
            errorMessage.setValue("Error updating song position");
        }
    }
    
    /**
     * Delete entire playlist (owner only)
     */
    public void deletePlaylist() {
        Playlist playlist = currentPlaylist.getValue();
        if (playlist == null) {
            errorMessage.setValue("No playlist to delete");
            return;
        }

        Boolean ownerCheck = isOwner.getValue();
        if (ownerCheck == null || !ownerCheck) {
            errorMessage.setValue("Only playlist owner can delete");
            return;
        }

        android.util.Log.d("PlaylistDetailViewModel", "Deleting playlist: " + playlist.getName());
        isLoading.setValue(true);

        // Execute deletion in background
        executor.execute(() -> {
            try {
                playlistRepository.delete(playlist).get();
                android.util.Log.d("PlaylistDetailViewModel", "Successfully deleted playlist");

                // Post success message on main thread
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    successMessage.setValue("Playlist deleted successfully");
                    isLoading.setValue(false);
                });

            } catch (Exception e) {
                android.util.Log.e("PlaylistDetailViewModel", "Error deleting playlist", e);

                // Post error message on main thread
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    errorMessage.setValue("Error deleting playlist: " + e.getMessage());
                    isLoading.setValue(false);
                });
            }
        });
    }
    
    /**
     * Get formatted playlist info string
     */
    public String getPlaylistInfoString() {
        User owner = playlistOwner.getValue();
        Integer count = songCount.getValue();
        String duration = totalDuration.getValue();

        StringBuilder info = new StringBuilder();

        // Add owner info
        if (owner != null) {
            info.append("Created by ").append(owner.getDisplayName());
        }

        // Add song count
        if (count != null && count > 0) {
            if (info.length() > 0) info.append(" • ");
            if (count == 1) {
                info.append("1 song");
            } else {
                info.append(count).append(" songs");
            }
        }

        // Add duration
        if (duration != null && !duration.equals("0:00")) {
            if (info.length() > 0) info.append(" • ");
            info.append(duration);
        }

        return info.toString();
    }

    /**
     * Remove song from playlist
     */
    public void removeSongFromPlaylist(long songId) {
        if (currentPlaylistId == -1) {
            android.util.Log.e("PlaylistDetailViewModel", "Cannot remove song - no playlist loaded");
            return;
        }

        android.util.Log.d("PlaylistDetailViewModel", "Removing song " + songId + " from playlist " + currentPlaylistId);

        // Execute removal in background
        executor.execute(() -> {
            try {
                playlistRepository.removeSongFromPlaylist(currentPlaylistId, songId).get();
                android.util.Log.d("PlaylistDetailViewModel", "Successfully removed song from playlist");

                // Refresh playlist data on main thread
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    loadPlaylist(currentPlaylistId); // Reload to refresh UI
                });

            } catch (Exception e) {
                android.util.Log.e("PlaylistDetailViewModel", "Error removing song from playlist", e);

                // Post error message on main thread
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    errorMessage.setValue("Error removing song from playlist: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Update playlist name
     */
    public void updatePlaylistName(String newName) {
        if (currentPlaylistId == -1) {
            android.util.Log.e("PlaylistDetailViewModel", "Cannot update playlist - no playlist loaded");
            errorMessage.setValue("No playlist loaded");
            return;
        }

        if (newName == null || newName.trim().isEmpty()) {
            android.util.Log.e("PlaylistDetailViewModel", "Cannot update playlist - name is empty");
            errorMessage.setValue("Playlist name cannot be empty");
            return;
        }

        if (newName.trim().length() > 50) {
            android.util.Log.e("PlaylistDetailViewModel", "Cannot update playlist - name too long");
            errorMessage.setValue("Playlist name is too long");
            return;
        }

        android.util.Log.d("PlaylistDetailViewModel", "Updating playlist " + currentPlaylistId + " with new name: " + newName.trim());

        // Execute update in background
        executor.execute(() -> {
            try {
                // Get current playlist from LiveData
                Playlist currentPlaylist = this.currentPlaylist.getValue();

                if (currentPlaylist == null) {
                    android.util.Log.e("PlaylistDetailViewModel", "Cannot find playlist to update");
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        errorMessage.setValue("Playlist not found");
                    });
                    return;
                }

                // Update playlist name
                currentPlaylist.setName(newName.trim());

                // Save to database
                playlistRepository.update(currentPlaylist).get();
                android.util.Log.d("PlaylistDetailViewModel", "Successfully updated playlist name");

                // Refresh playlist data on main thread
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    loadPlaylist(currentPlaylistId); // Reload to refresh UI
                });

            } catch (Exception e) {
                android.util.Log.e("PlaylistDetailViewModel", "Error updating playlist name", e);

                // Post error message on main thread
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    errorMessage.setValue("Error updating playlist: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Get formatted songs count string
     */
    public String getSongsCountString() {
        Integer count = songCount.getValue();
        if (count == null || count == 0) {
            return "No songs";
        } else if (count == 1) {
            return "1 song";
        } else {
            return count + " songs";
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
    public LiveData<Playlist> getCurrentPlaylist() {
        return currentPlaylist;
    }
    
    public LiveData<User> getPlaylistOwner() {
        return playlistOwner;
    }
    
    public LiveData<Boolean> getIsOwner() {
        return isOwner;
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
    
    public LiveData<Integer> getSongCount() {
        return songCount;
    }
    
    public LiveData<String> getTotalDuration() {
        return totalDuration;
    }
    
    public long getCurrentPlaylistId() {
        return currentPlaylistId;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up resources
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        playlistRepository.shutdown();
        userRepository.shutdown();
    }
}
