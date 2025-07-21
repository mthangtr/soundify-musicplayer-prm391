package com.g3.soundify_musicplayer.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.model.MediaPlayerState;
import com.g3.soundify_musicplayer.data.model.NavigationContext;
import com.g3.soundify_musicplayer.data.model.PlaybackQueue;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Repository quản lý media player operations và playback state
 * Extends SongDetailRepository để có access đến song data operations
 */
public class MediaPlayerRepository extends SongDetailRepository {
    
    private ExecutorService mediaExecutor;
    
    // LiveData cho playback state
    private MutableLiveData<MediaPlayerState.CurrentPlaybackState> currentPlaybackState;
    private MutableLiveData<MediaPlayerState.QueueInfo> queueInfo;
    private MutableLiveData<MediaPlayerState.PlaybackError> playbackError;
    private MutableLiveData<Boolean> isPlayerVisible;
    
    // Playback queue management
    private PlaybackQueue playbackQueue;
    private MediaPlayerState.CurrentPlaybackState currentState;
    
    // Mock ExoPlayer simulation
    private boolean isPlayerInitialized = false;
    private long mockCurrentPosition = 0;
    private long mockDuration = 0;
    
    public MediaPlayerRepository(Application application) {
        super(application);
        mediaExecutor = Executors.newFixedThreadPool(2);
        
        // Initialize LiveData
        currentPlaybackState = new MutableLiveData<>();
        queueInfo = new MutableLiveData<>();
        playbackError = new MutableLiveData<>();
        isPlayerVisible = new MutableLiveData<>();
        
        // Initialize state
        playbackQueue = new PlaybackQueue();
        currentState = new MediaPlayerState.CurrentPlaybackState();
        
        // Set initial values
        currentPlaybackState.setValue(currentState);
        queueInfo.setValue(new MediaPlayerState.QueueInfo());
        isPlayerVisible.setValue(false);
    }
    
    // ========== PLAYBACK CONTROL METHODS ==========
    
    /**
     * Play current song
     */
    public Future<Boolean> play() {
        return mediaExecutor.submit(() -> {
            try {
                if (currentState.getCurrentSong() == null) {
                    // No song to play
                    return false;
                }
                
                if (currentState.getPlaybackState() == MediaPlayerState.PlaybackState.PAUSED) {
                    // Resume playback
                    currentState.setPlaybackState(MediaPlayerState.PlaybackState.PLAYING);
                } else {
                    // Start new playback
                    currentState.setPlaybackState(MediaPlayerState.PlaybackState.LOADING);
                    currentPlaybackState.postValue(currentState);
                    
                    // Simulate loading time
                    Thread.sleep(500);
                    
                    // Mock duration setup
                    mockDuration = 180000; // 3 minutes
                    currentState.setDuration(mockDuration);
                    currentState.setPlaybackState(MediaPlayerState.PlaybackState.PLAYING);
                }
                
                currentPlaybackState.postValue(currentState);
                isPlayerVisible.postValue(true);
                
                // Start position update simulation
                startPositionUpdates();
                
                return true;
                
            } catch (Exception e) {
                handlePlaybackError("Error starting playback", e);
                return false;
            }
        });
    }
    
    /**
     * Pause playback
     */
    public Future<Boolean> pause() {
        return mediaExecutor.submit(() -> {
            try {
                if (currentState.canPause()) {
                    currentState.setPlaybackState(MediaPlayerState.PlaybackState.PAUSED);
                    currentPlaybackState.postValue(currentState);
                    return true;
                }
                return false;
            } catch (Exception e) {
                handlePlaybackError("Error pausing playback", e);
                return false;
            }
        });
    }
    
    /**
     * Stop playback
     */
    public Future<Boolean> stop() {
        return mediaExecutor.submit(() -> {
            try {
                currentState.setPlaybackState(MediaPlayerState.PlaybackState.STOPPED);
                currentState.setCurrentPosition(0);
                mockCurrentPosition = 0;
                currentPlaybackState.postValue(currentState);
                return true;
            } catch (Exception e) {
                handlePlaybackError("Error stopping playback", e);
                return false;
            }
        });
    }
    
    /**
     * Seek to position
     */
    public Future<Boolean> seekTo(long positionMs) {
        return mediaExecutor.submit(() -> {
            try {
                if (positionMs >= 0 && positionMs <= currentState.getDuration()) {
                    mockCurrentPosition = positionMs;
                    currentState.setCurrentPosition(positionMs);
                    currentPlaybackState.postValue(currentState);
                    return true;
                }
                return false;
            } catch (Exception e) {
                handlePlaybackError("Error seeking", e);
                return false;
            }
        });
    }
    
