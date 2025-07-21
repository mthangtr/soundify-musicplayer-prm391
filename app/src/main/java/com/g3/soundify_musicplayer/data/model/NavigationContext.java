package com.g3.soundify_musicplayer.data.model;

import java.io.Serializable;
import java.util.List;

/**
 * Navigation context data classes để quản lý các loại navigation khác nhau
 * khi user mở Song Detail screen từ các nguồn khác nhau
 */
public class NavigationContext implements Serializable {
    
    /**
     * Enum định nghĩa các loại navigation context
     */
    public enum Type {
        FROM_PLAYLIST,      // Từ playlist
        FROM_ARTIST,        // Từ artist/uploader profile
        FROM_SEARCH,        // Từ search results
        FROM_GENERAL        // Từ home/browse/recommendations
    }
    
    private Type type;
    private String contextTitle;        // Tên playlist, artist, search query, etc.
    private List<Long> songIds;         // Danh sách song IDs trong context
    private int currentPosition;        // Vị trí hiện tại trong danh sách
    private Long contextId;             // ID của playlist, artist, etc. (nullable)
    private String searchQuery;         // Search query nếu từ search (nullable)
    
    // Default constructor
    public NavigationContext() {
        this.currentPosition = 0;
    }
    
    // Constructor cho FROM_PLAYLIST
    public static NavigationContext fromPlaylist(long playlistId, String playlistName, List<Long> songIds, int currentPosition) {
        NavigationContext context = new NavigationContext();
        context.type = Type.FROM_PLAYLIST;
        context.contextId = playlistId;
        context.contextTitle = playlistName;
        context.songIds = songIds;
        context.currentPosition = currentPosition;
        return context;
    }
    
    // Constructor cho FROM_ARTIST
    public static NavigationContext fromArtist(long artistId, String artistName, List<Long> songIds, int currentPosition) {
        NavigationContext context = new NavigationContext();
        context.type = Type.FROM_ARTIST;
        context.contextId = artistId;
        context.contextTitle = artistName;
        context.songIds = songIds;
        context.currentPosition = currentPosition;
        return context;
    }
    
    // Constructor cho FROM_SEARCH
    public static NavigationContext fromSearch(String searchQuery, List<Long> songIds, int currentPosition) {
        NavigationContext context = new NavigationContext();
        context.type = Type.FROM_SEARCH;
        context.searchQuery = searchQuery;
        context.contextTitle = "Kết quả tìm kiếm: \"" + searchQuery + "\"";
        context.songIds = songIds;
        context.currentPosition = currentPosition;
        return context;
    }
    
    // Constructor cho FROM_GENERAL
    public static NavigationContext fromGeneral(String contextTitle, List<Long> songIds, int currentPosition) {
        NavigationContext context = new NavigationContext();
        context.type = Type.FROM_GENERAL;
        context.contextTitle = contextTitle;
        context.songIds = songIds;
        context.currentPosition = currentPosition;
        return context;
    }
    
    // Helper methods
    public boolean hasPrevious() {
        return currentPosition > 0;
    }
    
    public boolean hasNext() {
        return songIds != null && currentPosition < songIds.size() - 1;
    }
    
    public Long getPreviousSongId() {
        if (hasPrevious()) {
            return songIds.get(currentPosition - 1);
        }
        return null;
    }
    
    public Long getNextSongId() {
        if (hasNext()) {
            return songIds.get(currentPosition + 1);
        }
        return null;
    }
    
    public Long getCurrentSongId() {
        if (songIds != null && currentPosition >= 0 && currentPosition < songIds.size()) {
            return songIds.get(currentPosition);
        }
        return null;
    }
    
    public String getPositionText() {
        if (songIds != null && songIds.size() > 0) {
            return (currentPosition + 1) + " of " + songIds.size() + " songs";
        }
        return "";
    }
    
    public NavigationContext moveToPrevious() {
        if (hasPrevious()) {
            NavigationContext newContext = copy();
            newContext.currentPosition = currentPosition - 1;
            return newContext;
        }
        return this;
    }
    
    public NavigationContext moveToNext() {
        if (hasNext()) {
            NavigationContext newContext = copy();
            newContext.currentPosition = currentPosition + 1;
            return newContext;
        }
        return this;
    }
    
    private NavigationContext copy() {
        NavigationContext copy = new NavigationContext();
        copy.type = this.type;
        copy.contextTitle = this.contextTitle;
        copy.songIds = this.songIds;
        copy.currentPosition = this.currentPosition;
        copy.contextId = this.contextId;
        copy.searchQuery = this.searchQuery;
        return copy;
    }
    
    // Getters and Setters
    public Type getType() { 
        return type; 
    }
    
    public void setType(Type type) { 
        this.type = type; 
    }
    
    public String getContextTitle() { 
        return contextTitle; 
    }
    
    public void setContextTitle(String contextTitle) { 
        this.contextTitle = contextTitle; 
    }
    
    public List<Long> getSongIds() { 
        return songIds; 
    }
    
    public void setSongIds(List<Long> songIds) { 
        this.songIds = songIds; 
    }
    
    public int getCurrentPosition() { 
        return currentPosition; 
    }
    
    public void setCurrentPosition(int currentPosition) { 
        this.currentPosition = currentPosition; 
    }
    
    public Long getContextId() { 
        return contextId; 
    }
    
    public void setContextId(Long contextId) { 
        this.contextId = contextId; 
    }
    
    public String getSearchQuery() { 
        return searchQuery; 
    }
    
    public void setSearchQuery(String searchQuery) { 
        this.searchQuery = searchQuery; 
    }
    
    public int getTotalSongs() {
        return songIds != null ? songIds.size() : 0;
    }
}
