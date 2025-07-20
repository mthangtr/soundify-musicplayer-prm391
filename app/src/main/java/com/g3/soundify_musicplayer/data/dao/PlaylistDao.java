package com.g3.soundify_musicplayer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.g3.soundify_musicplayer.data.entity.Playlist;

import java.util.List;
import java.util.concurrent.Future;

@Dao
public interface PlaylistDao {
    
    @Insert
    long insert(Playlist playlist);
    
    @Update
    void update(Playlist playlist);
    
    @Delete
    void delete(Playlist playlist);
    
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    LiveData<Playlist> getPlaylistById(long playlistId);
    
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    Playlist getPlaylistByIdSync(long playlistId);
    
    @Query("SELECT * FROM playlists WHERE owner_id = :ownerId ORDER BY created_at DESC")
    LiveData<List<Playlist>> getPlaylistsByOwner(long ownerId);

    @Query("SELECT * FROM playlists WHERE owner_id = :ownerId ORDER BY created_at DESC")
    List<Playlist> getPlaylistsByOwnerSync(long ownerId);

    @Query("SELECT * FROM playlists WHERE owner_id = :ownerId AND is_public = 1 ORDER BY created_at DESC")
    List<Playlist> getPublicPlaylistsByOwnerSync(long ownerId);
    
    @Query("SELECT * FROM playlists WHERE is_public = 1 ORDER BY created_at DESC")
    LiveData<List<Playlist>> getPublicPlaylists();
    
    @Query("SELECT * FROM playlists WHERE is_public = 1 AND name LIKE '%' || :query || '%' ORDER BY created_at DESC")
    LiveData<List<Playlist>> searchPublicPlaylists(String query);
    
    @Query("SELECT * FROM playlists WHERE owner_id = :ownerId AND is_public = 1 ORDER BY created_at DESC")
    LiveData<List<Playlist>> getPublicPlaylistsByOwner(long ownerId);
    
    @Query("DELETE FROM playlists WHERE id = :playlistId")
    void deletePlaylistById(long playlistId);
    
    @Query("SELECT COUNT(*) FROM playlists WHERE owner_id = :ownerId")
    int getPlaylistCountByOwner(long ownerId);

    /**
     * Get all playlists (sync version for checking if playlists exist)
     */
    @Query("SELECT * FROM playlists")
    List<Playlist> getAllPlaylistsSync();
}