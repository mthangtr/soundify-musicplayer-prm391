package com.g3.soundify_musicplayer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.dto.SongWithUploader;
import com.g3.soundify_musicplayer.data.dto.SongWithUploaderInfo;

import java.util.List;
import java.util.concurrent.Future;

@Dao
public interface SongDao {
    
    @Insert
    long insert(Song song);
    
    @Update
    void update(Song song);
    
    @Delete
    void delete(Song song);
    
    @Query("SELECT * FROM songs WHERE id = :songId")
    LiveData<Song> getSongById(long songId);
    
    @Query("SELECT * FROM songs WHERE id = :songId")
    Song getSongByIdSync(long songId);
    
    @Query("SELECT * FROM songs ORDER BY created_at DESC")
    LiveData<List<Song>> getAllSongs();

    @Query("SELECT * FROM songs WHERE uploader_id = :uploaderId ORDER BY created_at DESC")
    LiveData<List<Song>> getSongsByUploader(long uploaderId);

    @Query("SELECT * FROM songs WHERE is_public = 1 ORDER BY created_at DESC")
    LiveData<List<Song>> getPublicSongs();
    @Query("SELECT * FROM songs ORDER BY RANDOM() LIMIT :limit")
    LiveData<List<Song>> getRandomSongs(int limit);

    @Query("SELECT * FROM songs WHERE uploader_id = :uploaderId ORDER BY created_at DESC")
    List<Song> getSongsByUploaderSync(long uploaderId);

    @Query("SELECT * FROM songs WHERE uploader_id = :uploaderId AND is_public = 1 ORDER BY created_at DESC")
    List<Song> getPublicSongsByUploaderSync(long uploaderId);
    
    @Query("SELECT * FROM songs WHERE is_public = 1 AND (title LIKE '%' || :query || '%' OR genre LIKE '%' || :query || '%') ORDER BY created_at DESC")
    LiveData<List<Song>> searchPublicSongs(String query);
    
    @Query("SELECT s.* FROM songs s INNER JOIN user_follows uf ON s.uploader_id = uf.followee_id WHERE uf.follower_id = :userId AND s.is_public = 1 ORDER BY s.created_at DESC")
    LiveData<List<Song>> getSongsFromFollowing(long userId);
    
    @Query("SELECT * FROM songs WHERE uploader_id = :uploaderId AND is_public = 1 ORDER BY created_at DESC")
    LiveData<List<Song>> getPublicSongsByUploader(long uploaderId);
    
    @Query("SELECT * FROM songs WHERE genre = :genre AND is_public = 1 ORDER BY created_at DESC")
    LiveData<List<Song>> getSongsByGenre(String genre);
    
    @Query("SELECT DISTINCT genre FROM songs WHERE genre IS NOT NULL AND genre != '' ORDER BY genre")
    LiveData<List<String>> getAllGenres();
    
    @Query("DELETE FROM songs WHERE id = :songId")
    void deleteSongById(long songId);

    /**
     * Get all songs (sync version for checking if songs exist)
     */
    @Query("SELECT * FROM songs")
    List<Song> getAllSongsSync();


    // ========== JOIN QUERIES FOR SONG WITH UPLOADER ==========

    /**
     * Get random songs with uploader information for suggested songs
     */
    @Query("SELECT s.id, s.uploader_id, s.title, s.description, s.audio_url, s.cover_art_url, " +
           "s.genre, s.duration_ms, s.is_public, s.created_at, " +
           "u.username as uploaderUsername, u.display_name as uploaderDisplayName, u.avatar_url as uploaderAvatarUrl " +
           "FROM songs s " +
           "INNER JOIN users u ON s.uploader_id = u.id " +
           "WHERE s.is_public = 1 " +
           "ORDER BY RANDOM() LIMIT :limit")
    LiveData<List<SongWithUploaderInfo>> getRandomSongsWithUploaderInfo(int limit);

    /**
     * Get all public songs with uploader information
     */
    @Query("SELECT s.id, s.uploader_id, s.title, s.description, s.audio_url, s.cover_art_url, " +
           "s.genre, s.duration_ms, s.is_public, s.created_at, " +
           "u.username as uploaderUsername, u.display_name as uploaderDisplayName, u.avatar_url as uploaderAvatarUrl " +
           "FROM songs s " +
           "INNER JOIN users u ON s.uploader_id = u.id " +
           "WHERE s.is_public = 1 " +
           "ORDER BY s.created_at DESC")
    LiveData<List<SongWithUploaderInfo>> getPublicSongsWithUploaderInfo();

    /**
     * Search public songs with uploader information
     */
    @Query("SELECT s.id, s.uploader_id, s.title, s.description, s.audio_url, s.cover_art_url, " +
           "s.genre, s.duration_ms, s.is_public, s.created_at, " +
           "u.username as uploaderUsername, u.display_name as uploaderDisplayName, u.avatar_url as uploaderAvatarUrl " +
           "FROM songs s " +
           "INNER JOIN users u ON s.uploader_id = u.id " +
           "WHERE s.is_public = 1 AND (s.title LIKE '%' || :query || '%' OR s.genre LIKE '%' || :query || '%' OR u.display_name LIKE '%' || :query || '%') " +
           "ORDER BY s.created_at DESC")
    LiveData<List<SongWithUploaderInfo>> searchPublicSongsWithUploaderInfo(String query);

    /**
     * Get public songs by uploader with uploader information (sync)
     */
    @Query("SELECT s.id, s.uploader_id, s.title, s.description, s.audio_url, s.cover_art_url, " +
           "s.genre, s.duration_ms, s.is_public, s.created_at, " +
           "u.username as uploaderUsername, u.display_name as uploaderDisplayName, u.avatar_url as uploaderAvatarUrl " +
           "FROM songs s " +
           "INNER JOIN users u ON s.uploader_id = u.id " +
           "WHERE s.uploader_id = :uploaderId AND s.is_public = 1 " +
           "ORDER BY s.created_at DESC")
    List<SongWithUploaderInfo> getPublicSongsByUploaderWithInfoSync(long uploaderId);

    /**
     * Get all songs by uploader with uploader information (sync) - for own profile
     */
    @Query("SELECT s.id, s.uploader_id, s.title, s.description, s.audio_url, s.cover_art_url, " +
           "s.genre, s.duration_ms, s.is_public, s.created_at, " +
           "u.username as uploaderUsername, u.display_name as uploaderDisplayName, u.avatar_url as uploaderAvatarUrl " +
           "FROM songs s " +
           "INNER JOIN users u ON s.uploader_id = u.id " +
           "WHERE s.uploader_id = :uploaderId " +
           "ORDER BY s.created_at DESC")
    List<SongWithUploaderInfo> getSongsByUploaderWithInfoSync(long uploaderId);

    /**
     * Get songs by genre (sync version for related songs)
     */
    @Query("SELECT * FROM songs WHERE genre = :genre AND is_public = 1 ORDER BY created_at DESC")
    List<Song> getSongsByGenreSync(String genre);

}