    /**
     * Toggle play/pause
     */
    public Future<Boolean> togglePlayPause() {
        return mediaExecutor.submit(() -> {
            try {
                if (currentState.isPlaying()) {
                    return pause().get();
                } else {
                    return play().get();
                }
            } catch (Exception e) {
                handlePlaybackError("Error toggling playback", e);
                return false;
            }
        });
    }
    
    // ========== QUEUE MANAGEMENT ==========
    
    /**
     * Play song with navigation context
     */
    public Future<Boolean> playSongWithContext(Song song, NavigationContext context) {
        return mediaExecutor.submit(() -> {
            try {
                // Load songs for the context
                List<Song> contextSongs = getContextSongs(context).get();
                
                // Set up queue
                playbackQueue.setQueue(contextSongs, context);
                
                // Jump to the specific song
                Song targetSong = playbackQueue.jumpToSong(song.getId());
                if (targetSong != null) {
                    currentState.setCurrentSong(targetSong);
                    currentState.setCurrentQueueIndex(playbackQueue.getCurrentIndex());
                    updateQueueInfo();
                    
                    // Start playback
                    return play().get();
                }
                
                return false;
                
            } catch (Exception e) {
                handlePlaybackError("Error playing song with context", e);
                return false;
            }
        });
    }
    
    /**
     * Play next song in queue
     */
    public Future<Boolean> playNext() {
        return mediaExecutor.submit(() -> {
            try {
                Song nextSong = playbackQueue.getNextSong();
                if (nextSong != null) {
                    currentState.setCurrentSong(nextSong);
                    currentState.setCurrentQueueIndex(playbackQueue.getCurrentIndex());
                    updateQueueInfo();
                    
                    // Reset position and start playback
                    mockCurrentPosition = 0;
                    currentState.setCurrentPosition(0);
                    
                    return play().get();
                }
                
                // No next song available
                if (playbackQueue.getRepeatMode() == MediaPlayerState.RepeatMode.OFF) {
                    stop();
                }
                
                return false;
                
            } catch (Exception e) {
                handlePlaybackError("Error playing next song", e);
                return false;
            }
        });
    }
    
    /**
     * Play previous song in queue
     */
    public Future<Boolean> playPrevious() {
        return mediaExecutor.submit(() -> {
            try {
                // If current position > 3 seconds, restart current song
                if (currentState.getCurrentPosition() > 3000) {
                    return seekTo(0).get() && play().get();
                }
                
                Song previousSong = playbackQueue.getPreviousSong();
                if (previousSong != null) {
                    currentState.setCurrentSong(previousSong);
                    currentState.setCurrentQueueIndex(playbackQueue.getCurrentIndex());
                    updateQueueInfo();
                    
                    // Reset position and start playback
                    mockCurrentPosition = 0;
                    currentState.setCurrentPosition(0);
                    
                    return play().get();
                }
                
                return false;
                
            } catch (Exception e) {
                handlePlaybackError("Error playing previous song", e);
                return false;
            }
        });
    }
    
    /**
     * Add song to queue
     */
    public Future<Boolean> addToQueue(Song song) {
        return mediaExecutor.submit(() -> {
            try {
                playbackQueue.addSong(song);
                updateQueueInfo();
                return true;
            } catch (Exception e) {
                handlePlaybackError("Error adding song to queue", e);
                return false;
            }
        });
    }
    
    /**
     * Remove song from queue
     */
    public Future<Boolean> removeFromQueue(int position) {
        return mediaExecutor.submit(() -> {
            try {
                playbackQueue.removeSong(position);
                updateQueueInfo();
                
                // If removed current song, play next
                if (position == playbackQueue.getCurrentIndex() && currentState.isPlaying()) {
                    playNext();
                }
                
                return true;
            } catch (Exception e) {
                handlePlaybackError("Error removing song from queue", e);
                return false;
            }
        });
    }
    
    // ========== SHUFFLE & REPEAT ==========
    
    /**
     * Toggle shuffle mode
     */
    public Future<Boolean> toggleShuffle() {
        return mediaExecutor.submit(() -> {
            try {
                playbackQueue.toggleShuffle();
                currentState.setShuffleEnabled(playbackQueue.isShuffleEnabled());
                updateQueueInfo();
                currentPlaybackState.postValue(currentState);
                return true;
            } catch (Exception e) {
                handlePlaybackError("Error toggling shuffle", e);
                return false;
            }
        });
    }
    
