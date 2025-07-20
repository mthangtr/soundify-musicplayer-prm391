package com.g3.soundify_musicplayer.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "recently_played",
    foreignKeys = {
        @ForeignKey(
            entity = User.class,
            parentColumns = "id",
            childColumns = "user_id",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = Song.class,
            parentColumns = "id", 
            childColumns = "song_id",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(value = {"user_id", "song_id"}, unique = true),
        @Index("played_at"),
        @Index("song_id")
    }
)
public class RecentlyPlayed {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "user_id")
    private long userId;

    @ColumnInfo(name = "song_id") 
    private long songId;

    @ColumnInfo(name = "played_at")
    private long playedAt; // timestamp

    // Constructors
    public RecentlyPlayed() {}

    public RecentlyPlayed(long userId, long songId, long playedAt) {
        this.userId = userId;
        this.songId = songId;
        this.playedAt = playedAt;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public long getSongId() { return songId; }
    public void setSongId(long songId) { this.songId = songId; }

    public long getPlayedAt() { return playedAt; }
    public void setPlayedAt(long playedAt) { this.playedAt = playedAt; }
}
