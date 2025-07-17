package com.g3.soundify_musicplayer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.g3.soundify_musicplayer.data.entity.Comment;

import java.util.List;

@Dao
public interface CommentDao {
    
    @Insert
    long insert(Comment comment);
    
    @Update
    void update(Comment comment);
    
    @Delete
    void delete(Comment comment);
    
    @Query("SELECT * FROM comments WHERE id = :commentId")
    LiveData<Comment> getCommentById(long commentId);
    
    @Query("SELECT * FROM comments WHERE id = :commentId")
    Comment getCommentByIdSync(long commentId);
    
    @Query("SELECT * FROM comments WHERE song_id = :songId ORDER BY created_at DESC")
    LiveData<List<Comment>> getCommentsBySong(long songId);
    
    @Query("SELECT * FROM comments WHERE user_id = :userId ORDER BY created_at DESC")
    LiveData<List<Comment>> getCommentsByUser(long userId);
    
    @Query("SELECT COUNT(*) FROM comments WHERE song_id = :songId")
    int getCommentCountBySong(long songId);
    
    @Query("SELECT COUNT(*) FROM comments WHERE user_id = :userId")
    int getCommentCountByUser(long userId);
    
    @Query("DELETE FROM comments WHERE id = :commentId")
    void deleteCommentById(long commentId);
    
    @Query("DELETE FROM comments WHERE song_id = :songId")
    void deleteCommentsBySong(long songId);
    
    @Query("DELETE FROM comments WHERE user_id = :userId")
    void deleteCommentsByUser(long userId);
} 