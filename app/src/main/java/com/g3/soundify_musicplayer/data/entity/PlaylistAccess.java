package com.g3.soundify_musicplayer.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "playlist_access",
    foreignKeys = {
        @ForeignKey(
            entity = User.class,
            parentColumns = "id",
            childColumns = "user_id",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = Playlist.class,
            parentColumns = "id", 
            childColumns = "playlist_id",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(value = {"user_id", "playlist_id"}, unique = true),
        @Index("accessed_at"),
        @Index("playlist_id")
    }
)
public class PlaylistAccess {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "user_id")
    private long userId;

    @ColumnInfo(name = "playlist_id") 
    private long playlistId;

    @ColumnInfo(name = "accessed_at")
    private long accessedAt; // timestamp

    // Constructors
    public PlaylistAccess() {}

    public PlaylistAccess(long userId, long playlistId, long accessedAt) {
        this.userId = userId;
        this.playlistId = playlistId;
        this.accessedAt = accessedAt;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public long getPlaylistId() { return playlistId; }
    public void setPlaylistId(long playlistId) { this.playlistId = playlistId; }

    public long getAccessedAt() { return accessedAt; }
    public void setAccessedAt(long accessedAt) { this.accessedAt = accessedAt; }
}
