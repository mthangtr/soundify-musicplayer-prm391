package com.g3.soundify_musicplayer.data.model;

import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;

/**
 * Data classes để quản lý trạng thái media player
 */
public class MediaPlayerState {
    
    /**
     * Enum định nghĩa các trạng thái playback
     */
    public enum PlaybackState {
        IDLE,           // Chưa load media
        LOADING,        // Đang load media
        READY,          // Sẵn sàng phát
        PLAYING,        // Đang phát
        PAUSED,         // Tạm dừng
        STOPPED,        // Dừng
        ERROR           // Lỗi
    }
    
    /**
     * Enum định nghĩa các chế độ repeat
     */
    public enum RepeatMode {
        OFF,            // Không repeat
        ONE,            // Repeat một bài
        ALL             // Repeat toàn bộ queue
    }
    
    /**
     * Class chứa thông tin trạng thái playback hiện tại
     */
    public static class CurrentPlaybackState {
        private Song currentSong;
        private User currentArtist; // ADDED: Centralized artist state
        private PlaybackState playbackState;
        private long currentPosition;       // Vị trí hiện tại (milliseconds)
        private long duration;              // Tổng thời lượng (milliseconds)
        private boolean isShuffleEnabled;
        private RepeatMode repeatMode;
        private float playbackSpeed;        // Tốc độ phát (1.0f = normal)
        private int currentQueueIndex;      // Vị trí trong queue
        
        public CurrentPlaybackState() {
            this.playbackState = PlaybackState.IDLE;
            this.currentPosition = 0;
            this.duration = 0;
            this.isShuffleEnabled = false;
            this.repeatMode = RepeatMode.OFF;
            this.playbackSpeed = 1.0f;
            this.currentQueueIndex = -1;
        }
        
        public CurrentPlaybackState(Song currentSong, PlaybackState playbackState) {
            this();
            this.currentSong = currentSong;
            this.playbackState = playbackState;
        }
        
        // Helper methods
        public boolean isPlaying() {
            return playbackState == PlaybackState.PLAYING;
        }
        
        public boolean isPaused() {
            return playbackState == PlaybackState.PAUSED;
        }
        
        public boolean isLoading() {
            return playbackState == PlaybackState.LOADING;
        }
        
        public boolean hasError() {
            return playbackState == PlaybackState.ERROR;
        }
        
        public boolean canPlay() {
            return playbackState == PlaybackState.READY || playbackState == PlaybackState.PAUSED;
        }
        
        public boolean canPause() {
            return playbackState == PlaybackState.PLAYING;
        }
        
        public int getProgressPercentage() {
            if (duration > 0) {
                return (int) ((currentPosition * 100) / duration);
            }
            return 0;
        }
        
        public String getFormattedCurrentPosition() {
            return formatTime(currentPosition);
        }
        
        public String getFormattedDuration() {
            return formatTime(duration);
        }
        
        private String formatTime(long timeMs) {
            long seconds = timeMs / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%d:%02d", minutes, seconds);
        }
        
        // Getters and Setters
        public Song getCurrentSong() { return currentSong; }
        public void setCurrentSong(Song currentSong) { this.currentSong = currentSong; }

        public User getCurrentArtist() { return currentArtist; }
        public void setCurrentArtist(User currentArtist) { this.currentArtist = currentArtist; }
        
        public PlaybackState getPlaybackState() { return playbackState; }
        public void setPlaybackState(PlaybackState playbackState) { this.playbackState = playbackState; }
        
        public long getCurrentPosition() { return currentPosition; }
        public void setCurrentPosition(long currentPosition) { this.currentPosition = currentPosition; }
        
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        
        public boolean isShuffleEnabled() { return isShuffleEnabled; }
        public void setShuffleEnabled(boolean shuffleEnabled) { isShuffleEnabled = shuffleEnabled; }
        
        public RepeatMode getRepeatMode() { return repeatMode; }
        public void setRepeatMode(RepeatMode repeatMode) { this.repeatMode = repeatMode; }
        
        public float getPlaybackSpeed() { return playbackSpeed; }
        public void setPlaybackSpeed(float playbackSpeed) { this.playbackSpeed = playbackSpeed; }
        
        public int getCurrentQueueIndex() { return currentQueueIndex; }
        public void setCurrentQueueIndex(int currentQueueIndex) { this.currentQueueIndex = currentQueueIndex; }
    }
    
    /**
     * Class chứa thông tin về queue và navigation
     */
    public static class QueueInfo {
        private int currentIndex;
        private int totalSongs;
        private boolean hasPrevious;
        private boolean hasNext;
        private String queueTitle;          // Tên playlist, artist, search query, etc.
        private NavigationContext.Type contextType;
        
        public QueueInfo() {
            this.currentIndex = -1;
            this.totalSongs = 0;
            this.hasPrevious = false;
            this.hasNext = false;
            this.queueTitle = "";
        }
        
        public QueueInfo(int currentIndex, int totalSongs, String queueTitle, NavigationContext.Type contextType) {
            this.currentIndex = currentIndex;
            this.totalSongs = totalSongs;
            this.queueTitle = queueTitle;
            this.contextType = contextType;
            this.hasPrevious = currentIndex > 0;
            this.hasNext = currentIndex < totalSongs - 1;
        }
        
        public String getPositionText() {
            if (totalSongs > 0 && currentIndex >= 0) {
                return (currentIndex + 1) + " of " + totalSongs + " songs";
            }
            return "";
        }
        
        public String getContextActionText() {
            if (contextType == null) return "";
            
            switch (contextType) {
                case FROM_PLAYLIST:
                    return "View Playlist";
                case FROM_ARTIST:
                    return "View Artist Profile";
                case FROM_SEARCH:
                    return "Back to Search Results";
                case FROM_GENERAL:
                    return "Back to Browse";
                default:
                    return "";
            }
        }
        
        // Getters and Setters
        public int getCurrentIndex() { return currentIndex; }
        public void setCurrentIndex(int currentIndex) { 
            this.currentIndex = currentIndex;
            this.hasPrevious = currentIndex > 0;
            this.hasNext = currentIndex < totalSongs - 1;
        }
        
        public int getTotalSongs() { return totalSongs; }
        public void setTotalSongs(int totalSongs) { 
            this.totalSongs = totalSongs;
            this.hasNext = currentIndex < totalSongs - 1;
        }
        
        public boolean isHasPrevious() { return hasPrevious; }
        public boolean isHasNext() { return hasNext; }
        
        public String getQueueTitle() { return queueTitle; }
        public void setQueueTitle(String queueTitle) { this.queueTitle = queueTitle; }
        
        public NavigationContext.Type getContextType() { return contextType; }
        public void setContextType(NavigationContext.Type contextType) { this.contextType = contextType; }
    }
    
    /**
     * Class chứa thông tin error
     */
    public static class PlaybackError {
        private String errorMessage;
        private int errorCode;
        private Exception exception;
        
        public PlaybackError(String errorMessage) {
            this.errorMessage = errorMessage;
            this.errorCode = -1;
        }
        
        public PlaybackError(String errorMessage, int errorCode) {
            this.errorMessage = errorMessage;
            this.errorCode = errorCode;
        }
        
        public PlaybackError(String errorMessage, Exception exception) {
            this.errorMessage = errorMessage;
            this.exception = exception;
            this.errorCode = -1;
        }
        
        // Getters
        public String getErrorMessage() { return errorMessage; }
        public int getErrorCode() { return errorCode; }
        public Exception getException() { return exception; }
    }
}
