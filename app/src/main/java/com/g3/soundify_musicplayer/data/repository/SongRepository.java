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
                long result = recentlyPlayedDao.insert(recentlyPlayed);

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

    /**
     * Get all songs synchronously for search
     */
    public Future<List<Song>> getAllSongsSync() {
        return executor.submit(() -> songDao.getAllSongsSync());
    }

    // ========== METHODS WITH UPLOADER INFORMATION ==========

    /**
     * Get random suggested songs with uploader information (max 10, but adapt to available songs)
     */
    public LiveData<List<SongWithUploaderInfo>> getSuggestedSongsWithUploaderInfo() {
        // Get up to 10 random songs, but will return whatever is available
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

    /**
     * Get public songs by uploader with uploader information (sync)
     */
    public Future<List<SongWithUploaderInfo>> getPublicSongsByUploaderWithInfoSync(long uploaderId) {
        return executor.submit(() -> songDao.getPublicSongsByUploaderWithInfoSync(uploaderId));
    }

    /**
     * Get all songs by uploader with uploader information (sync) - for own profile
     */
    public Future<List<SongWithUploaderInfo>> getSongsByUploaderWithInfoSync(long uploaderId) {
        return executor.submit(() -> songDao.getSongsByUploaderWithInfoSync(uploaderId));
    }

    /**
     * Get song with uploader information by song ID (sync)
     */
    public Future<SongWithUploaderInfo> getSongWithUploaderInfoSync(long songId) {
        return executor.submit(() -> songDao.getSongWithUploaderInfoSync(songId));
    }

    // Song Detail specific methods

    /**
     * Get song with additional metadata for song detail screen
     * This method can be extended to include like count, comment count, etc.
     */
    public Future<Song> getSongWithMetadata(long songId) {
        return executor.submit(() -> {
            // For now, just return the song. Can be extended to include metadata
            return songDao.getSongByIdSync(songId);
        });
    }

    /**
     * Check if song exists and is accessible by user
     */
    public Future<Boolean> isSongAccessible(long songId, long userId) {
        return executor.submit(() -> {
            Song song = songDao.getSongByIdSync(songId);
            if (song == null) {
                return false;
            }
            // Song is accessible if it's public or user is the uploader
            return song.isPublic() || song.getUploaderId() == userId;
        });
    }

    /**
     * Check if user is the owner of the song
     */
    public Future<Boolean> isSongOwner(long songId, long userId) {
        return executor.submit(() -> {
            Song song = songDao.getSongByIdSync(songId);
            return song != null && song.getUploaderId() == userId;
        });
    }

    /**
     * Update song information (only for song owner)
     */
    public Future<Boolean> updateSongInfo(long songId, long userId, String title, String description, String genre, boolean isPublic, String coverArtUrl) {
        return executor.submit(() -> {
            try {
                // Check ownership first
                Song song = songDao.getSongByIdSync(songId);
                if (song == null) {
                    android.util.Log.e("SongRepository", "Song not found: " + songId);
                    return false;
                }

                if (song.getUploaderId() != userId) {
                    android.util.Log.e("SongRepository", "User " + userId + " is not owner of song " + songId);
                    return false;
                }

                // Update song fields
                song.setTitle(title);
                song.setDescription(description);
                song.setGenre(genre);
                song.setPublic(isPublic);
                if (coverArtUrl != null) {
                    song.setCoverArtUrl(coverArtUrl);
                }

                // Save to database
                songDao.update(song);
                android.util.Log.d("SongRepository", "Successfully updated song: " + songId);
                return true;

            } catch (Exception e) {
                android.util.Log.e("SongRepository", "Error updating song: " + songId, e);
                return false;
            }
        });
    }

    /**
     * Delete song (only for song owner) - cascades to all related data
     */
    public Future<Boolean> deleteSongByOwner(long songId, long userId) {
        return executor.submit(() -> {
            try {
                // Check ownership first
                Song song = songDao.getSongByIdSync(songId);
                if (song == null) {
                    android.util.Log.e("SongRepository", "Song not found: " + songId);
                    return false;
                }

                if (song.getUploaderId() != userId) {
                    android.util.Log.e("SongRepository", "User " + userId + " is not owner of song " + songId);
                    return false;
                }

                // Delete song - Room will handle cascade operations for:
                // - Comments (via foreign key cascade)
                // - Song likes (via foreign key cascade)
                // - Playlist songs (via foreign key cascade)
                // - Recently played (via foreign key cascade)
                songDao.delete(song);
                android.util.Log.d("SongRepository", "Successfully deleted song: " + songId);
                return true;

            } catch (Exception e) {
                android.util.Log.e("SongRepository", "Error deleting song: " + songId, e);
                return false;
            }
        });
    }

    /**
     * Get songs by the same uploader (for "More from this artist" section)
     */
    public Future<List<Song>> getMoreSongsByUploader(long uploaderId, long excludeSongId, int limit) {
        return executor.submit(() -> {
            List<Song> allSongs = songDao.getPublicSongsByUploaderSync(uploaderId);
            return allSongs.stream()
                    .filter(song -> song.getId() != excludeSongId)
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toList());
        });
    }

    /**
     * Get related songs by genre (for "You might also like" section)
     */
    public Future<List<Song>> getRelatedSongsByGenre(String genre, long excludeSongId, int limit) {
        return executor.submit(() -> {
            List<Song> allSongs = songDao.getSongsByGenreSync(genre);
            return allSongs.stream()
                    .filter(song -> song.getId() != excludeSongId && song.isPublic())
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toList());
        });
    }

    // ========== DIRECT DATABASE ACCESS (TO AVOID DEADLOCK) ==========

    /**
     * Get song by ID directly from database (no ExecutorService)
     */
    public Song getSongByIdDirectly(long songId) {
        return songDao.getSongByIdSync(songId);
    }

    /**
     * Get public songs by uploader directly from database (no ExecutorService)
     */
    public List<Song> getPublicSongsByUploaderDirectly(long uploaderId) {
        return songDao.getPublicSongsByUploaderSync(uploaderId);
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}