package com.g3.soundify_musicplayer.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.g3.soundify_musicplayer.data.entity.Comment;
import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.model.NavigationContext;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Repository tổng hợp cho Song Detail Screen
 * Kết hợp các operations từ SongRepository, MusicPlayerRepository, và PlaylistRepository
 * để cung cấp một interface thống nhất cho Song Detail functionality
 */
public class SongDetailRepository {
    
    private SongRepository songRepository;
    private MusicPlayerRepository musicPlayerRepository;
    private PlaylistRepository playlistRepository;
    private ExecutorService executor;
    
    public SongDetailRepository(Application application) {
        songRepository = new SongRepository(application);
        musicPlayerRepository = new MusicPlayerRepository(application);
        playlistRepository = new PlaylistRepository(application);
        executor = Executors.newFixedThreadPool(4);
    }
    
    // ========== SONG OPERATIONS ==========
    
    /**
     * Get song by ID with accessibility check
     */
    public LiveData<Song> getSongById(long songId) {
        return songRepository.getSongById(songId);
    }
    
    /**
     * Check if song is accessible by user
     */
    public Future<Boolean> isSongAccessible(long songId, long userId) {
        return songRepository.isSongAccessible(songId, userId);
    }
    
    /**
     * Get more songs by the same uploader
     */
    public Future<List<Song>> getMoreSongsByUploader(long uploaderId, long excludeSongId, int limit) {
        return songRepository.getMoreSongsByUploader(uploaderId, excludeSongId, limit);
    }
    
    /**
     * Get related songs by genre
     */
    public Future<List<Song>> getRelatedSongsByGenre(String genre, long excludeSongId, int limit) {
        return songRepository.getRelatedSongsByGenre(genre, excludeSongId, limit);
    }

    /**
     * Get song by ID synchronously
     */
    public Future<Song> getSongByIdSync(long songId) {
        return songRepository.getSongByIdSync(songId);
    }
    
    // ========== SONG LIKE OPERATIONS ==========
    
    /**
     * Toggle song like status
     */
    public Future<Boolean> toggleSongLike(long songId, long userId) {
        return musicPlayerRepository.toggleSongLike(songId, userId);
    }
    
    /**
     * Get song like info (status and count)
     */
    public Future<MusicPlayerRepository.SongLikeInfo> getSongLikeInfo(long songId, long userId) {
        return musicPlayerRepository.getSongLikeInfo(songId, userId);
    }
    
    /**
     * Get users who liked the song
     */
    public LiveData<List<User>> getUsersWhoLikedSong(long songId) {
        return musicPlayerRepository.getUsersWhoLikedSong(songId);
    }
    
    // ========== COMMENT OPERATIONS ==========
    
    /**
     * Get comments for song
     */
    public LiveData<List<Comment>> getCommentsBySong(long songId) {
        return musicPlayerRepository.getCommentsBySong(songId);
    }
    
    /**
     * Add new comment
     */
    public Future<Long> addComment(long songId, long userId, String content) {
        return musicPlayerRepository.addComment(songId, userId, content);
    }
    
    /**
     * Update existing comment
     */
    public Future<Void> updateComment(Comment comment) {
        return musicPlayerRepository.updateComment(comment);
    }
    
    /**
     * Delete comment
     */
    public Future<Void> deleteComment(Comment comment) {
        return musicPlayerRepository.deleteComment(comment);
    }
    
    /**
     * Delete comment by ID
     */
    public Future<Void> deleteCommentById(long commentId) {
        return musicPlayerRepository.deleteCommentById(commentId);
    }
    
    /**
     * Get comment count for song
     */
    public Future<Integer> getCommentCountBySong(long songId) {
        return musicPlayerRepository.getCommentCountBySong(songId);
    }
    
    /**
     * Toggle comment like
     */
    public Future<Boolean> toggleCommentLike(long commentId, long userId) {
        return musicPlayerRepository.toggleCommentLike(commentId, userId);
    }
    
    /**
     * Get comment like info
     */
    public Future<MusicPlayerRepository.CommentLikeInfo> getCommentLikeInfo(long commentId, long userId) {
        return musicPlayerRepository.getCommentLikeInfo(commentId, userId);
    }
    
    // ========== PLAYLIST OPERATIONS ==========
    
    /**
     * Get user's playlists for "Add to Playlist" dialog
     */
    public Future<List<Playlist>> getUserPlaylistsForAddSong(long userId) {
        return playlistRepository.getUserPlaylistsForAddSong(userId);
    }
    
    /**
     * Add song to playlist
     */
    public Future<Void> addSongToPlaylist(long playlistId, long songId) {
        return playlistRepository.addSongToPlaylist(playlistId, songId);
    }
    
    /**
     * Add song to multiple playlists
     */
    public Future<Void> addSongToMultiplePlaylists(long songId, List<Long> playlistIds) {
        return playlistRepository.addSongToMultiplePlaylists(songId, playlistIds);
    }
    
    /**
     * Check if song is in playlist
     */
    public Future<Boolean> isSongInPlaylist(long playlistId, long songId) {
        return playlistRepository.isSongInPlaylist(playlistId, songId);
    }
    
    /**
     * Get playlist IDs that contain the song
     */
    public Future<List<Long>> getPlaylistIdsContainingSong(long songId, long userId) {
        return playlistRepository.getPlaylistIdsContainingSong(songId, userId);
    }
    
    /**
     * Create new playlist and add song to it
     */
    public Future<Long> createPlaylistWithSong(String playlistName, String description, boolean isPublic, long ownerId, long songId) {
        return playlistRepository.createPlaylistWithSong(playlistName, description, isPublic, ownerId, songId);
    }
    
    // ========== UTILITY METHODS ==========
    
    /**
     * Get all data needed for song detail screen in one call
     */
    public Future<SongDetailData> getSongDetailData(long songId, long userId) {
        return executor.submit(() -> {
            try {
                // Get song
                Song song = songRepository.getSongByIdSync(songId).get();
                if (song == null) {
                    return null;
                }
                
                // Get like info
                MusicPlayerRepository.SongLikeInfo likeInfo = musicPlayerRepository.getSongLikeInfo(songId, userId).get();
                
                // Get comment count
                int commentCount = musicPlayerRepository.getCommentCountBySong(songId).get();
                
                // Get playlist IDs containing this song
                List<Long> playlistIds = playlistRepository.getPlaylistIdsContainingSong(songId, userId).get();
                
                return new SongDetailData(song, likeInfo.isLiked, likeInfo.likeCount, commentCount, playlistIds);
                
            } catch (Exception e) {
                android.util.Log.e("SongDetailRepository", "Error getting song detail data", e);
                return null;
            }
        });
    }
    
    // Helper class for returning all song detail data
    public static class SongDetailData {
        public final Song song;
        public final boolean isLiked;
        public final int likeCount;
        public final int commentCount;
        public final List<Long> playlistIds;
        
        public SongDetailData(Song song, boolean isLiked, int likeCount, int commentCount, List<Long> playlistIds) {
            this.song = song;
            this.isLiked = isLiked;
            this.likeCount = likeCount;
            this.commentCount = commentCount;
            this.playlistIds = playlistIds;
        }
    }
    
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
        if (songRepository != null) {
            songRepository.shutdown();
        }
        if (musicPlayerRepository != null) {
            musicPlayerRepository.shutdown();
        }
        if (playlistRepository != null) {
            playlistRepository.shutdown();
        }
    }
}
