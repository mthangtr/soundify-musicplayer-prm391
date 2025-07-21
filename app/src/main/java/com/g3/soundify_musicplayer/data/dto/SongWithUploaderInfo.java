package com.g3.soundify_musicplayer.data.dto;

import androidx.room.ColumnInfo;

/**
 * Simple DTO for Song with Uploader information
 * Used for JOIN queries that return flattened data
 */
public class SongWithUploaderInfo {

    // Song fields
    private long id;

    @ColumnInfo(name = "uploader_id")
    private long uploaderId;

    private String title;
    private String description;

    @ColumnInfo(name = "audio_url")
    private String audioUrl;

    @ColumnInfo(name = "cover_art_url")
    private String coverArtUrl;

    private String genre;

    @ColumnInfo(name = "duration_ms")
    private Integer durationMs;

    @ColumnInfo(name = "is_public")
    private boolean isPublic;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    // Uploader fields
    @ColumnInfo(name = "uploaderUsername")
    private String uploaderUsername;

    @ColumnInfo(name = "uploaderDisplayName")
    private String uploaderDisplayName;

    @ColumnInfo(name = "uploaderAvatarUrl")
    private String uploaderAvatarUrl;
    
    // Constructors
    public SongWithUploaderInfo() {}
    
    // Getters and Setters for Song fields
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
    
    // Getters and Setters for Uploader fields
    public String getUploaderUsername() {
        return uploaderUsername;
    }
    
    public void setUploaderUsername(String uploaderUsername) {
        this.uploaderUsername = uploaderUsername;
    }
    
    public String getUploaderDisplayName() {
        return uploaderDisplayName;
    }
    
    public void setUploaderDisplayName(String uploaderDisplayName) {
        this.uploaderDisplayName = uploaderDisplayName;
    }
    
    public String getUploaderAvatarUrl() {
        return uploaderAvatarUrl;
    }
    
    public void setUploaderAvatarUrl(String uploaderAvatarUrl) {
        this.uploaderAvatarUrl = uploaderAvatarUrl;
    }
    
    // Helper methods
    public String getDisplayUploaderName() {
        if (uploaderDisplayName != null && !uploaderDisplayName.trim().isEmpty()) {
            return uploaderDisplayName;
        } else if (uploaderUsername != null && !uploaderUsername.trim().isEmpty()) {
            return uploaderUsername;
        }
        return "Unknown Artist";
    }
}
