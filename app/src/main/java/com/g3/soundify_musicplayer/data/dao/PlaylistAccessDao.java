package com.g3.soundify_musicplayer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.g3.soundify_musicplayer.data.entity.PlaylistAccess;
import com.g3.soundify_musicplayer.data.entity.Playlist;

import java.util.List;

@Dao
public interface PlaylistAccessDao {
    
    /**
     * Get 3 most recently accessed playlists for a user
     */
    @Query("SELECT p.* FROM playlists p " +
           "INNER JOIN playlist_access pa ON p.id = pa.playlist_id " +
           "WHERE pa.user_id = :userId " +
           "ORDER BY pa.accessed_at DESC " +
           "LIMIT 3")
    LiveData<List<Playlist>> getRecentlyAccessedPlaylists(long userId);

    /**
     * Insert or update playlist access record
     * If playlist already accessed by user, update timestamp
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PlaylistAccess playlistAccess);

    /**
     * Check if playlist was accessed by user
     */
    @Query("SELECT * FROM playlist_access " +
           "WHERE user_id = :userId AND playlist_id = :playlistId")
    PlaylistAccess getPlaylistAccess(long userId, long playlistId);

    /**
     * Delete old access records to keep only recent ones
     * Keep only 10 most recent per user to avoid database bloat
     */
    @Query("DELETE FROM playlist_access " +
           "WHERE user_id = :userId AND id NOT IN (" +
           "  SELECT id FROM playlist_access " +
           "  WHERE user_id = :userId " +
           "  ORDER BY accessed_at DESC " +
           "  LIMIT 10" +
           ")")
    int cleanupOldAccessRecords(long userId);

    /**
     * Get all playlist access records for a user (for debugging)
     */
    @Query("SELECT * FROM playlist_access " +
           "WHERE user_id = :userId " +
           "ORDER BY accessed_at DESC")
    LiveData<List<PlaylistAccess>> getAllPlaylistAccess(long userId);
}
