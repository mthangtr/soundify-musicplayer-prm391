package com.g3.soundify_musicplayer.data.model;

import com.g3.soundify_musicplayer.data.entity.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Class quản lý queue phát nhạc với support cho shuffle, repeat và navigation context
 */
public class PlaybackQueue {
    
    private List<Song> originalQueue;           // Queue gốc (không shuffle)
    private List<Song> currentQueue;            // Queue hiện tại (có thể đã shuffle)
    private final List<Integer> shuffleIndices;       // Mapping indices cho shuffle mode
    private int currentIndex;                   // Vị trí hiện tại trong queue
    private boolean isShuffleEnabled;
    private MediaPlayerState.RepeatMode repeatMode;
    private NavigationContext navigationContext;
    private final Random random;
    
    public PlaybackQueue() {
        this.originalQueue = new ArrayList<>();
        this.currentQueue = new ArrayList<>();
        this.shuffleIndices = new ArrayList<>();
        this.currentIndex = -1;
        this.isShuffleEnabled = false;
        this.repeatMode = MediaPlayerState.RepeatMode.OFF;
        this.random = new Random();
    }
    
    public PlaybackQueue(List<Song> songs, NavigationContext context) {
        this();
        setQueue(songs, context);
    }
    
    // ========== QUEUE MANAGEMENT ==========
    
    /**
     * Set queue mới với navigation context
     */
    public void setQueue(List<Song> songs, NavigationContext context) {
        this.originalQueue = new ArrayList<>(songs);
        this.navigationContext = context;
        this.currentIndex = context != null ? context.getCurrentPosition() : 0;
        
        rebuildCurrentQueue();
    }
    
    /**
     * Thêm bài hát vào cuối queue
     */
    public void addSong(Song song) {
        originalQueue.add(song);
        rebuildCurrentQueue();
    }
    
    /**
     * Thêm bài hát vào vị trí cụ thể
     */
    public void addSong(int position, Song song) {
        if (position >= 0 && position <= originalQueue.size()) {
            originalQueue.add(position, song);
            
            // Adjust current index if needed
            if (position <= currentIndex) {
                currentIndex++;
            }
            
            rebuildCurrentQueue();
        }
    }
    
    /**
     * Xóa bài hát khỏi queue
     */
    public void removeSong(int position) {
        if (position >= 0 && position < originalQueue.size()) {
            originalQueue.remove(position);
            
            // Adjust current index
            if (position < currentIndex) {
                currentIndex--;
            } else if (position == currentIndex) {
                // If removing current song, stay at same index (next song will play)
                if (currentIndex >= originalQueue.size()) {
                    currentIndex = originalQueue.size() - 1;
                }
            }
            
            rebuildCurrentQueue();
        }
    }
    
    /**
     * Di chuyển bài hát trong queue
     */
    public void moveSong(int fromPosition, int toPosition) {
        if (fromPosition >= 0 && fromPosition < originalQueue.size() &&
            toPosition >= 0 && toPosition < originalQueue.size()) {
            
            Song song = originalQueue.remove(fromPosition);
            originalQueue.add(toPosition, song);
            
            // Adjust current index
            if (fromPosition == currentIndex) {
                currentIndex = toPosition;
            } else if (fromPosition < currentIndex && toPosition >= currentIndex) {
                currentIndex--;
            } else if (fromPosition > currentIndex && toPosition <= currentIndex) {
                currentIndex++;
            }
            
            rebuildCurrentQueue();
        }
    }
    
    /**
     * Clear toàn bộ queue
     */
    public void clear() {
        originalQueue.clear();
        currentQueue.clear();
        shuffleIndices.clear();
        currentIndex = -1;
        navigationContext = null;
    }
    
    // ========== NAVIGATION METHODS ==========
    
    /**
     * Chuyển đến bài hát tiếp theo
     */
    public Song getNextSong() {
        if (isEmpty()) return null;

        switch (repeatMode) {
            case ONE:
                return getCurrentSong();

            case ALL:
                currentIndex = (currentIndex + 1) % currentQueue.size();
                return currentQueue.get(currentIndex);

            case OFF:
            default:
                if (hasNext()) {
                    currentIndex++;
                    return currentQueue.get(currentIndex);
                }
                return null;
        }
    }
    
    /**
     * Chuyển đến bài hát trước đó
     */
    public Song getPreviousSong() {
        if (isEmpty()) return null;

        switch (repeatMode) {
            case ONE:
                return getCurrentSong();

            case ALL:
                currentIndex = currentIndex - 1;
                if (currentIndex < 0) {
                    currentIndex = currentQueue.size() - 1;
                }
                return currentQueue.get(currentIndex);

            case OFF:
            default:
                if (hasPrevious()) {
                    currentIndex--;
                    return currentQueue.get(currentIndex);
                }
                return null;
        }
    }
    
