package com.g3.soundify_musicplayer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.musicplayer_prm.data.entity.CommentLike;
import com.example.musicplayer_prm.data.entity.User;

import java.util.List;

@Dao
public interface CommentLikeDao {
    
    @Insert
    void insert(CommentLike commentLike);
    
    @Delete
    void delete(CommentLike commentLike);
    
    @Query("SELECT u.* FROM users u INNER JOIN comment_likes cl ON u.id = cl.user_id WHERE cl.comment_id = :commentId ORDER BY cl.created_at DESC")
    LiveData<List<User>> getUsersWhoLikedComment(long commentId);
    
    @Query("SELECT COUNT(*) FROM comment_likes WHERE comment_id = :commentId")
    int getLikeCountForComment(long commentId);
    
    @Query("SELECT COUNT(*) FROM comment_likes WHERE comment_id = :commentId AND user_id = :userId")
    int isCommentLikedByUser(long commentId, long userId);
    
    @Query("DELETE FROM comment_likes WHERE comment_id = :commentId AND user_id = :userId")
    void unlikeComment(long commentId, long userId);
    
    @Query("SELECT * FROM comment_likes WHERE comment_id = :commentId AND user_id = :userId")
    CommentLike getCommentLike(long commentId, long userId);
    
    @Query("DELETE FROM comment_likes WHERE comment_id = :commentId")
    void deleteAllLikesForComment(long commentId);
    
    @Query("DELETE FROM comment_likes WHERE user_id = :userId")
    void deleteAllLikesByUser(long userId);
} 