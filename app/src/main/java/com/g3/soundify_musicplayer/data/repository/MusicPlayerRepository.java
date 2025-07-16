package com.g3.soundify_musicplayer.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.musicplayer_prm.data.database.AppDatabase;
import com.example.musicplayer_prm.data.dao.CommentDao;
import com.example.musicplayer_prm.data.dao.CommentLikeDao;
import com.example.musicplayer_prm.data.dao.SongLikeDao;
import com.example.musicplayer_prm.data.dao.UserFollowDao;
import com.example.musicplayer_prm.data.entity.Comment;
import com.example.musicplayer_prm.data.entity.CommentLike;
import com.example.musicplayer_prm.data.entity.SongLike;
import com.example.musicplayer_prm.data.entity.User;
import com.example.musicplayer_prm.data.entity.UserFollow;
import com.example.musicplayer_prm.data.entity.Song;

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
        return songLikeDao.getLikedSongsByUser(userId);
    }
    
    public LiveData<List<User>> getUsersWhoLikedSong(long songId) {
        return songLikeDao.getUsersWhoLikedSong(songId);
    }
    
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
} 