package com.g3.soundify_musicplayer.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "playlists",
    foreignKeys = @ForeignKey(
        entity = User.class,
        parentColumns = "id",
        childColumns = "owner_id",
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = {"owner_id"}),
        @Index(value = {"is_public"})
    }
)
public class Playlist {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "owner_id")
    private long ownerId;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "is_public")
    private boolean isPublic;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    // Constructors
    public Playlist() {}

    public Playlist(long ownerId, String name) {
        this.ownerId = ownerId;
        this.name = name;
        this.isPublic = true;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
} 