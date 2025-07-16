package com.g3.soundify_musicplayer.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "comments",
    foreignKeys = {
        @ForeignKey(
            entity = Song.class,
            parentColumns = "id",
            childColumns = "song_id",
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
        @Index(value = {"song_id"}),
        @Index(value = {"user_id"})
    }
)
public class Comment {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "song_id")
    private long songId;

    @ColumnInfo(name = "user_id")
    private long userId;

    @ColumnInfo(name = "content")
    private String content;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private Long updatedAt;

    // Constructors
    public Comment() {}

    public Comment(long songId, long userId, String content) {
        this.songId = songId;
        this.userId = userId;
        this.content = content;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSongId() {
        return songId;
    }

    public void setSongId(long songId) {
        this.songId = songId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
} 