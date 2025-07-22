package com.g3.soundify_musicplayer.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.g3.soundify_musicplayer.data.database.AppDatabase;
import com.g3.soundify_musicplayer.data.dao.CommentDao;
import com.g3.soundify_musicplayer.data.dao.CommentLikeDao;
import com.g3.soundify_musicplayer.data.dao.SongLikeDao;
import com.g3.soundify_musicplayer.data.dao.UserFollowDao;
import com.g3.soundify_musicplayer.data.entity.Comment;
import com.g3.soundify_musicplayer.data.entity.CommentLike;
import com.g3.soundify_musicplayer.data.entity.SongLike;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.entity.UserFollow;
import com.g3.soundify_musicplayer.data.entity.Song;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MusicPlayerRepository {
    
    private UserFollowDao userFollowDao;
    private CommentDao commentDao;
    private CommentLikeDao commentLikeDao;
    private SongLikeDao songLikeDao;
    private ExecutorService executor;
    
    public MusicPlayerRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        userFollowDao = database.userFollowDao();
        commentDao = database.commentDao();
        commentLikeDao = database.commentLikeDao();
        songLikeDao = database.songLikeDao();
        executor = Executors.newFixedThreadPool(4);
    }
    
    // User Follow Operations
    public Future<Void> followUser(long followerId, long followeeId) {
        return executor.submit(() -> {
            if (followerId != followeeId) { // Prevent self-follow
                UserFollow userFollow = new UserFollow(followerId, followeeId);
                userFollowDao.insert(userFollow);
            }
            return null;
        });
    }
    
    public Future<Void> unfollowUser(long followerId, long followeeId) {
        return executor.submit(() -> {
            userFollowDao.unfollow(followerId, followeeId);
            return null;
        });
    }
    
    public LiveData<List<User>> getFollowing(long userId) {
        return userFollowDao.getFollowing(userId);
    }
    
    public LiveData<List<User>> getFollowers(long userId) {
        return userFollowDao.getFollowers(userId);
    }
    
    public Future<Boolean> isFollowing(long followerId, long followeeId) {
        return executor.submit(() -> userFollowDao.isFollowing(followerId, followeeId) > 0);
    }
    
    public Future<Integer> getFollowingCount(long userId) {
        return executor.submit(() -> userFollowDao.getFollowingCount(userId));
    }
    
    public Future<Integer> getFollowersCount(long userId) {
        return executor.submit(() -> userFollowDao.getFollowersCount(userId));
    }
    
    // Comment Operations
    public Future<Long> addComment(long songId, long userId, String content) {
        return executor.submit(() -> {
            Comment comment = new Comment(songId, userId, content);
            return commentDao.insert(comment);
        });
    }
    
    public Future<Void> updateComment(Comment comment) {
        return executor.submit(() -> {
            commentDao.update(comment);
            return null;
        });
    }
    
    public Future<Void> deleteComment(Comment comment) {
        return executor.submit(() -> {
            commentDao.delete(comment);
            return null;
        });
    }
    
    public LiveData<List<Comment>> getCommentsBySong(long songId) {
        return commentDao.getCommentsBySong(songId);
    }
    
    public LiveData<List<Comment>> getCommentsByUser(long userId) {
        return commentDao.getCommentsByUser(userId);
    }
    
    public Future<Integer> getCommentCountBySong(long songId) {
        return executor.submit(() -> commentDao.getCommentCountBySong(songId));
    }
    
    // Comment Like Operations
    public Future<Void> likeComment(long commentId, long userId) {
        return executor.submit(() -> {
            CommentLike commentLike = new CommentLike(commentId, userId);
            commentLikeDao.insert(commentLike);
            return null;
        });
    }
    
    public Future<Void> unlikeComment(long commentId, long userId) {
        return executor.submit(() -> {
            commentLikeDao.unlikeComment(commentId, userId);
            return null;
        });
    }
    
    public Future<Boolean> isCommentLikedByUser(long commentId, long userId) {
        return executor.submit(() -> commentLikeDao.isCommentLikedByUser(commentId, userId) > 0);
    }
    
    public Future<Integer> getLikeCountForComment(long commentId) {
        return executor.submit(() -> commentLikeDao.getLikeCountForComment(commentId));
    }
    
    public LiveData<List<User>> getUsersWhoLikedComment(long commentId) {
        return commentLikeDao.getUsersWhoLikedComment(commentId);
    }
    
    // Song Like Operations
    public Future<Void> likeSong(long songId, long userId) {
        return executor.submit(() -> {
            SongLike songLike = new SongLike(songId, userId);
            songLikeDao.insert(songLike);
            return null;
        });
    }
    
    public Future<Void> unlikeSong(long songId, long userId) {
        return executor.submit(() -> {
            songLikeDao.unlikeSong(songId, userId);
            return null;
        });
    }
    
    public Future<Boolean> isSongLikedByUser(long songId, long userId) {
        return executor.submit(() -> songLikeDao.isSongLikedByUser(songId, userId) > 0);
    }
    
    public Future<Integer> getLikeCountForSong(long songId) {
        return executor.submit(() -> songLikeDao.getLikeCountForSong(songId));
    }
    
    public LiveData<List<Song>> getLikedSongsByUser(long userId) {
        android.util.Log.d("MusicPlayerRepository", "🔍 getLikedSongsByUser called for userId: " + userId);
        LiveData<List<Song>> result = songLikeDao.getLikedSongsByUser(userId);
        android.util.Log.d("MusicPlayerRepository", "🔍 LiveData result: " + (result != null ? "SUCCESS" : "NULL"));
        return result;
    }
    
    public LiveData<List<User>> getUsersWhoLikedSong(long songId) {
        return songLikeDao.getUsersWhoLikedSong(songId);
    }

    // Enhanced Song Like Operations for Song Detail Screen

    /**
     * Toggle song like status (like if not liked, unlike if liked)
     * Returns the new like status
     */
    public Future<Boolean> toggleSongLike(long songId, long userId) {
        android.util.Log.d("MusicPlayerRepository", "🔄 toggleSongLike called - songId: " + songId + ", userId: " + userId);
        
        return executor.submit(() -> {
            try {
                boolean isCurrentlyLiked = songLikeDao.isSongLikedByUser(songId, userId) > 0;
                android.util.Log.d("MusicPlayerRepository", "🔍 Current like status: " + isCurrentlyLiked);
                
                if (isCurrentlyLiked) {
                    android.util.Log.d("MusicPlayerRepository", "🔄 Unliking song...");
                    songLikeDao.unlikeSong(songId, userId);
                    android.util.Log.d("MusicPlayerRepository", "✅ Song unliked successfully");
                    return false;
                } else {
                    android.util.Log.d("MusicPlayerRepository", "🔄 Liking song...");
                    SongLike songLike = new SongLike(songId, userId);
                    songLikeDao.insert(songLike);
                    android.util.Log.d("MusicPlayerRepository", "✅ Song liked successfully");
                    return true;
                }
            } catch (Exception e) {
                android.util.Log.e("MusicPlayerRepository", "❌ Error in toggleSongLike", e);
                throw e;
            }
        });
    }

    /**
     * Get song like status and count in one call for efficiency
     */
    public Future<SongLikeInfo> getSongLikeInfo(long songId, long userId) {
        return executor.submit(() -> {
            boolean isLiked = songLikeDao.isSongLikedByUser(songId, userId) > 0;
            int likeCount = songLikeDao.getLikeCountForSong(songId);
            return new SongLikeInfo(isLiked, likeCount);
        });
    }

    // Enhanced Comment Operations for Song Detail Screen

    /**
     * Get comment with like info for display
     */
    public Future<CommentLikeInfo> getCommentLikeInfo(long commentId, long userId) {
        return executor.submit(() -> {
            boolean isLiked = commentLikeDao.isCommentLikedByUser(commentId, userId) > 0;
            int likeCount = commentLikeDao.getLikeCountForComment(commentId);
            return new CommentLikeInfo(isLiked, likeCount);
        });
    }

    /**
     * Toggle comment like status
     */
    public Future<Boolean> toggleCommentLike(long commentId, long userId) {
        return executor.submit(() -> {
            boolean isCurrentlyLiked = commentLikeDao.isCommentLikedByUser(commentId, userId) > 0;
            if (isCurrentlyLiked) {
                commentLikeDao.unlikeComment(commentId, userId);
                return false;
            } else {
                CommentLike commentLike = new CommentLike(commentId, userId);
                commentLikeDao.insert(commentLike);
                return true;
            }
        });
    }

    /**
     * Delete comment by ID (convenience method)
     */
    public Future<Void> deleteCommentById(long commentId) {
        return executor.submit(() -> {
            commentDao.deleteCommentById(commentId);
            return null;
        });
    }

    // Helper classes for returning multiple values
    public static class SongLikeInfo {
        public final boolean isLiked;
        public final int likeCount;

        public SongLikeInfo(boolean isLiked, int likeCount) {
            this.isLiked = isLiked;
            this.likeCount = likeCount;
        }
    }

    public static class CommentLikeInfo {
        public final boolean isLiked;
        public final int likeCount;

        public CommentLikeInfo(boolean isLiked, int likeCount) {
            this.isLiked = isLiked;
            this.likeCount = likeCount;
        }
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}