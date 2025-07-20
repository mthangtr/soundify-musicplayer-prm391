package com.g3.soundify_musicplayer.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.RecentlyPlayed;
import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.PlaylistAccess;
import com.g3.soundify_musicplayer.data.repository.SongRepository;
import com.g3.soundify_musicplayer.data.repository.PlaylistRepository;
import com.g3.soundify_musicplayer.utils.AuthManager;

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
    private LiveData<List<Song>> recentSongs;
    private LiveData<List<Song>> suggestedSongs;
    private LiveData<List<Playlist>> recentPlaylists;
    
    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.songRepository = new SongRepository(application);
        this.playlistRepository = new PlaylistRepository(application);
        this.authManager = new AuthManager(application);
        
        // Initialize recent songs for current user
        initializeRecentSongs();

        // Initialize suggested songs (random)
        suggestedSongs = songRepository.getSuggestedSongs();

        // Initialize recent playlists for current user
        initializeRecentPlaylists();
    }
    
    private void initializeRecentSongs() {
        long currentUserId = authManager.getCurrentUserId();
        if (currentUserId != -1) {
            recentSongs = songRepository.getRecentSongs(currentUserId);
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
    
    /**
     * Get recent songs LiveData
     */
    public LiveData<List<Song>> getRecentSongs() {
        return recentSongs;
    }

    /**
     * Get suggested songs LiveData (10 random songs)
     */
    public LiveData<List<Song>> getSuggestedSongs() {
        return suggestedSongs;
    }

    /**
     * Get recent playlists LiveData (3 most recently accessed)
     */
    public LiveData<List<Playlist>> getRecentPlaylists() {
        return recentPlaylists;
    }
    
    /**
     * Track that user played a song
     * Call this when user clicks play button
     */
    public void trackRecentlyPlayed(long songId) {
        long currentUserId = authManager.getCurrentUserId();
        if (currentUserId != -1) {
            songRepository.trackRecentlyPlayed(currentUserId, songId);
            android.util.Log.d("HomeViewModel", "Tracked recently played: userId=" + currentUserId + ", songId=" + songId);
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
            android.util.Log.d("HomeViewModel", "Tracked playlist access: userId=" + currentUserId + ", playlistId=" + playlistId);
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
     * Refresh suggested songs (get new random songs)
     */
    public void refreshSuggestedSongs() {
        suggestedSongs = songRepository.getSuggestedSongs();
    }

    /**
     * Refresh recent playlists (if user changes)
     */
    public void refreshRecentPlaylists() {
        initializeRecentPlaylists();
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
