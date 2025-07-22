package com.g3.soundify_musicplayer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.g3.soundify_musicplayer.data.entity.PlaylistSong;
import com.g3.soundify_musicplayer.data.entity.Song;

import java.util.List;

@Dao
public interface PlaylistSongDao {
    
    @Insert
    void insert(PlaylistSong playlistSong);
    
    @Update
    void update(PlaylistSong playlistSong);
    
    @Delete
    void delete(PlaylistSong playlistSong);
    
    @Query("SELECT s.*, u.username as uploaderName FROM songs s " +
           "INNER JOIN playlist_songs ps ON s.id = ps.song_id " +
           "LEFT JOIN users u ON s.uploader_id = u.id " +
           "WHERE ps.playlist_id = :playlistId " +
           "ORDER BY ps.position ASC, ps.rowid ASC")
    LiveData<List<Song>> getSongsInPlaylist(long playlistId);

    @Query("SELECT s.*, u.username as uploaderName FROM songs s " +
           "INNER JOIN playlist_songs ps ON s.id = ps.song_id " +
           "LEFT JOIN users u ON s.uploader_id = u.id " +
           "WHERE ps.playlist_id = :playlistId " +
           "ORDER BY ps.position ASC, ps.rowid ASC")
    List<Song> getSongsInPlaylistSync(long playlistId);
    
    @Query("SELECT p.* FROM playlists p INNER JOIN playlist_songs ps ON p.id = ps.playlist_id WHERE ps.song_id = :songId")
    LiveData<List<com.g3.soundify_musicplayer.data.entity.Playlist>> getPlaylistsContainingSong(long songId);
    
    @Query("DELETE FROM playlist_songs WHERE playlist_id = :playlistId AND song_id = :songId")
    void removeSongFromPlaylist(long playlistId, long songId);
    
    @Query("DELETE FROM playlist_songs WHERE playlist_id = :playlistId")
    void removeAllSongsFromPlaylist(long playlistId);
    
    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlist_id = :playlistId")
    int getSongCountInPlaylist(long playlistId);
    
    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlist_id = :playlistId AND song_id = :songId")
    int checkSongInPlaylist(long playlistId, long songId);
    
    @Query("SELECT MAX(position) FROM playlist_songs WHERE playlist_id = :playlistId")
    Integer getMaxPositionInPlaylist(long playlistId);
    
    @Query("UPDATE playlist_songs SET position = :newPosition WHERE playlist_id = :playlistId AND song_id = :songId")
    void updateSongPosition(long playlistId, long songId, int newPosition);
} 