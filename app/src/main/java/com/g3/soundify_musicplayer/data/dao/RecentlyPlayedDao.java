package com.g3.soundify_musicplayer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.g3.soundify_musicplayer.data.entity.RecentlyPlayed;
import com.g3.soundify_musicplayer.data.entity.Song;

import java.util.List;
import java.util.concurrent.Future;

@Dao
public interface RecentlyPlayedDao {
    
    /**
     * Get 6 most recent songs for a user
     */
    @Query("SELECT s.* FROM songs s " +
           "INNER JOIN recently_played rp ON s.id = rp.song_id " +
           "WHERE rp.user_id = :userId " +
           "ORDER BY rp.played_at DESC " +
           "LIMIT 6")
    LiveData<List<Song>> getRecentSongs(long userId);

    /**
     * Get 6 most recent songs for a user (sync version)
     */
    @Query("SELECT s.* FROM songs s " +
           "INNER JOIN recently_played rp ON s.id = rp.song_id " +
           "WHERE rp.user_id = :userId " +
           "ORDER BY rp.played_at DESC " +
           "LIMIT 6")
    List<Song> getRecentSongsSync(long userId);

    /**
     * Insert or update recently played record
     * If song already exists for user, update timestamp
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(RecentlyPlayed recentlyPlayed);

    /**
     * Check if song was recently played by user
     */
    @Query("SELECT * FROM recently_played " +
           "WHERE user_id = :userId AND song_id = :songId")
    RecentlyPlayed getRecentlyPlayed(long userId, long songId);

    /**
     * Delete old records to keep only recent ones
     * Keep only 20 most recent per user to avoid database bloat
     */
    @Query("DELETE FROM recently_played " +
           "WHERE user_id = :userId AND id NOT IN (" +
           "  SELECT id FROM recently_played " +
           "  WHERE user_id = :userId " +
           "  ORDER BY played_at DESC " +
           "  LIMIT 20" +
           ")")
    int cleanupOldRecords(long userId);

    /**
     * Get all recently played records for a user (for debugging)
     */
    @Query("SELECT * FROM recently_played " +
           "WHERE user_id = :userId " +
           "ORDER BY played_at DESC")
    LiveData<List<RecentlyPlayed>> getAllRecentlyPlayed(long userId);
}
