package com.g3.soundify_musicplayer.data.repository;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.model.MediaPlayerState;
import com.g3.soundify_musicplayer.service.MediaPlaybackService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Simple Media Player Repository - Zero Queue Rule
 * Chỉ quản lý danh sách bài hát hiện tại với navigation cơ bản
 */
public class MediaPlayerRepository extends SongDetailRepository implements MediaPlaybackService.PlaybackStateListener {

    private final ExecutorService executor;
    private final Application application;

    // LiveData
    private final MutableLiveData<MediaPlayerState.CurrentPlaybackState> currentPlaybackState;
    private final MutableLiveData<MediaPlayerState.QueueInfo> queueInfo;
    private final MutableLiveData<Boolean> isPlayerVisible;

    // Zero Queue Rule - 3 fields only
    private final List<Song> currentSongList = new ArrayList<>();
    private int currentIndex = 0;
    private String currentListTitle = "";

    private final MediaPlayerState.CurrentPlaybackState currentState;

    // Service
    private MediaPlaybackService mediaService;
    private boolean isServiceBound = false;
    private ServiceConnection serviceConnection;
    
    public MediaPlayerRepository(Application application) {
        super(application);
        this.application = application;
        executor = Executors.newSingleThreadExecutor();

        // Initialize LiveData
        currentPlaybackState = new MutableLiveData<>();
        queueInfo = new MutableLiveData<>();
        isPlayerVisible = new MutableLiveData<>();

        currentState = new MediaPlayerState.CurrentPlaybackState();

        // Set initial values
        currentPlaybackState.setValue(currentState);
        queueInfo.setValue(new MediaPlayerState.QueueInfo());
        isPlayerVisible.setValue(false);

        // Bind to service
        bindToMediaService();
    }

    /**
     * Simple service binding
     */
    private void bindToMediaService() {
        Intent serviceIntent = new Intent(application, MediaPlaybackService.class);
        
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MediaPlaybackService.MediaPlaybackBinder binder = (MediaPlaybackService.MediaPlaybackBinder) service;
                mediaService = binder.getService();
                isServiceBound = true;
                
                if (mediaService != null) {
                    mediaService.setPlaybackStateListener(MediaPlayerRepository.this);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mediaService = null;
                isServiceBound = false;
            }
        };

        try {
            application.startService(serviceIntent);
            application.bindService(serviceIntent, serviceConnection, android.content.Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            android.util.Log.e("MediaPlayerRepository", "Service binding failed", e);
        }
    }
    
    // ========== CORE METHODS - ZERO QUEUE RULE ==========

    /**
     * Replace entire list and play (Zero Queue Rule)
     */
    public void replaceListAndPlay(List<Song> songs, String title, int startIndex) {
        executor.execute(() -> {
            try {
                // Clear and replace
                currentSongList.clear();
                currentSongList.addAll(songs);
                currentListTitle = title;
                currentIndex = startIndex;

                // Update state
                Song songToPlay = currentSongList.get(currentIndex);
                currentState.setCurrentSong(songToPlay);
                updateQueueInfo();

                // Start playback
                if (isServiceBound && mediaService != null) {
                    mediaService.playSong(songToPlay, null);
                }

                isPlayerVisible.postValue(true);
            } catch (Exception e) {
                android.util.Log.e("MediaPlayerRepository", "Error in replaceListAndPlay", e);
            }
        });
    }


