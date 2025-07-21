package com.g3.soundify_musicplayer.ui.player.queue;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Song;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for Queue Screen
 * UI ONLY - Uses mock data for demonstration
 */
public class QueueViewModel extends AndroidViewModel {

    // LiveData for UI state
    private final MutableLiveData<List<QueueItem>> queueItems = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentPosition = new MutableLiveData<>(0);

    public QueueViewModel(@NonNull Application application) {
        super(application);
    }

    // Public methods for Fragment to call
    public void loadQueue(long currentSongId) {
        // Create mock queue data
        List<QueueItem> mockQueue = createMockQueue(currentSongId);
        queueItems.setValue(mockQueue);
        currentPosition.setValue(0); // First song is currently playing
    }

    public void moveToPosition(int position) {
        currentPosition.setValue(position);
    }

    public void moveItem(int fromPosition, int toPosition) {
        List<QueueItem> currentQueue = queueItems.getValue();
        if (currentQueue != null && fromPosition < currentQueue.size() && toPosition < currentQueue.size()) {
            // Create a new list to avoid modifying the original
            List<QueueItem> newQueue = new ArrayList<>(currentQueue);
            
            // Move the item
            QueueItem item = newQueue.remove(fromPosition);
            newQueue.add(toPosition, item);
            
            // Update current position if needed
            Integer currentPos = currentPosition.getValue();
            if (currentPos != null) {
                if (fromPosition == currentPos) {
                    // Currently playing song was moved
                    currentPosition.setValue(toPosition);
                } else if (fromPosition < currentPos && toPosition >= currentPos) {
                    // Item moved from before current to after current
                    currentPosition.setValue(currentPos - 1);
                } else if (fromPosition > currentPos && toPosition <= currentPos) {
                    // Item moved from after current to before current
                    currentPosition.setValue(currentPos + 1);
                }
            }
            
            queueItems.setValue(newQueue);
        }
    }

    // Getters for LiveData
    public MutableLiveData<List<QueueItem>> getQueueItems() {
        return queueItems;
    }

    public MutableLiveData<Integer> getCurrentPosition() {
        return currentPosition;
    }

    // Mock data creation
    private List<QueueItem> createMockQueue(long currentSongId) {
        List<QueueItem> queue = new ArrayList<>();
        
        // Add current song (now playing)
        queue.add(new QueueItem(createMockSong(currentSongId, "Beautiful Sunset", "Ambient Artist"), true));
        
        // Add upcoming songs
        queue.add(new QueueItem(createMockSong(2L, "Ocean Waves", "Nature Sounds"), false));
        queue.add(new QueueItem(createMockSong(3L, "Mountain Breeze", "Relaxation Music"), false));
        queue.add(new QueueItem(createMockSong(4L, "Forest Rain", "Ambient Collective"), false));
        queue.add(new QueueItem(createMockSong(5L, "Starlight Dreams", "Dream Weaver"), false));
        queue.add(new QueueItem(createMockSong(6L, "Peaceful Morning", "Zen Masters"), false));
        queue.add(new QueueItem(createMockSong(7L, "Gentle Breeze", "Calm Sounds"), false));
        queue.add(new QueueItem(createMockSong(8L, "Sunset Meditation", "Mindful Music"), false));
        
        return queue;
    }

    private Song createMockSong(long id, String title, String artist) {
        Song song = new Song(1L, title, "file:///android_asset/" + title.toLowerCase().replace(" ", "_") + ".mp3");
        song.setId(id);
        song.setDescription("A beautiful " + title.toLowerCase() + " track");
        song.setUploaderId(1L);
        song.setGenre("Ambient");
        song.setDurationMs(180000 + (int)(Math.random() * 120000)); // 3-5 minutes
        song.setPublic(true);
        song.setCreatedAt(System.currentTimeMillis() - (id * 86400000L));
        song.setCoverArtUrl(""); // Empty for placeholder
        return song;
    }

    // Queue Item class
    public static class QueueItem {
        private Song song;
        private boolean isCurrentlyPlaying;

        public QueueItem(Song song, boolean isCurrentlyPlaying) {
            this.song = song;
            this.isCurrentlyPlaying = isCurrentlyPlaying;
        }

        public Song getSong() {
            return song;
        }

        public void setSong(Song song) {
            this.song = song;
        }

        public boolean isCurrentlyPlaying() {
            return isCurrentlyPlaying;
        }

        public void setCurrentlyPlaying(boolean currentlyPlaying) {
            isCurrentlyPlaying = currentlyPlaying;
        }
    }
}
