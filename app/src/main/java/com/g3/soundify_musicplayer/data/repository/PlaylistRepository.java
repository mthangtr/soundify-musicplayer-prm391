package com.g3.soundify_musicplayer.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.g3.soundify_musicplayer.data.database.AppDatabase;
import com.g3.soundify_musicplayer.data.dao.PlaylistDao;
import com.g3.soundify_musicplayer.data.dao.PlaylistSongDao;
import com.g3.soundify_musicplayer.data.dao.PlaylistAccessDao;
import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.PlaylistSong;
import com.g3.soundify_musicplayer.data.entity.PlaylistAccess;
import com.g3.soundify_musicplayer.data.entity.Song;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PlaylistRepository {
    
    private PlaylistDao playlistDao;
    private PlaylistSongDao playlistSongDao;
    private PlaylistAccessDao playlistAccessDao;
    private ExecutorService executor;
    
    public PlaylistRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        playlistDao = database.playlistDao();
        playlistSongDao = database.playlistSongDao();
        playlistAccessDao = database.playlistAccessDao();
        executor = Executors.newFixedThreadPool(4);
    }
    
    // Playlist CRUD
    public Future<Long> insert(Playlist playlist) {
        return executor.submit(() -> playlistDao.insert(playlist));
    }
    
    public LiveData<Playlist> getPlaylistById(long playlistId) {
        return playlistDao.getPlaylistById(playlistId);
    }
    
    public Future<Playlist> getPlaylistByIdSync(long playlistId) {
        return executor.submit(() -> playlistDao.getPlaylistByIdSync(playlistId));
    }
    
    public LiveData<List<Playlist>> getPlaylistsByOwner(long ownerId) {
        return playlistDao.getPlaylistsByOwner(ownerId);
    }

    public Future<List<Playlist>> getPlaylistsByOwnerSync(long ownerId) {
        return executor.submit(() -> playlistDao.getPlaylistsByOwnerSync(ownerId));
    }

    public Future<List<Playlist>> getPublicPlaylistsByOwnerSync(long ownerId) {
        return executor.submit(() -> playlistDao.getPublicPlaylistsByOwnerSync(ownerId));
    }
    
    public LiveData<List<Playlist>> getPublicPlaylists() {
        return playlistDao.getPublicPlaylists();
    }
    
    public LiveData<List<Playlist>> searchPublicPlaylists(String query) {
        return playlistDao.searchPublicPlaylists(query);
    }
    
    public LiveData<List<Playlist>> getPublicPlaylistsByOwner(long ownerId) {
        return playlistDao.getPublicPlaylistsByOwner(ownerId);
    }
    
    public Future<Void> update(Playlist playlist) {
        return executor.submit(() -> {
            playlistDao.update(playlist);
            return null;
        });
    }
    
    public Future<Void> delete(Playlist playlist) {
        return executor.submit(() -> {
            playlistDao.delete(playlist);
            return null;
        });
    }
    
    public Future<Void> deletePlaylistById(long playlistId) {
        return executor.submit(() -> {
            playlistDao.deletePlaylistById(playlistId);
            return null;
        });
    }
    
    // Playlist-Song relationship
    public Future<Void> addSongToPlaylist(long playlistId, long songId) {
        return executor.submit(() -> {
            Integer maxPosition = playlistSongDao.getMaxPositionInPlaylist(playlistId);
            int newPosition = (maxPosition == null) ? 1 : maxPosition + 1;
            PlaylistSong playlistSong = new PlaylistSong(playlistId, songId, newPosition);
            playlistSongDao.insert(playlistSong);
            return null;
        });
    }
    
    public Future<Void> removeSongFromPlaylist(long playlistId, long songId) {
        return executor.submit(() -> {
            playlistSongDao.removeSongFromPlaylist(playlistId, songId);
            return null;
        });
    }
    
    public LiveData<List<Song>> getSongsInPlaylist(long playlistId) {
        return playlistSongDao.getSongsInPlaylist(playlistId);
    }
    
    public Future<List<Song>> getSongsInPlaylistSync(long playlistId) {
        return executor.submit(() -> playlistSongDao.getSongsInPlaylistSync(playlistId));
    }
    
    public LiveData<List<Playlist>> getPlaylistsContainingSong(long songId) {
        return playlistSongDao.getPlaylistsContainingSong(songId);
    }
    
    public Future<Boolean> isSongInPlaylist(long playlistId, long songId) {
        return executor.submit(() -> playlistSongDao.checkSongInPlaylist(playlistId, songId) > 0);
    }
    
    public Future<Integer> getSongCountInPlaylist(long playlistId) {
        return executor.submit(() -> playlistSongDao.getSongCountInPlaylist(playlistId));
    }
    
    public Future<Void> updateSongPosition(long playlistId, long songId, int newPosition) {
        return executor.submit(() -> {
            playlistSongDao.updateSongPosition(playlistId, songId, newPosition);
            return null;
        });
    }

    // Recently Accessed Playlists methods

    /**
     * Get 3 most recently accessed playlists for current user
     */
    public LiveData<List<Playlist>> getRecentlyAccessedPlaylists(long userId) {
        return playlistAccessDao.getRecentlyAccessedPlaylists(userId);
    }

    /**
     * Track that user accessed a playlist
     */
    public void trackPlaylistAccess(long userId, long playlistId) {
        executor.execute(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                PlaylistAccess playlistAccess = new PlaylistAccess(userId, playlistId, currentTime);

                // Insert/update the record
                playlistAccessDao.insert(playlistAccess);

                // Clean up old records to prevent database bloat
                playlistAccessDao.cleanupOldAccessRecords(userId);

            } catch (Exception e) {
                android.util.Log.e("PlaylistRepository", "Error tracking playlist access", e);
            }
        });
    }

    /**
     * Get playlist access records for debugging
     */
    public LiveData<List<PlaylistAccess>> getAllPlaylistAccess(long userId) {
        return playlistAccessDao.getAllPlaylistAccess(userId);
    }

    /**
     * Get all public playlists synchronously for search
     */
    public Future<List<Playlist>> getAllPublicPlaylistsSync() {
        return executor.submit(() -> {
            // Get all playlists and filter public ones
            List<Playlist> allPlaylists = playlistDao.getAllPlaylistsSync();
            List<Playlist> publicPlaylists = new ArrayList<>();
            for (Playlist playlist : allPlaylists) {
                if (playlist.isPublic()) {
                    publicPlaylists.add(playlist);
                }
            }
            return publicPlaylists;
        });
    }

    // Enhanced Playlist Operations for Song Detail Screen

    /**
     * Get user's playlists for "Add to Playlist" dialog
     * Returns only playlists owned by the user
     */
    public Future<List<Playlist>> getUserPlaylistsForAddSong(long userId) {
        return executor.submit(() -> playlistDao.getPlaylistsByOwnerSync(userId));
    }

    /**
     * Add song to multiple playlists at once
     */
    public Future<Void> addSongToMultiplePlaylists(long songId, List<Long> playlistIds) {
        return executor.submit(() -> {
            for (Long playlistId : playlistIds) {
                // Check if song is not already in playlist
                if (playlistSongDao.checkSongInPlaylist(playlistId, songId) == 0) {
                    Integer maxPosition = playlistSongDao.getMaxPositionInPlaylist(playlistId);
                    int newPosition = (maxPosition == null) ? 1 : maxPosition + 1;
                    PlaylistSong playlistSong = new PlaylistSong(playlistId, songId, newPosition);
                    playlistSongDao.insert(playlistSong);
                }
            }
            return null;
        });
    }

    /**
     * Get playlists that contain a specific song
     * Useful for showing which playlists already have the song
     */
    public Future<List<Long>> getPlaylistIdsContainingSong(long songId, long userId) {
        return executor.submit(() -> {
            List<Playlist> userPlaylists = playlistDao.getPlaylistsByOwnerSync(userId);
            List<Long> playlistsWithSong = new java.util.ArrayList<>();

            for (Playlist playlist : userPlaylists) {
                if (playlistSongDao.checkSongInPlaylist(playlist.getId(), songId) > 0) {
                    playlistsWithSong.add(playlist.getId());
                }
            }
            return playlistsWithSong;
        });
    }

    /**
     * Create a new playlist and add song to it
     */
    public Future<Long> createPlaylistWithSong(String playlistName, String description, boolean isPublic, long ownerId, long songId) {
        return executor.submit(() -> {
            // Create playlist
            Playlist playlist = new Playlist(ownerId, playlistName);
            playlist.setDescription(description);
            playlist.setPublic(isPublic);
            long playlistId = playlistDao.insert(playlist);

            // Add song to playlist
            PlaylistSong playlistSong = new PlaylistSong(playlistId, songId, 1);
            playlistSongDao.insert(playlistSong);

            return playlistId;
        });
    }

    /**
     * Get playlist info with song count for display
     */
    public Future<PlaylistInfo> getPlaylistInfo(long playlistId) {
        return executor.submit(() -> {
            Playlist playlist = playlistDao.getPlaylistByIdSync(playlistId);
            if (playlist == null) {
                return null;
            }
            int songCount = playlistSongDao.getSongCountInPlaylist(playlistId);
            return new PlaylistInfo(playlist, songCount);
        });
    }

    // Helper class for returning playlist with metadata
    public static class PlaylistInfo {
        public final Playlist playlist;
        public final int songCount;

        public PlaylistInfo(Playlist playlist, int songCount) {
            this.playlist = playlist;
            this.songCount = songCount;
        }
    }
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}