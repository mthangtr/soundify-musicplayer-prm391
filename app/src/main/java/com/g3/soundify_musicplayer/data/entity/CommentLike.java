package com.g3.soundify_musicplayer.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
    tableName = "comment_likes",
    primaryKeys = {"comment_id", "user_id"},
    foreignKeys = {
        @ForeignKey(
            entity = Comment.class,
            parentColumns = "id",
            childColumns = "comment_id",
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = User.class,
            parentColumns = "id",
            childColumns = "user_id",
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(value = {"user_id"})
    }
)
public class CommentLike {
    @ColumnInfo(name = "comment_id")
    private long commentId;

    @ColumnInfo(name = "user_id")
    private long userId;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    // Constructors
    public CommentLike() {}

    public CommentLike(long commentId, long userId) {
        this.commentId = commentId;
        this.userId = userId;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getCommentId() {
        return commentId;
    }

    public void setCommentId(long commentId) {
        this.commentId = commentId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
} 