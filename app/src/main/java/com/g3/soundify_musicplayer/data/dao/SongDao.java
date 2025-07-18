package com.g3.soundify_musicplayer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.g3.soundify_musicplayer.data.entity.Song;

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
} 