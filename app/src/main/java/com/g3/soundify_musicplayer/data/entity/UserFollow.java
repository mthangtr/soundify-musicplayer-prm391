package com.g3.soundify_musicplayer.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
    tableName = "user_follows",
    primaryKeys = {"follower_id", "followee_id"},
    foreignKeys = {
        @ForeignKey(
            entity = User.class,
            parentColumns = "id",
            childColumns = "follower_id",
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = User.class,
            parentColumns = "id",
            childColumns = "followee_id",
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(value = {"followee_id"})
    }
)
public class UserFollow {
    @ColumnInfo(name = "follower_id")
    private long followerId;

    @ColumnInfo(name = "followee_id")
    private long followeeId;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    // Constructors
    public UserFollow() {}

    public UserFollow(long followerId, long followeeId) {
        this.followerId = followerId;
        this.followeeId = followeeId;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getFollowerId() {
        return followerId;
    }

    public void setFollowerId(long followerId) {
        this.followerId = followerId;
    }

    public long getFolloweeId() {
        return followeeId;
    }

    public void setFolloweeId(long followeeId) {
        this.followeeId = followeeId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
} 