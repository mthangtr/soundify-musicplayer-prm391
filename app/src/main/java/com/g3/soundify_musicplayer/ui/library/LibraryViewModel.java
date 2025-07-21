package com.g3.soundify_musicplayer.ui.library;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.repository.SongRepository;
import com.g3.soundify_musicplayer.data.repository.PlaylistRepository;
import com.g3.soundify_musicplayer.data.repository.MusicPlayerRepository;
import com.g3.soundify_musicplayer.utils.AuthManager;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for Library Screen
 * Updated to use real database data instead of mock data
 */
public class LibraryViewModel extends AndroidViewModel {

    // LiveData for each tab
    private LiveData<List<Song>> mySongs;
    private LiveData<List<Playlist>> myPlaylists;
    private LiveData<List<Song>> likedSongs;

    // Repositories
    private SongRepository songRepository;
    private PlaylistRepository playlistRepository;
    private MusicPlayerRepository musicPlayerRepository;
    private AuthManager authManager;

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

    public LiveData<List<Playlist>> getMyPlaylists() {
        return myPlaylists;
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

            // Load my playlists (playlists I created)
            myPlaylists = playlistRepository.getPlaylistsByOwner(currentUserId);

            // Load liked songs (songs I liked)
            likedSongs = musicPlayerRepository.getLikedSongsByUser(currentUserId);
        } else {
            // User not logged in, show empty lists
            mySongs = new MutableLiveData<>(new ArrayList<>());
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
                break;
            case 1: // My Playlists
                myPlaylists = playlistRepository.getPlaylistsByOwner(currentUserId);
                break;
            case 2: // Liked Songs
                likedSongs = musicPlayerRepository.getLikedSongsByUser(currentUserId);
                break;
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
        if (musicPlayerRepository != null) {
            musicPlayerRepository.shutdown();
        }
    }
}
