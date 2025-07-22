package com.g3.soundify_musicplayer.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "songs",
    foreignKeys = @ForeignKey(
        entity = User.class,
        parentColumns = "id",
        childColumns = "uploader_id",
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = {"uploader_id"}),
        @Index(value = {"is_public"})
    }
)
public class Song {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "uploader_id")
    private long uploaderId;

    // Transient field for uploader name (not stored in DB, populated via JOIN)
    @ColumnInfo(name = "uploaderName")
    private String uploaderName;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "audio_url")
    private String audioUrl;

    @ColumnInfo(name = "cover_art_url")
    private String coverArtUrl;

    @ColumnInfo(name = "genre")
    private String genre;

    @ColumnInfo(name = "duration_ms")
    private Integer durationMs;

    @ColumnInfo(name = "is_public")
    private boolean isPublic;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    // Constructors
    public Song() {}

    public Song(long uploaderId, String title, String audioUrl) {
        this.uploaderId = uploaderId;
        this.title = title;
        this.audioUrl = audioUrl;
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

    public long getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(long uploaderId) {
        this.uploaderId = uploaderId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getCoverArtUrl() {
        return coverArtUrl;
    }

    public void setCoverArtUrl(String coverArtUrl) {
        this.coverArtUrl = coverArtUrl;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Integer durationMs) {
        this.durationMs = durationMs;
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

    public String getUploaderName() {
        return uploaderName;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }
}