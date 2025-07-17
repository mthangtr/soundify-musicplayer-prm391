package com.g3.soundify_musicplayer.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.g3.soundify_musicplayer.data.database.AppDatabase;
import com.g3.soundify_musicplayer.data.dao.SongDao;
import com.g3.soundify_musicplayer.data.entity.Song;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SongRepository {
    
    private SongDao songDao;
    private ExecutorService executor;
    
    public SongRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        songDao = database.songDao();
        executor = Executors.newFixedThreadPool(4);
    }
    
    // Create
    public Future<Long> insert(Song song) {
        return executor.submit(() -> songDao.insert(song));
    }
    
    // Read
    public LiveData<Song> getSongById(long songId) {
        return songDao.getSongById(songId);
    }
    
    public Future<Song> getSongByIdSync(long songId) {
        return executor.submit(() -> songDao.getSongByIdSync(songId));
    }
    
    public LiveData<List<Song>> getSongsByUploader(long uploaderId) {
        return songDao.getSongsByUploader(uploaderId);
    }
    
    public LiveData<List<Song>> getPublicSongs() {
        return songDao.getPublicSongs();
    }
    
    public LiveData<List<Song>> searchPublicSongs(String query) {
        return songDao.searchPublicSongs(query);
    }
    
    public LiveData<List<Song>> getSongsFromFollowing(long userId) {
        return songDao.getSongsFromFollowing(userId);
    }
    
    public LiveData<List<Song>> getPublicSongsByUploader(long uploaderId) {
        return songDao.getPublicSongsByUploader(uploaderId);
    }
    
    public LiveData<List<Song>> getSongsByGenre(String genre) {
        return songDao.getSongsByGenre(genre);
    }
    
    public LiveData<List<String>> getAllGenres() {
        return songDao.getAllGenres();
    }
    
    // Update
    public Future<Void> update(Song song) {
        return executor.submit(() -> {
            songDao.update(song);
            return null;
        });
    }
    
    // Delete
    public Future<Void> delete(Song song) {
        return executor.submit(() -> {
            songDao.delete(song);
            return null;
        });
    }
    
    public Future<Void> deleteSongById(long songId) {
        return executor.submit(() -> {
            songDao.deleteSongById(songId);
            return null;
        });
    }
    
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
} 