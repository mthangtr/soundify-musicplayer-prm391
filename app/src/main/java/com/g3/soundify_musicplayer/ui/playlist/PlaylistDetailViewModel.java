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
import java.util.concurrent.Future;

/**
 * ViewModel for Playlist Detail Screen
 * Manages playlist data, songs list, and user interactions
 */
public class PlaylistDetailViewModel extends AndroidViewModel {
    
    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;
    private final AuthManager authManager;
    
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
        
        isLoading.setValue(true);
        
        Future<Void> future = playlistRepository.delete(playlist);
        try {
            future.get();
            successMessage.setValue("Playlist deleted");
        } catch (ExecutionException | InterruptedException e) {
            errorMessage.setValue("Error deleting playlist");
        } finally {
            isLoading.setValue(false);
        }
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
        playlistRepository.shutdown();
        userRepository.shutdown();
    }
}
