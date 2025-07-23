package com.g3.soundify_musicplayer.ui.search;

import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;

/**
 * Model class representing a search result item.
 * Can contain different types of data: Song, User, or Playlist.
 * Used for displaying unified search results in RecyclerView.
 */
public class SearchResult {
    
    public enum Type {
        SONG,
        ARTIST,
        PLAYLIST
    }
    
    private Type type;
    private Song song;
    private User user;
    private Playlist playlist;
    private long id;
    private String primaryText;
    private String secondaryText;
    private String tertiaryText;
    private String imageUrl;
    
    // Constructor for Song results
    public SearchResult(Song song, User artist) {
        this.type = Type.SONG;
        this.song = song;
        this.user = artist;
        this.id = song.getId();
        this.primaryText = song.getTitle();
        this.secondaryText = (artist != null ? 
            (artist.getDisplayName() != null ? artist.getDisplayName() : artist.getUsername()) : "Unknown Artist") +
            (song.getGenre() != null ? " • " + song.getGenre() : "");
        this.tertiaryText = formatDuration(song.getDurationMs());
        this.imageUrl = song.getCoverArtUrl();
    }
    
    // Constructor for Artist/User results
    public SearchResult(User user, int songCount) {
        this.type = Type.ARTIST;
        this.user = user;
        this.id = user.getId();
        this.primaryText = user.getDisplayName() != null ? user.getDisplayName() : user.getUsername();
        this.secondaryText = "@" + user.getUsername();
        this.tertiaryText = songCount + (songCount == 1 ? " song" : " songs");
        this.imageUrl = user.getAvatarUrl();
    }
    
    // Constructor for Playlist results
    public SearchResult(Playlist playlist, User owner, int songCount) {
        this.type = Type.PLAYLIST;
        this.playlist = playlist;
        this.user = owner;
        this.id = playlist.getId();
        this.primaryText = playlist.getName();
        this.secondaryText = "by " + (owner != null ? 
            (owner.getDisplayName() != null ? owner.getDisplayName() : owner.getUsername()) : "Unknown");
        this.tertiaryText = songCount + (songCount == 1 ? " song" : " songs");
        this.imageUrl = null; // Playlists don't have cover art in this implementation
    }
    
    // Getters
    public Type getType() {
        return type;
    }
    
    public Song getSong() {
        return song;
    }
    
    public User getUser() {
        return user;
    }
    
    public Playlist getPlaylist() {
        return playlist;
    }
    
    public long getId() {
        return id;
    }
    
    public String getPrimaryText() {
        return primaryText;
    }
    
    public String getSecondaryText() {
        return secondaryText;
    }
    
    public String getTertiaryText() {
        return tertiaryText;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    // Helper method to format duration
    private String formatDuration(Integer durationMs) {
        if (durationMs == null || durationMs <= 0) {
            return "";
        }
        
        int totalSeconds = durationMs / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        
        return String.format("%d:%02d", minutes, seconds);
    }

}
