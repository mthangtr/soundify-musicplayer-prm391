package com.g3.soundify_musicplayer.data.dto;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;

/**
 * DTO class to combine Song with Uploader information
 * Used for displaying songs with artist names in the UI
 */
public class SongWithUploader {
    
    @Embedded
    public Song song;
    
    @Relation(
        parentColumn = "uploader_id",
        entityColumn = "id"
    )
    public User uploader;
    
    // Constructors
    public SongWithUploader() {}
    
    public SongWithUploader(Song song, User uploader) {
        this.song = song;
        this.uploader = uploader;
    }
    
    // Getters and Setters
    public Song getSong() {
        return song;
    }
    
    public void setSong(Song song) {
        this.song = song;
    }
    
    public User getUploader() {
        return uploader;
    }
    
    public void setUploader(User uploader) {
        this.uploader = uploader;
    }
    
    /**
     * Get uploader display name with fallback
     */
    public String getUploaderDisplayName() {
        if (uploader != null) {
            if (uploader.getDisplayName() != null && !uploader.getDisplayName().trim().isEmpty()) {
                return uploader.getDisplayName();
            } else if (uploader.getUsername() != null && !uploader.getUsername().trim().isEmpty()) {
                return uploader.getUsername();
            }
        }
        return "Unknown Artist";
    }
    
    /**
     * Get uploader username with fallback
     */
    public String getUploaderUsername() {
        if (uploader != null && uploader.getUsername() != null) {
            return uploader.getUsername();
        }
        return "unknown";
    }
    
    /**
     * Get uploader avatar URL with fallback
     */
    public String getUploaderAvatarUrl() {
        if (uploader != null && uploader.getAvatarUrl() != null) {
            return uploader.getAvatarUrl();
        }
        return null;
    }
}
