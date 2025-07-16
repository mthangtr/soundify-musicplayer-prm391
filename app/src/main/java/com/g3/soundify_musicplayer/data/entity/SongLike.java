package com.g3.soundify_musicplayer.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
    tableName = "song_likes",
    primaryKeys = {"song_id", "user_id"},
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
        @Index(value = {"user_id"})
    }
)
public class SongLike {
    @ColumnInfo(name = "song_id")
    private long songId;

    @ColumnInfo(name = "user_id")
    private long userId;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    // Constructors
    public SongLike() {}

    public SongLike(long songId, long userId) {
        this.songId = songId;
        this.userId = userId;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
} 