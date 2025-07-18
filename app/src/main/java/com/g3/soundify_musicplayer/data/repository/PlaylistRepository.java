package com.g3.soundify_musicplayer.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.g3.soundify_musicplayer.data.database.AppDatabase;
import com.g3.soundify_musicplayer.data.dao.PlaylistDao;
import com.g3.soundify_musicplayer.data.dao.PlaylistSongDao;
import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.PlaylistSong;
import com.g3.soundify_musicplayer.data.entity.Song;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PlaylistRepository {
    
    private PlaylistDao playlistDao;
    private PlaylistSongDao playlistSongDao;
    private ExecutorService executor;
    
    public PlaylistRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        playlistDao = database.playlistDao();
        playlistSongDao = database.playlistSongDao();
        executor = Executors.newFixedThreadPool(4);
    }
    
    // Playlist CRUD
    public Future<Long> insert(Playlist playlist) {
        return executor.submit(() -> playlistDao.insert(playlist));
    }
    
    public LiveData<Playlist> getPlaylistById(long playlistId) {
        return playlistDao.getPlaylistById(playlistId);
    }
    
    public Future<Playlist> getPlaylistByIdSync(long playlistId) {
        return executor.submit(() -> playlistDao.getPlaylistByIdSync(playlistId));
    }
    
    public LiveData<List<Playlist>> getPlaylistsByOwner(long ownerId) {
        return playlistDao.getPlaylistsByOwner(ownerId);
    }

    public Future<List<Playlist>> getPlaylistsByOwnerSync(long ownerId) {
        return executor.submit(() -> playlistDao.getPlaylistsByOwnerSync(ownerId));
    }

    public Future<List<Playlist>> getPublicPlaylistsByOwnerSync(long ownerId) {
        return executor.submit(() -> playlistDao.getPublicPlaylistsByOwnerSync(ownerId));
    }
    
    public LiveData<List<Playlist>> getPublicPlaylists() {
        return playlistDao.getPublicPlaylists();
    }
    
    public LiveData<List<Playlist>> searchPublicPlaylists(String query) {
        return playlistDao.searchPublicPlaylists(query);
    }
    
    public LiveData<List<Playlist>> getPublicPlaylistsByOwner(long ownerId) {
        return playlistDao.getPublicPlaylistsByOwner(ownerId);
    }
    
    public Future<Void> update(Playlist playlist) {
        return executor.submit(() -> {
            playlistDao.update(playlist);
            return null;
        });
    }
    
    public Future<Void> delete(Playlist playlist) {
        return executor.submit(() -> {
            playlistDao.delete(playlist);
            return null;
        });
    }
    
    public Future<Void> deletePlaylistById(long playlistId) {
        return executor.submit(() -> {
            playlistDao.deletePlaylistById(playlistId);
            return null;
        });
    }
    
    // Playlist-Song relationship
    public Future<Void> addSongToPlaylist(long playlistId, long songId) {
        return executor.submit(() -> {
            Integer maxPosition = playlistSongDao.getMaxPositionInPlaylist(playlistId);
            int newPosition = (maxPosition == null) ? 1 : maxPosition + 1;
            PlaylistSong playlistSong = new PlaylistSong(playlistId, songId, newPosition);
            playlistSongDao.insert(playlistSong);
            return null;
        });
    }
    
    public Future<Void> removeSongFromPlaylist(long playlistId, long songId) {
        return executor.submit(() -> {
            playlistSongDao.removeSongFromPlaylist(playlistId, songId);
            return null;
        });
    }
    
    public LiveData<List<Song>> getSongsInPlaylist(long playlistId) {
        return playlistSongDao.getSongsInPlaylist(playlistId);
    }
    
    public Future<List<Song>> getSongsInPlaylistSync(long playlistId) {
        return executor.submit(() -> playlistSongDao.getSongsInPlaylistSync(playlistId));
    }
    
    public LiveData<List<Playlist>> getPlaylistsContainingSong(long songId) {
        return playlistSongDao.getPlaylistsContainingSong(songId);
    }
    
    public Future<Boolean> isSongInPlaylist(long playlistId, long songId) {
        return executor.submit(() -> playlistSongDao.checkSongInPlaylist(playlistId, songId) > 0);
    }
    
    public Future<Integer> getSongCountInPlaylist(long playlistId) {
        return executor.submit(() -> playlistSongDao.getSongCountInPlaylist(playlistId));
    }
    
    public Future<Void> updateSongPosition(long playlistId, long songId, int newPosition) {
        return executor.submit(() -> {
            playlistSongDao.updateSongPosition(playlistId, songId, newPosition);
            return null;
        });
    }
    
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
} 