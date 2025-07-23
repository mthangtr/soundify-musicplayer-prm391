package com.g3.soundify_musicplayer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.SongLike;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.dto.SongWithUploaderInfo;

import java.util.List;

@Dao
public interface SongLikeDao {
    
    @Insert
    void insert(SongLike songLike);
    
    @Delete
    void delete(SongLike songLike);
    
    @Query("SELECT s.* FROM songs s INNER JOIN song_likes sl ON s.id = sl.song_id WHERE sl.user_id = :userId ORDER BY sl.created_at DESC")
    LiveData<List<Song>> getLikedSongsByUser(long userId);

    /**
     * Get liked songs with uploader information for a user
     */
    @Query("SELECT s.id, s.uploader_id, s.title, s.description, s.audio_url, s.cover_art_url, " +
           "s.genre, s.duration_ms, s.is_public, s.created_at, " +
           "u.username as uploaderUsername, u.display_name as uploaderDisplayName, u.avatar_url as uploaderAvatarUrl " +
           "FROM songs s " +
           "INNER JOIN song_likes sl ON s.id = sl.song_id " +
           "INNER JOIN users u ON s.uploader_id = u.id " +
           "WHERE sl.user_id = :userId " +
           "ORDER BY sl.created_at DESC")
    LiveData<List<SongWithUploaderInfo>> getLikedSongsWithUploaderInfoByUser(long userId);
    
    @Query("SELECT u.* FROM users u INNER JOIN song_likes sl ON u.id = sl.user_id WHERE sl.song_id = :songId ORDER BY sl.created_at DESC")
    LiveData<List<User>> getUsersWhoLikedSong(long songId);
    
    @Query("SELECT COUNT(*) FROM song_likes WHERE song_id = :songId")
    int getLikeCountForSong(long songId);
    
    @Query("SELECT COUNT(*) FROM song_likes WHERE song_id = :songId AND user_id = :userId")
    int isSongLikedByUser(long songId, long userId);
    
    @Query("DELETE FROM song_likes WHERE song_id = :songId AND user_id = :userId")
    void unlikeSong(long songId, long userId);
    
    @Query("SELECT * FROM song_likes WHERE song_id = :songId AND user_id = :userId")
    SongLike getSongLike(long songId, long userId);
    
    @Query("DELETE FROM song_likes WHERE song_id = :songId")
    void deleteAllLikesForSong(long songId);
    
    @Query("DELETE FROM song_likes WHERE user_id = :userId")
    void deleteAllLikesByUser(long userId);
} 