    /**
     * Cycle repeat mode
     */
    public Future<MediaPlayerState.RepeatMode> cycleRepeatMode() {
        return mediaExecutor.submit(() -> {
            try {
                MediaPlayerState.RepeatMode newMode = playbackQueue.cycleRepeatMode();
                currentState.setRepeatMode(newMode);
                currentPlaybackState.postValue(currentState);
                return newMode;
            } catch (Exception e) {
                handlePlaybackError("Error cycling repeat mode", e);
                return MediaPlayerState.RepeatMode.OFF;
            }
        });
    }
    
    // ========== SONG OPERATIONS (DELEGATED) ==========

    /**
     * Toggle song like (delegated to parent)
     */
    public Future<Boolean> toggleSongLike(long songId, long userId) {
        return mediaExecutor.submit(() -> {
            try {
                return super.toggleSongLike(songId, userId).get();
            } catch (Exception e) {
                android.util.Log.e("MediaPlayerRepository", "Error toggling song like", e);
                return false;
            }
        });
    }

    /**
     * Add comment (delegated to parent)
     */
    public Future<Long> addComment(long songId, long userId, String content) {
        return mediaExecutor.submit(() -> {
            try {
                return super.addComment(songId, userId, content).get();
            } catch (Exception e) {
                android.util.Log.e("MediaPlayerRepository", "Error adding comment", e);
                return -1L;
            }
        });
    }

    // ========== HELPER METHODS ==========

    /**
     * Get song by ID synchronously (for internal use)
     */
    public Future<Song> getSongByIdSync(long songId) {
        return mediaExecutor.submit(() -> {
            try {
                return super.getSongByIdSync(songId).get();
            } catch (Exception e) {
                android.util.Log.e("MediaPlayerRepository", "Error getting song by ID", e);
                return null;
            }
        });
    }

    /**
     * Get songs for navigation context
     */
    private Future<List<Song>> getContextSongs(NavigationContext context) {
        return mediaExecutor.submit(() -> {
            try {
                List<Long> songIds = context.getSongIds();
                List<Song> songs = new java.util.ArrayList<>();
                
                for (Long songId : songIds) {
                    Song song = getSongByIdSync(songId).get();
                    if (song != null) {
                        songs.add(song);
                    }
                }
                
                return songs;
                
            } catch (Exception e) {
                android.util.Log.e("MediaPlayerRepository", "Error getting context songs", e);
                return new java.util.ArrayList<>();
            }
        });
    }
    
    /**
     * Update queue info LiveData
     */
    private void updateQueueInfo() {
        MediaPlayerState.QueueInfo info = playbackQueue.getQueueInfo();
        queueInfo.postValue(info);
    }
    
    /**
     * Handle playback errors
     */
    private void handlePlaybackError(String message, Exception e) {
        android.util.Log.e("MediaPlayerRepository", message, e);
        currentState.setPlaybackState(MediaPlayerState.PlaybackState.ERROR);
        currentPlaybackState.postValue(currentState);
        playbackError.postValue(new MediaPlayerState.PlaybackError(message, e));
    }
    
    /**
     * Start position updates (mock implementation)
     */
    private void startPositionUpdates() {
        mediaExecutor.execute(() -> {
            while (currentState.isPlaying()) {
                try {
                    Thread.sleep(1000); // Update every second
                    
                    if (currentState.isPlaying()) {
                        mockCurrentPosition += 1000;
                        
                        // Check if song finished
                        if (mockCurrentPosition >= mockDuration) {
                            mockCurrentPosition = mockDuration;
                            currentState.setCurrentPosition(mockCurrentPosition);
                            currentPlaybackState.postValue(currentState);
                            
                            // Auto play next
                            playNext();
                            break;
                        } else {
                            currentState.setCurrentPosition(mockCurrentPosition);
                            currentPlaybackState.postValue(currentState);
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }
    
    // ========== GETTERS FOR LIVEDATA ==========
    
    public LiveData<MediaPlayerState.CurrentPlaybackState> getCurrentPlaybackState() {
        return currentPlaybackState;
    }
    
    public LiveData<MediaPlayerState.QueueInfo> getQueueInfo() {
        return queueInfo;
    }
    
    public LiveData<MediaPlayerState.PlaybackError> getPlaybackError() {
        return playbackError;
    }
    
    public LiveData<Boolean> getIsPlayerVisible() {
        return isPlayerVisible;
    }
    
    public PlaybackQueue getPlaybackQueue() {
        return playbackQueue;
    }
    
    // ========== PLAYER VISIBILITY ==========
    
    public void showPlayer() {
        isPlayerVisible.setValue(true);
    }
    
    public void hidePlayer() {
        isPlayerVisible.setValue(false);
    }
    
    @Override
    public void shutdown() {
        super.shutdown();
        if (mediaExecutor != null) {
            mediaExecutor.shutdown();
        }
    }
}
