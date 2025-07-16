package com.g3.soundify_musicplayer.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
    tableName = "playlist_songs",
    primaryKeys = {"playlist_id", "song_id"},
    foreignKeys = {
        @ForeignKey(
            entity = Playlist.class,
            parentColumns = "id",
            childColumns = "playlist_id",
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = Song.class,
            parentColumns = "id",
            childColumns = "song_id",
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(value = {"song_id"})
    }
)
public class PlaylistSong {
    @ColumnInfo(name = "playlist_id")
    private long playlistId;

    @ColumnInfo(name = "song_id")
    private long songId;

    @ColumnInfo(name = "position")
    private Integer position;

    // Constructors
    public PlaylistSong() {}

    public PlaylistSong(long playlistId, long songId) {
        this.playlistId = playlistId;
        this.songId = songId;
    }

    public PlaylistSong(long playlistId, long songId, Integer position) {
        this.playlistId = playlistId;
        this.songId = songId;
        this.position = position;
    }

    // Getters and Setters
    public long getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(long playlistId) {
        this.playlistId = playlistId;
    }

    public long getSongId() {
        return songId;
    }

    public void setSongId(long songId) {
        this.songId = songId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }
} 