    /**
     * Chuyển đến bài hát cụ thể
     */
    public Song jumpToSong(int index) {
        if (index >= 0 && index < currentQueue.size()) {
            currentIndex = index;
            return currentQueue.get(currentIndex);
        }
        return null;
    }
    
    /**
     * Chuyển đến bài hát theo ID
     */
    public Song jumpToSong(long songId) {
        for (int i = 0; i < currentQueue.size(); i++) {
            if (currentQueue.get(i).getId() == songId) {
                currentIndex = i;
                return currentQueue.get(i);
            }
        }
        return null;
    }
    
    // ========== SHUFFLE & REPEAT ==========
    
    /**
     * Toggle shuffle mode
     */
    public void toggleShuffle() {
        setShuffleEnabled(!isShuffleEnabled);
    }
    
    /**
     * Set shuffle mode
     */
    public void setShuffleEnabled(boolean enabled) {
        if (this.isShuffleEnabled != enabled) {
            this.isShuffleEnabled = enabled;
            rebuildCurrentQueue();
        }
    }
    
    /**
     * Cycle through repeat modes: OFF -> ALL -> ONE -> OFF
     */
    public MediaPlayerState.RepeatMode cycleRepeatMode() {
        switch (repeatMode) {
            case OFF:
                repeatMode = MediaPlayerState.RepeatMode.ALL;
                break;
            case ALL:
                repeatMode = MediaPlayerState.RepeatMode.ONE;
                break;
            case ONE:
            default:
                repeatMode = MediaPlayerState.RepeatMode.OFF;
                break;
        }
        return repeatMode;
    }
    
    /**
     * Rebuild current queue based on shuffle state
     */
    private void rebuildCurrentQueue() {
        if (originalQueue.isEmpty()) {
            currentQueue.clear();
            shuffleIndices.clear();
            return;
        }
        
        Song currentSong = getCurrentSong();
        
        if (isShuffleEnabled) {
            // Create shuffled queue
            shuffleIndices.clear();
            for (int i = 0; i < originalQueue.size(); i++) {
                shuffleIndices.add(i);
            }
            Collections.shuffle(shuffleIndices, random);
            
            // Build shuffled queue
            currentQueue.clear();
            for (int index : shuffleIndices) {
                currentQueue.add(originalQueue.get(index));
            }
            
            // Find current song in shuffled queue
            if (currentSong != null) {
                for (int i = 0; i < currentQueue.size(); i++) {
                    if (currentQueue.get(i).getId() == currentSong.getId()) {
                        currentIndex = i;
                        break;
                    }
                }
            }
        } else {
            // Use original queue
            currentQueue = new ArrayList<>(originalQueue);
            shuffleIndices.clear();
            
            // Restore original index
            if (navigationContext != null) {
                currentIndex = Math.min(navigationContext.getCurrentPosition(), currentQueue.size() - 1);
            }
        }
        
        // Ensure valid index
        if (currentIndex >= currentQueue.size()) {
            currentIndex = currentQueue.size() - 1;
        }
        if (currentIndex < 0 && !currentQueue.isEmpty()) {
            currentIndex = 0;
        }
    }
    
    // ========== GETTERS & UTILITY METHODS ==========
    
    public Song getCurrentSong() {
        if (currentIndex >= 0 && currentIndex < currentQueue.size()) {
            return currentQueue.get(currentIndex);
        }
        return null;
    }
    
    public boolean hasNext() {
        return currentIndex < currentQueue.size() - 1;
    }
    
    public boolean hasPrevious() {
        return currentIndex > 0;
    }
    
    public boolean isEmpty() {
        return currentQueue.isEmpty();
    }
    
    public int size() {
        return currentQueue.size();
    }
    
    public int getCurrentIndex() {
        return currentIndex;
    }
    
    public List<Song> getCurrentQueue() {
        return new ArrayList<>(currentQueue);
    }
    
    public List<Song> getOriginalQueue() {
        return new ArrayList<>(originalQueue);
    }
    
    public boolean isShuffleEnabled() {
        return isShuffleEnabled;
    }
    
    public MediaPlayerState.RepeatMode getRepeatMode() {
        return repeatMode;
    }
    
    public void setRepeatMode(MediaPlayerState.RepeatMode repeatMode) {
        this.repeatMode = repeatMode;
    }
    
    public NavigationContext getNavigationContext() {
        return navigationContext;
    }
    
    public MediaPlayerState.QueueInfo getQueueInfo() {
        String title = navigationContext != null ? navigationContext.getContextTitle() : "Queue";
        NavigationContext.Type type = navigationContext != null ? navigationContext.getType() : NavigationContext.Type.FROM_GENERAL;
        return new MediaPlayerState.QueueInfo(currentIndex, currentQueue.size(), title, type);
    }
}