    /**
     * Play next song
     */
    public Future<Boolean> playNext() {
        return executor.submit(() -> {
            try {
                if (currentSongList.isEmpty() || currentIndex >= currentSongList.size() - 1) {
                    return false;
                }

                currentIndex++;
                Song nextSong = currentSongList.get(currentIndex);

                currentState.setCurrentSong(nextSong);
                currentState.setCurrentPosition(0);
                updateQueueInfo();

                if (isServiceBound && mediaService != null) {
                    mediaService.playSong(nextSong, null);
                }

                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * Play previous song
     */
    public Future<Boolean> playPrevious() {
        return executor.submit(() -> {
            try {
                if (currentSongList.isEmpty()) {
                    return false;
                }
                android.util.Log.d("MediaPlayerRepository", "Playing previous song===================== 2");

                // Get current position
                long currentPosition = 0;
                try {
                    if (mediaService != null) {
                        currentPosition = mediaService.getCurrentPosition();
                        android.util.Log.d("MediaPlayerRepository", "Got position from service: " + currentPosition);
                    } else {
                        currentPosition = currentState.getCurrentPosition();
                        android.util.Log.d("MediaPlayerRepository", "Got position from state: " + currentPosition);
                    }
                } catch (Exception positionException) {
                    android.util.Log.e("MediaPlayerRepository", "Error getting position, using fallback", positionException);
                    currentPosition = currentState.getCurrentPosition();
                }



                android.util.Log.d("MediaPlayerRepository", "Playing previous song===================== 3");
                // If position > 3 seconds, restart current song
                if (currentPosition > 3000) {
                    return seekToSync(0);
                }


                android.util.Log.d("MediaPlayerRepository", "Playing previous song===================== 4");

                // Go to previous song
                if (currentIndex > 0) {
                    currentIndex--;
                    Song prevSong = currentSongList.get(currentIndex);

                    currentState.setCurrentSong(prevSong);
                    currentState.setCurrentPosition(0);
                    updateQueueInfo();

                    if (isServiceBound && mediaService != null) {

                        android.util.Log.d("MediaPlayerRepository", "Playing previous song===================== 5");
                        mediaService.playSong(prevSong, null);
                    }

                    return true;
                } else {
                    return seekToSync(0); // Restart current song
                }

            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * Toggle play/pause
     */
    public Future<Boolean> togglePlayPause() {
        return executor.submit(() -> {
            try {
                if (currentState.isPlaying()) {
                    return pauseSync();
                } else {
                    return resumeSync();
                }
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * Pause playback
     */
    public Future<Boolean> pause() {
        return executor.submit(this::pauseSync);
    }

    private boolean pauseSync() {
        try {
            if (mediaService != null) {
                mediaService.pause();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean resumeSync() {
        try {
            if (mediaService != null) {
                mediaService.resume();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Seek to position
     */
    public Future<Boolean> seekTo(long positionMs) {
        return executor.submit(() -> seekToSync(positionMs));
    }

    private boolean seekToSync(long positionMs) {
        try {
            if (positionMs >= 0 && positionMs <= currentState.getDuration()) {
                if (mediaService != null) {
                    mediaService.seekTo(positionMs);
                }
                currentState.setCurrentPosition(positionMs);
                currentPlaybackState.postValue(currentState);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Jump to specific index in current list
     */
    public void jumpToIndex(int position) {
        executor.submit(() -> {
            try {
                if (position >= 0 && position < currentSongList.size()) {
                    currentIndex = position;
                    Song songToPlay = currentSongList.get(currentIndex);

                    currentState.setCurrentSong(songToPlay);
                    updateQueueInfo();

                    if (isServiceBound && mediaService != null) {
                        mediaService.playSong(songToPlay, currentState.getCurrentArtist());
                    }

                    return true;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * Move item in current list (for drag & drop)
     */
    public void moveItemInList(int fromPosition, int toPosition) {
        executor.submit(() -> {
            try {
                if (fromPosition >= 0 && fromPosition < currentSongList.size() &&
                        toPosition >= 0 && toPosition < currentSongList.size()) {

                    // Move the song
                    Song songToMove = currentSongList.remove(fromPosition);
                    currentSongList.add(toPosition, songToMove);

                    // Adjust current index
                    if (fromPosition == currentIndex) {
                        currentIndex = toPosition;
                    } else if (fromPosition < currentIndex && toPosition >= currentIndex) {
                        currentIndex--;
                    } else if (fromPosition > currentIndex && toPosition <= currentIndex) {
                        currentIndex++;
                    }

                    updateQueueInfo();
                    return true;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        });
    }

    // ========== GETTERS ==========

    public List<Song> getCurrentQueue() {
        return new ArrayList<>(currentSongList);
    }

    public LiveData<MediaPlayerState.CurrentPlaybackState> getCurrentPlaybackState() {
        return currentPlaybackState;
    }

    public LiveData<MediaPlayerState.QueueInfo> getQueueInfo() {
        return queueInfo;
    }

    public LiveData<Boolean> getIsPlayerVisible() {
        return isPlayerVisible;
    }

    public void hidePlayer() {
        isPlayerVisible.setValue(false);
    }

    private void updateQueueInfo() {
        MediaPlayerState.QueueInfo info = new MediaPlayerState.QueueInfo(
            currentIndex,
            currentSongList.size(),
            currentListTitle,
            currentListTitle
        );
        queueInfo.postValue(info);
    }

    public void cleanup() {
        if (isServiceBound && serviceConnection != null) {
            try {
                application.unbindService(serviceConnection);
            } catch (Exception e) {
                // Ignore
            }
        }
        mediaService = null;
        isServiceBound = false;
        serviceConnection = null;
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        currentState.setPlaybackState(isPlaying ?
            MediaPlayerState.PlaybackState.PLAYING :
            MediaPlayerState.PlaybackState.PAUSED);
        currentPlaybackState.postValue(currentState);
    }

    @Override
    public void onPositionChanged(long currentPosition, long duration) {
        currentState.setCurrentPosition(currentPosition);
        currentState.setDuration(duration);
        currentPlaybackState.postValue(currentState);
    }

    @Override
    public void onSongChanged(Song song, User artist) {
        currentState.setCurrentSong(song);
        currentState.setCurrentArtist(artist);
        currentPlaybackState.postValue(currentState);
        isPlayerVisible.postValue(true);
    }

    @Override
    public void onSongCompleted() {
        executor.execute(() -> {
            try {
                playNext().get();
            } catch (Exception e) {
                // Ignore
            }
        });
    }

    @Override
    public void onPlayerError(String error) {
        currentState.setPlaybackState(MediaPlayerState.PlaybackState.ERROR);
        currentPlaybackState.postValue(currentState);
    }

    /**
     * Check service binding status
     * Called from RepositoryManager to verify service connection
     */
    public void checkServiceStatus() {
        if (!isServiceBound || mediaService == null) {
            android.util.Log.w("MediaPlayerRepository", "Service not properly connected - attempting rebind");
            bindToMediaService();
        }
    }

    @Override
    public void shutdown() {
        cleanup();
        super.shutdown();
        if (executor != null) {
            executor.shutdown();
        }
    }
}
