package com.g3.soundify_musicplayer.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.g3.soundify_musicplayer.data.database.AppDatabase;
import com.g3.soundify_musicplayer.data.dao.SongDao;
import com.g3.soundify_musicplayer.data.dao.RecentlyPlayedDao;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.RecentlyPlayed;
import com.g3.soundify_musicplayer.data.dto.SongWithUploader;
import com.g3.soundify_musicplayer.data.dto.SongWithUploaderInfo;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SongRepository {

    private SongDao songDao;
    private RecentlyPlayedDao recentlyPlayedDao;
    private ExecutorService executor;
    
    public SongRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        songDao = database.songDao();
        recentlyPlayedDao = database.recentlyPlayedDao();
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

    public LiveData<List<Song>> getAllSongs() {
        return songDao.getAllSongs();
    }

    public LiveData<List<Song>> getSongsByUploader(long uploaderId) {
        return songDao.getSongsByUploader(uploaderId);
    }
    
    public LiveData<List<Song>> getPublicSongs() {
        return songDao.getPublicSongs();
    }

    public Future<List<Song>> getSongsByUploaderSync(long uploaderId) {
        return executor.submit(() -> songDao.getSongsByUploaderSync(uploaderId));
    }

    public Future<List<Song>> getPublicSongsByUploaderSync(long uploaderId) {
        return executor.submit(() -> songDao.getPublicSongsByUploaderSync(uploaderId));
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
    
    // Recently Played methods

    /**
     * Get 6 most recent songs for current user
     */
    public LiveData<List<Song>> getRecentSongs(long userId) {
        return recentlyPlayedDao.getRecentSongs(userId);
    }

    /**
     * Track that user played a song
     */
    public void trackRecentlyPlayed(long userId, long songId) {
        executor.execute(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                RecentlyPlayed recentlyPlayed = new RecentlyPlayed(userId, songId, currentTime);

                // Insert/update the record
                recentlyPlayedDao.insert(recentlyPlayed);

                // Clean up old records to prevent database bloat
                recentlyPlayedDao.cleanupOldRecords(userId);

            } catch (Exception e) {
                android.util.Log.e("SongRepository", "Error tracking recently played", e);
            }
        });
    }

    /**
     * Get recently played records for debugging
     */
    public LiveData<List<RecentlyPlayed>> getAllRecentlyPlayed(long userId) {
        return recentlyPlayedDao.getAllRecentlyPlayed(userId);
    }

    /**
     * Get 10 random suggested songs
     */
    public LiveData<List<Song>> getSuggestedSongs() {
        return songDao.getRandomSongs(10);
    }

    // ========== METHODS WITH UPLOADER INFORMATION ==========

    /**
     * Get 10 random suggested songs with uploader information
     */
    public LiveData<List<SongWithUploaderInfo>> getSuggestedSongsWithUploaderInfo() {
        return songDao.getRandomSongsWithUploaderInfo(10);
    }

    /**
     * Get 6 most recent songs with uploader information for current user
     */
    public LiveData<List<SongWithUploaderInfo>> getRecentSongsWithUploaderInfo(long userId) {
        return recentlyPlayedDao.getRecentSongsWithUploaderInfo(userId);
    }

    /**
     * Get all public songs with uploader information
     */
    public LiveData<List<SongWithUploaderInfo>> getPublicSongsWithUploaderInfo() {
        return songDao.getPublicSongsWithUploaderInfo();
    }

    /**
     * Search public songs with uploader information
     */
    public LiveData<List<SongWithUploaderInfo>> searchPublicSongsWithUploaderInfo(String query) {
        return songDao.searchPublicSongsWithUploaderInfo(query);
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}