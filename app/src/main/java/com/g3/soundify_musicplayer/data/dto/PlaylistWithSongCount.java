package com.g3.soundify_musicplayer.data.dto;

import com.g3.soundify_musicplayer.data.entity.Playlist;

/**
 * DTO class that combines Playlist with song count information
 * Used for displaying playlists with their song counts in UI
 */
public class PlaylistWithSongCount {
    private Playlist playlist;
    private int songCount;
    
    public PlaylistWithSongCount(Playlist playlist, int songCount) {
        this.playlist = playlist;
        this.songCount = songCount;
    }
    
    // Getters
    public Playlist getPlaylist() {
        return playlist;
    }
    
    public int getSongCount() {
        return songCount;
    }
    
    // Convenience methods to access playlist properties
    public long getId() {
        return playlist != null ? playlist.getId() : -1;
    }
    
    public String getName() {
        return playlist != null ? playlist.getName() : "";
    }
    
    public String getDescription() {
        return playlist != null ? playlist.getDescription() : "";
    }
    
    public boolean isPublic() {
        return playlist != null && playlist.isPublic();
    }
    
    public long getCreatedAt() {
        return playlist != null ? playlist.getCreatedAt() : 0;
    }
    
    public long getOwnerId() {
        return playlist != null ? playlist.getOwnerId() : -1;
    }
    
    // Setters
    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }
    
    public void setSongCount(int songCount) {
        this.songCount = songCount;
    }
}
