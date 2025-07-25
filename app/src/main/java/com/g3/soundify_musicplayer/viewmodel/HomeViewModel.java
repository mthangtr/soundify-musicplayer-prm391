package com.g3.soundify_musicplayer.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.RecentlyPlayed;
import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.dto.PlaylistWithSongCount;
import com.g3.soundify_musicplayer.data.entity.PlaylistAccess;
import com.g3.soundify_musicplayer.data.dto.SongWithUploader;
import com.g3.soundify_musicplayer.data.dto.SongWithUploaderInfo;
import com.g3.soundify_musicplayer.data.repository.SongRepository;
import com.g3.soundify_musicplayer.data.repository.PlaylistRepository;
import com.g3.soundify_musicplayer.utils.AuthManager;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for HomeFragment
 * Manages recently played songs, suggested songs, and user playlists
 */
public class HomeViewModel extends AndroidViewModel {
    
    private SongRepository songRepository;
    private PlaylistRepository playlistRepository;
    private AuthManager authManager;
    
    // LiveData for recent songs, suggested songs, and recent playlists
    private LiveData<List<SongWithUploaderInfo>> recentSongs;
    private LiveData<List<SongWithUploaderInfo>> suggestedSongs;
    private LiveData<List<Playlist>> recentPlaylists;
    private LiveData<List<PlaylistWithSongCount>> userPlaylists;
    
    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.songRepository = new SongRepository(application);
        this.playlistRepository = new PlaylistRepository(application);
        this.authManager = new AuthManager(application);
        
        // Initialize recent songs for current user
        initializeRecentSongs();

        // Initialize suggested songs (random) with uploader info
        suggestedSongs = songRepository.getSuggestedSongsWithUploaderInfo();

        // Initialize recent playlists for current user
        initializeRecentPlaylists();

        // Initialize user playlists (all playlists owned by user)
        initializeUserPlaylists();
    }
    
    private void initializeRecentSongs() {
        long currentUserId = authManager.getCurrentUserId();
        if (currentUserId != -1) {
            recentSongs = songRepository.getRecentSongsWithUploaderInfo(currentUserId);
        } else {
            recentSongs = new MutableLiveData<>();
        }
    }

    private void initializeRecentPlaylists() {
        long currentUserId = authManager.getCurrentUserId();
        if (currentUserId != -1) {
            recentPlaylists = playlistRepository.getRecentlyAccessedPlaylists(currentUserId);
        } else {
            recentPlaylists = new MutableLiveData<>();
        }
    }

    private void initializeUserPlaylists() {
        long currentUserId = authManager.getCurrentUserId();
        if (currentUserId != -1) {
            // Create MutableLiveData and load data asynchronously
            MutableLiveData<List<PlaylistWithSongCount>> liveData = new MutableLiveData<>();
            userPlaylists = liveData;

            // Load playlists with song count in background using executor
            java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    List<PlaylistWithSongCount> playlists = playlistRepository.getPlaylistsByOwnerWithSongCount(currentUserId).get();
                    liveData.postValue(playlists);
                } catch (Exception e) {
                    android.util.Log.e("HomeViewModel", "Error loading user playlists", e);
                    liveData.postValue(new ArrayList<>());
                }
            });
        } else {
            userPlaylists = new MutableLiveData<>();
        }
    }
    
    /**
     * Get recent songs LiveData with uploader information
     */
    public LiveData<List<SongWithUploaderInfo>> getRecentSongs() {
        return recentSongs;
    }

    /**
     * Get suggested songs LiveData with uploader information (10 random songs)
     */
    public LiveData<List<SongWithUploaderInfo>> getSuggestedSongs() {
        return suggestedSongs;
    }

    /**
     * Get recent playlists LiveData (3 most recently accessed)
     */
    public LiveData<List<Playlist>> getRecentPlaylists() {
        return recentPlaylists;
    }

    /**
     * Get all user playlists LiveData (all playlists owned by current user)
     */
    public LiveData<List<PlaylistWithSongCount>> getUserPlaylists() {
        return userPlaylists;
    }
    
    /**
     * Track that user played a song
     * Call this when user clicks play button
     */
    public void trackRecentlyPlayed(long songId) {
        long currentUserId = authManager.getCurrentUserId();
        if (currentUserId != -1) {
            songRepository.trackRecentlyPlayed(currentUserId, songId);

            // Refresh recent songs to show the newly tracked song
            refreshRecentSongs();
        } else {
            android.util.Log.w("HomeViewModel", "Cannot track recently played - user not logged in");
        }
    }

    /**
     * Track that user accessed a playlist
     * Call this when user clicks on a playlist
     */
    public void trackPlaylistAccess(long playlistId) {
        long currentUserId = authManager.getCurrentUserId();
        if (currentUserId != -1) {
            playlistRepository.trackPlaylistAccess(currentUserId, playlistId);
        } else {
            android.util.Log.w("HomeViewModel", "Cannot track playlist access - user not logged in");
        }
    }
    
    /**
     * Refresh recent songs (if user changes)
     */
    public void refreshRecentSongs() {
        initializeRecentSongs();
    }

    /**
     * Refresh suggested songs (get new random songs with uploader info)
     */
    public void refreshSuggestedSongs() {
        suggestedSongs = songRepository.getSuggestedSongsWithUploaderInfo();
    }

    /**
     * Refresh recent playlists (if user changes)
     */
    public void refreshRecentPlaylists() {
        initializeRecentPlaylists();
    }

    /**
     * Refresh user playlists (if user changes or creates new playlist)
     */
    public void refreshUserPlaylists() {
        initializeUserPlaylists();
    }
    
    /**
     * Get all recently played records for debugging
     */
    public LiveData<List<RecentlyPlayed>> getAllRecentlyPlayed() {
        long currentUserId = authManager.getCurrentUserId();
        if (currentUserId != -1) {
            return songRepository.getAllRecentlyPlayed(currentUserId);
        } else {
            return new MutableLiveData<>();
        }
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
    }
}
