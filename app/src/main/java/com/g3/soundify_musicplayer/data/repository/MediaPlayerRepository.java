package com.g3.soundify_musicplayer.data.repository;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.model.MediaPlayerState;
import com.g3.soundify_musicplayer.data.model.NavigationContext;
import com.g3.soundify_musicplayer.data.model.PlaybackQueue;
import com.g3.soundify_musicplayer.service.MediaPlaybackService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Repository quản lý media player operations và playback state
 * Integrates with real MediaPlaybackService for actual song playback
 * Extends SongDetailRepository để có access đến song data operations
 * IMPLEMENTS PlaybackStateListener để nhận trạng thái thực tế từ ExoPlayer
 */
public class MediaPlayerRepository extends SongDetailRepository implements MediaPlaybackService.PlaybackStateListener {

    private final ExecutorService mediaExecutor;
    private final Application application;

    // LiveData cho playback state
    private final MutableLiveData<MediaPlayerState.CurrentPlaybackState> currentPlaybackState;
    private final MutableLiveData<MediaPlayerState.QueueInfo> queueInfo;
    private final MutableLiveData<MediaPlayerState.PlaybackError> playbackError;
    private final MutableLiveData<Boolean> isPlayerVisible;

    // Playback queue management
    private final PlaybackQueue playbackQueue;
    private final MediaPlayerState.CurrentPlaybackState currentState;

    // Real MediaPlaybackService integration
    private MediaPlaybackService mediaService;
    private boolean isServiceBound = false;
    private ServiceConnection serviceConnection;

    // Current artist information (since CurrentPlaybackState doesn't store it)
    private User currentArtist;
    
    public MediaPlayerRepository(Application application) {
        super(application);
        this.application = application;
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

        // Bind to MediaPlaybackService for real playback
        bindToMediaService();

        // Add a delayed check to ensure service binding worked
        mediaExecutor.execute(() -> {
            try {
                Thread.sleep(2000); // Wait 2 seconds for binding
                if (!isServiceBound || mediaService == null) {
                    android.util.Log.w("MediaPlayerRepository", "⚠️ Service binding check: Service not bound after 2 seconds");
                    android.util.Log.w("MediaPlayerRepository", "⚠️ Bound: " + isServiceBound + ", Service: " + (mediaService != null));
                    // Try binding again
                    bindToMediaService();
                } else {
                    android.util.Log.d("MediaPlayerRepository", "✅ Service binding check: Service successfully bound");
                }
            } catch (InterruptedException e) {
                android.util.Log.e("MediaPlayerRepository", "Service binding check interrupted", e);
            }
        });
    }

    /**
     * Bind to MediaPlaybackService for real audio playback
     */
    private void bindToMediaService() {
        android.util.Log.d("MediaPlayerRepository", "🔄 Starting MediaPlaybackService binding process...");

        Intent serviceIntent = new Intent(application, MediaPlaybackService.class);
        android.util.Log.d("MediaPlayerRepository", "🔄 Created Intent for: " + serviceIntent.getComponent());

        // Store ServiceConnection as field to prevent garbage collection
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                try {
                    android.util.Log.d("MediaPlayerRepository", "🔄 onServiceConnected called for: " + name);
                    MediaPlaybackService.MediaPlaybackBinder binder = (MediaPlaybackService.MediaPlaybackBinder) service;
                    mediaService = binder.getService();
                    isServiceBound = true;
                    android.util.Log.d("MediaPlayerRepository", "✅ MediaPlaybackService connected successfully!");
                    android.util.Log.d("MediaPlayerRepository", "✅ Service instance: " + (mediaService != null ? "Available" : "NULL"));

                    // 🎯 QUAN TRỌNG: Đăng ký listener để nhận trạng thái thật từ ExoPlayer
                    if (mediaService != null) {
                        mediaService.setPlaybackStateListener(MediaPlayerRepository.this);
                        android.util.Log.d("MediaPlayerRepository", "🔗 PlaybackStateListener registered - Two-way communication established!");
                    }
                } catch (Exception e) {
                    android.util.Log.e("MediaPlayerRepository", "❌ Error in onServiceConnected", e);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                android.util.Log.w("MediaPlayerRepository", "⚠️ onServiceDisconnected called for: " + name);
                mediaService = null;
                isServiceBound = false;
            }

            @Override
            public void onBindingDied(ComponentName name) {
                android.util.Log.e("MediaPlayerRepository", "💀 onBindingDied called for: " + name);
                mediaService = null;
                isServiceBound = false;
            }

            @Override
            public void onNullBinding(ComponentName name) {
                android.util.Log.e("MediaPlayerRepository", "❌ onNullBinding called for: " + name + " - Service returned null binder!");
                mediaService = null;
                isServiceBound = false;
            }
        };

        try {
            // Start service first to ensure it's running
            android.util.Log.d("MediaPlayerRepository", "🔄 Starting MediaPlaybackService...");
            ComponentName startResult = application.startService(serviceIntent);
            android.util.Log.d("MediaPlayerRepository", "🔄 Service start result: " + startResult);

            // Then bind to it
            android.util.Log.d("MediaPlayerRepository", "🔄 Attempting to bind to MediaPlaybackService...");
            boolean bindResult = application.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            android.util.Log.d("MediaPlayerRepository", "🔄 Service bind attempt result: " + bindResult);

            if (!bindResult) {
                android.util.Log.e("MediaPlayerRepository", "❌ bindService() returned false - binding failed immediately");
            }
        } catch (Exception e) {
            android.util.Log.e("MediaPlayerRepository", "❌ Exception during service binding", e);
        }
    }
    
    // ========== PLAYBACK CONTROL METHODS ==========
    
    /**
     * Play current song (always starts from beginning for queue navigation)
     */
    public Future<Boolean> play() {
        return mediaExecutor.submit(() -> playSync());
    }

    /**
     * Play current song synchronously (internal use to avoid deadlock)
     */
    private boolean playSync() {
        try {
            if (currentState.getCurrentSong() == null || mediaService == null) {
                return false;
            }

            Song currentSong = currentState.getCurrentSong();
            android.util.Log.d("MediaPlayerRepository", "Playing song: " + currentSong.getTitle());

            // Always play from beginning for queue navigation
            // Use artist from centralized state (fallback to old field for compatibility)
            User artist = currentState.getCurrentArtist() != null ? currentState.getCurrentArtist() : currentArtist;
            mediaService.playSong(currentSong, artist);

            currentState.setPlaybackState(MediaPlayerState.PlaybackState.PLAYING);
            currentPlaybackState.postValue(currentState);
            isPlayerVisible.postValue(true);

            return true;

        } catch (Exception e) {
            handlePlaybackError("Error starting playback", e);
            return false;
        }
    }
    
    /**
     * Pause playback
     */
    public Future<Boolean> pause() {
        return mediaExecutor.submit(() -> pauseSync());
    }

    /**
     * Pause playback synchronously (internal use to avoid deadlock)
     */
    private boolean pauseSync() {
        try {
            if (mediaService != null) {
                mediaService.pause();
            }

            // NOTE: State sẽ được cập nhật bởi onPlaybackStateChanged() callback
            // currentState.setPlaybackState(MediaPlayerState.PlaybackState.PAUSED);
            // currentPlaybackState.postValue(currentState);
            return true;
        } catch (Exception e) {
            handlePlaybackError("Error pausing playback", e);
            return false;
        }
    }

    /**
     * Resume playback synchronously (internal use to avoid deadlock)
     * KHÁC BIỆT với playSync(): resume() chỉ tiếp tục, playSync() restart từ đầu
     */
    private boolean resumeSync() {
        try {
            if (mediaService != null) {
                mediaService.resume(); // Gọi resume() thay vì playSong()
                android.util.Log.d("MediaPlayerRepository", "▶️ RESUME: Continue from current position");
            }

            // NOTE: State sẽ được cập nhật bởi onPlaybackStateChanged() callback
            // currentState.setPlaybackState(MediaPlayerState.PlaybackState.PLAYING);
            // currentPlaybackState.postValue(currentState);
            return true;
        } catch (Exception e) {
            handlePlaybackError("Error resuming playback", e);
            return false;
        }
    }
    
    /**
     * Stop playback
     */
    public Future<Boolean> stop() {
        return mediaExecutor.submit(() -> {
            try {
                if (mediaService != null) {
                    mediaService.stop(); // Use proper stop() method
                }

                currentState.setPlaybackState(MediaPlayerState.PlaybackState.STOPPED);
                currentState.setCurrentPosition(0);
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
        return mediaExecutor.submit(() -> seekToSync(positionMs));
    }

    /**
     * Seek to position synchronously (internal use to avoid deadlock)
     */
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
            handlePlaybackError("Error seeking", e);
            return false;
        }
    }
    
    /**
     * Toggle play/pause - FIXED: Use resume() instead of playSync() to avoid restart
     */
    public Future<Boolean> togglePlayPause() {
        return mediaExecutor.submit(() -> {
            try {
                if (currentState.isPlaying()) {
                    return pauseSync();
                } else {
                    return resumeSync(); // 🎯 FIXED: Resume thay vì restart
                }
            } catch (Exception e) {
                handlePlaybackError("Error toggling playback", e);
                return false;
            }
        });
    }
    
    // ========== QUEUE MANAGEMENT ==========

    /**
     * Set single song queue (fallback when no NavigationContext available)
     */
    public Future<Boolean> setSingleSongQueue(Song song) {
        return mediaExecutor.submit(() -> {
            try {
                android.util.Log.d("MediaPlayerRepository", "Setting single song queue: " + song.getTitle());

                // Create single-song list
                List<Song> singleSongList = new java.util.ArrayList<>();
                singleSongList.add(song);

                // Create simple NavigationContext
                NavigationContext singleContext = NavigationContext.fromGeneral(
                    "Single Song: " + song.getTitle(),
                    java.util.Arrays.asList(song.getId()),
                    0
                );

                // Set up queue
                playbackQueue.setQueue(singleSongList, singleContext);
                currentState.setCurrentSong(song);
                currentState.setCurrentQueueIndex(0);
                updateQueueInfo();

                android.util.Log.d("MediaPlayerRepository", "Single song queue setup complete");

                // Set player visibility for single song queue
                isPlayerVisible.postValue(true);

                return true;

            } catch (Exception e) {
                handlePlaybackError("Error setting single song queue", e);
                return false;
            }
        });
    }

    /**
     * Setup queue from context without starting playback (for when song is already playing)
     */
    public Future<Boolean> setupQueueFromContext(Song song, User artist, NavigationContext context) {
        return mediaExecutor.submit(() -> {
            try {
                android.util.Log.d("MediaPlayerRepository", "setupQueueFromContext called - Song: " +
                    song.getTitle() + ", Context: " + context.getType() + " (" + context.getContextTitle() + ")");

                // Load songs for the context - FIXED: Direct call to avoid deadlock
                List<Song> contextSongs = getContextSongsSync(context);

                android.util.Log.d("MediaPlayerRepository", "Loaded " + contextSongs.size() +
                    " songs from " + context.getType() + ": " + context.getContextTitle());

                // Set up queue
                playbackQueue.setQueue(contextSongs, context);

                // Jump to the specific song
                Song targetSong = playbackQueue.jumpToSong(song.getId());
                if (targetSong != null) {
                    currentState.setCurrentSong(targetSong);
                    currentState.setCurrentArtist(artist); // FIXED: Store artist in centralized state
                    currentArtist = artist; // Keep for backward compatibility
                    currentState.setCurrentQueueIndex(playbackQueue.getCurrentIndex());
                    updateQueueInfo();

                    android.util.Log.d("MediaPlayerRepository", "Queue setup complete (no playback start) - Position: " +
                        playbackQueue.getCurrentIndex() + "/" + contextSongs.size());

                    // IMPORTANT: Set player visibility even when not starting playback
                    // This ensures mini player shows when queue is setup for already playing song
                    isPlayerVisible.postValue(true);

                    return true;
                }

                android.util.Log.e("MediaPlayerRepository", "Failed to find target song in queue");
                return false;

            } catch (Exception e) {
                handlePlaybackError("Error setting up queue from context", e);
                return false;
            }
        });
    }

    /**
     * Setup queue from context synchronously (internal use to avoid deadlock)
     */
    private boolean setupQueueFromContextSync(Song song, User artist, NavigationContext context) {
        try {
            android.util.Log.d("MediaPlayerRepository", "setupQueueFromContextSync called - Song: " +
                song.getTitle() + ", Context: " + context.getType() + " (" + context.getContextTitle() + ")");

            // Load songs for the context - direct call to avoid deadlock
            List<Song> contextSongs = getContextSongsSync(context);

            android.util.Log.d("MediaPlayerRepository", "Loaded " + contextSongs.size() +
                " songs from " + context.getType() + ": " + context.getContextTitle());

            // Set up queue
            playbackQueue.setQueue(contextSongs, context);

            // Jump to the specific song
            Song targetSong = playbackQueue.jumpToSong(song.getId());
            if (targetSong != null) {
                currentState.setCurrentSong(targetSong);
                currentState.setCurrentArtist(artist); // FIXED: Store artist in centralized state
                currentArtist = artist; // Keep for backward compatibility
                currentState.setCurrentQueueIndex(playbackQueue.getCurrentIndex());
                updateQueueInfo();

                android.util.Log.d("MediaPlayerRepository", "Queue setup complete (sync) - Position: " +
                    playbackQueue.getCurrentIndex() + "/" + contextSongs.size());

                // IMPORTANT: Set player visibility even when not starting playback
                // This ensures mini player shows when queue is setup for already playing song
                isPlayerVisible.postValue(true);

                return true;
            }

            android.util.Log.e("MediaPlayerRepository", "Failed to find target song in queue");
            return false;

        } catch (Exception e) {
            handlePlaybackError("Error setting up queue from context (sync)", e);
            return false;
        }
    }

    /**
     * Play song with navigation context and artist information
     * FIXED: Removed deadlock by avoiding Future.get() calls
     */
    public Future<Boolean> playSongWithContext(Song song, User artist, NavigationContext context) {
        return mediaExecutor.submit(() -> {
            try {
                android.util.Log.d("MediaPlayerRepository", "playSongWithContext called - Song: " +
                    song.getTitle() + ", Context: " + context.getType() + " (" + context.getContextTitle() + ")");

                // Setup queue directly without blocking call
                boolean queueSetup = setupQueueFromContextSync(song, artist, context);
                if (!queueSetup) {
                    return false;
                }

                // Start playback directly without blocking call
                return playSync();

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
                if (playbackQueue.isEmpty()) {
                    return false;
                }

                // Check if we can go to next song
                if (!playbackQueue.hasNext() && playbackQueue.getRepeatMode() == MediaPlayerState.RepeatMode.OFF) {
                    // At last song with no repeat - pause instead of restarting
                    android.util.Log.d("MediaPlayerRepository", "At last song, no repeat - pausing playback");
                    return pauseSync(); // Pause instead of restart - FIXED: Direct call
                }

                Song nextSong = playbackQueue.getNextSong();
                if (nextSong != null) {
                    currentState.setCurrentSong(nextSong);
                    currentState.setCurrentQueueIndex(playbackQueue.getCurrentIndex());
                    currentState.setCurrentPosition(0);
                    updateQueueInfo();

                    return playSync(); // FIXED: Direct call to avoid deadlock
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
                if (playbackQueue.isEmpty()) {
                    return false;
                }

                // Get current position from service (with fallback to state)
                long currentPosition = 0;
                if (mediaService != null) {
                    currentPosition = mediaService.getCurrentPosition();
                } else {
                    // Fallback to state if service not connected
                    currentPosition = currentState.getCurrentPosition();
                }

                // If position > 3 seconds, restart current song
                if (currentPosition > 3000) {
                    android.util.Log.d("MediaPlayerRepository", "Position > 3s, restarting current song");
                    return seekToSync(0); // FIXED: Direct call to avoid deadlock
                }

                // Check if we can go to previous song
                if (!playbackQueue.hasPrevious() && playbackQueue.getRepeatMode() == MediaPlayerState.RepeatMode.OFF) {
                    // At first song with no repeat - restart current song
                    android.util.Log.d("MediaPlayerRepository", "At first song, no repeat - restarting current song");
                    return seekToSync(0); // FIXED: Direct call to avoid deadlock
                }

                // Go to previous song
                Song previousSong = playbackQueue.getPreviousSong();
                if (previousSong != null) {
                    currentState.setCurrentSong(previousSong);
                    currentState.setCurrentQueueIndex(playbackQueue.getCurrentIndex());
                    currentState.setCurrentPosition(0);
                    updateQueueInfo();

                    return playSync(); // FIXED: Direct call to avoid deadlock
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
     * Jump to song at specific position in queue
     */
    public Future<Boolean> jumpToSongInQueue(int position) {
        return mediaExecutor.submit(() -> {
            try {
                Song targetSong = playbackQueue.jumpToSong(position);
                if (targetSong != null) {
                    currentState.setCurrentSong(targetSong);
                    currentState.setCurrentQueueIndex(playbackQueue.getCurrentIndex());
                    updateQueueInfo();

                    // Start playback of the selected song
                    if (isServiceBound && mediaService != null) {
                        User currentArtist = currentState.getCurrentArtist();
                        mediaService.playSong(targetSong, currentArtist);
                    }

                    android.util.Log.d("MediaPlayerRepository", "Jumped to position " + position + " - Song: " + targetSong.getTitle());
                    return true;
                }
                return false;
            } catch (Exception e) {
                handlePlaybackError("Error jumping to song in queue", e);
                return false;
            }
        });
    }

    /**
     * Move song in queue from one position to another
     */
    public Future<Boolean> moveSongInQueue(int fromPosition, int toPosition) {
        return mediaExecutor.submit(() -> {
            try {
                playbackQueue.moveSong(fromPosition, toPosition);
                currentState.setCurrentQueueIndex(playbackQueue.getCurrentIndex());
                updateQueueInfo();

                android.util.Log.d("MediaPlayerRepository", "Moved song from " + fromPosition + " to " + toPosition);
                return true;
            } catch (Exception e) {
                handlePlaybackError("Error moving song in queue", e);
                return false;
            }
        });
    }

    /**
     * Get current queue as list of songs
     */
    public List<Song> getCurrentQueue() {
        return playbackQueue.getCurrentQueue();
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
     * Toggle song like (delegated to parent) - FIXED: Use separate executor to avoid deadlock
     */
    public Future<Boolean> toggleSongLike(long songId, long userId) {
        // Use parent's executor to avoid deadlock
        return super.toggleSongLike(songId, userId);
    }

    /**
     * Add comment (delegated to parent) - FIXED: Use separate executor to avoid deadlock
     */
    public Future<Long> addComment(long songId, long userId, String content) {
        // Use parent's executor to avoid deadlock
        return super.addComment(songId, userId, content);
    }

    // ========== HELPER METHODS ==========

    /**
     * Get song by ID synchronously (for internal use) - FIXED: Use separate executor to avoid deadlock
     */
    public Future<Song> getSongByIdSync(long songId) {
        // Use parent's executor to avoid deadlock
        return super.getSongByIdSync(songId);
    }

    /**
     * Get songs for navigation context
     * Cải thiện để lấy songs từ các nguồn khác nhau thay vì chỉ dựa vào songIds có sẵn
     */
    private Future<List<Song>> getContextSongs(NavigationContext context) {
        return mediaExecutor.submit(() -> {
            try {
                List<Song> songs = new java.util.ArrayList<>();

                // Xử lý theo từng loại NavigationContext
                switch (context.getType()) {
                    case FROM_PLAYLIST:
                        // Lấy tất cả songs trong playlist
                        if (context.getContextId() != null) {
                            songs = playlistRepository.getSongsInPlaylistDirectly(context.getContextId());
                            android.util.Log.d("MediaPlayerRepository", "Loaded " + songs.size() +
                                " songs from playlist: " + context.getContextTitle());
                        }
                        break;

                    case FROM_ARTIST:
                        // Lấy tất cả public songs của artist/user
                        if (context.getContextId() != null) {
                            songs = songRepository.getPublicSongsByUploaderDirectly(context.getContextId());
                            android.util.Log.d("MediaPlayerRepository", "Loaded " + songs.size() +
                                " songs from artist: " + context.getContextTitle());
                        }
                        break;

                    case FROM_SEARCH:
                        // Lấy songs từ search results dựa vào songIds có sẵn
                        List<Long> searchSongIds = context.getSongIds();
                        if (searchSongIds != null && !searchSongIds.isEmpty()) {
                            for (Long songId : searchSongIds) {
                                Song song = getSongByIdDirectly(songId);
                                if (song != null) {
                                    songs.add(song);
                                }
                            }
                            android.util.Log.d("MediaPlayerRepository", "Loaded " + songs.size() +
                                " songs from search: " + context.getContextTitle());
                        }
                        break;

                    case FROM_GENERAL:
                    default:
                        // Fallback: sử dụng songIds có sẵn (logic cũ)
                        List<Long> songIds = context.getSongIds();
                        if (songIds != null && !songIds.isEmpty()) {
                            for (Long songId : songIds) {
                                Song song = getSongByIdDirectly(songId);
                                if (song != null) {
                                    songs.add(song);
                                }
                            }
                            android.util.Log.d("MediaPlayerRepository", "Loaded " + songs.size() +
                                " songs from general context: " + context.getContextTitle());
                        }
                        break;
                }

                return songs;

            } catch (Exception e) {
                android.util.Log.e("MediaPlayerRepository", "Error getting context songs for " +
                    context.getType() + ": " + context.getContextTitle(), e);
                return new java.util.ArrayList<>();
            }
        });
    }

    /**
     * Get songs for navigation context synchronously (internal use to avoid deadlock)
     */
    private List<Song> getContextSongsSync(NavigationContext context) {
        try {
            List<Song> songs = new java.util.ArrayList<>();

            // Xử lý theo từng loại NavigationContext
            switch (context.getType()) {
                case FROM_PLAYLIST:
                    // Lấy tất cả songs trong playlist - direct call to avoid deadlock
                    if (context.getContextId() != null) {
                        songs = getSongsInPlaylistDirectly(context.getContextId());
                        android.util.Log.d("MediaPlayerRepository", "Loaded " + songs.size() +
                            " songs from playlist: " + context.getContextTitle());
                    }
                    break;

                case FROM_ARTIST:
                    // Lấy tất cả public songs của artist/user - direct call to avoid deadlock
                    if (context.getContextId() != null) {
                        songs = getPublicSongsByUploaderDirectly(context.getContextId());
                        android.util.Log.d("MediaPlayerRepository", "Loaded " + songs.size() +
                            " songs from artist: " + context.getContextTitle());
                    }
                    break;

                case FROM_SEARCH:
                    // Lấy songs từ search results dựa vào songIds có sẵn
                    List<Long> searchSongIds = context.getSongIds();
                    if (searchSongIds != null && !searchSongIds.isEmpty()) {
                        for (Long songId : searchSongIds) {
                            Song song = getSongByIdDirectly(songId);
                            if (song != null) {
                                songs.add(song);
                            }
                        }
                        android.util.Log.d("MediaPlayerRepository", "Loaded " + songs.size() +
                            " songs from search: " + context.getContextTitle());
                    }
                    break;

                case FROM_GENERAL:
                default:
                    // Fallback: sử dụng songIds có sẵn (logic cũ)
                    List<Long> songIds = context.getSongIds();
                    if (songIds != null && !songIds.isEmpty()) {
                        for (Long songId : songIds) {
                            Song song = getSongByIdDirectly(songId);
                            if (song != null) {
                                songs.add(song);
                            }
                        }
                        android.util.Log.d("MediaPlayerRepository", "Loaded " + songs.size() +
                            " songs from general context: " + context.getContextTitle());
                    }
                    break;
            }

            return songs;

        } catch (Exception e) {
            android.util.Log.e("MediaPlayerRepository", "Error getting context songs for " +
                context.getType() + ": " + context.getContextTitle(), e);
            return new java.util.ArrayList<>();
        }
    }

    // ========== DIRECT DATABASE ACCESS METHODS (TO AVOID DEADLOCK) ==========

    /**
     * Get song by ID directly from database (no ExecutorService)
     */
    private Song getSongByIdDirectly(long songId) {
        try {
            return songRepository.getSongByIdDirectly(songId);
        } catch (Exception e) {
            android.util.Log.e("MediaPlayerRepository", "Error getting song by ID directly", e);
            return null;
        }
    }

    /**
     * Get songs in playlist directly from database (no ExecutorService)
     */
    private List<Song> getSongsInPlaylistDirectly(long playlistId) {
        try {
            return playlistRepository.getSongsInPlaylistDirectly(playlistId);
        } catch (Exception e) {
            android.util.Log.e("MediaPlayerRepository", "Error getting playlist songs directly", e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Get public songs by uploader directly from database (no ExecutorService)
     */
    private List<Song> getPublicSongsByUploaderDirectly(long uploaderId) {
        try {
            return songRepository.getPublicSongsByUploaderDirectly(uploaderId);
        } catch (Exception e) {
            android.util.Log.e("MediaPlayerRepository", "Error getting uploader songs directly", e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Check service status for debugging
     */
    public void checkServiceStatus() {
        android.util.Log.d("MediaPlayerRepository", "=== SERVICE STATUS DEBUG ===");
        android.util.Log.d("MediaPlayerRepository", "MediaService bound: " + (mediaService != null));
        android.util.Log.d("MediaPlayerRepository", "Current song: " +
            (currentState.getCurrentSong() != null ? currentState.getCurrentSong().getTitle() : "NULL"));
        android.util.Log.d("MediaPlayerRepository", "Current artist: " +
            (currentState.getCurrentArtist() != null ? currentState.getCurrentArtist().getDisplayName() : "NULL"));
        android.util.Log.d("MediaPlayerRepository", "Is playing: " + currentState.isPlaying());
        android.util.Log.d("MediaPlayerRepository", "Player visible: " +
            (isPlayerVisible.getValue() != null ? isPlayerVisible.getValue() : "NULL"));
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

    /**
     * Get current artist information
     */
    public User getCurrentArtist() {
        return currentArtist;
    }

    // REMOVED: Duplicate checkServiceStatus() method - using the enhanced version above

    /**
     * Force sync current state and ensure mini player visibility
     */
    public void ensurePlayerVisibility() {
        if (mediaService != null) {
            Song currentSong = mediaService.getCurrentSong();
            if (currentSong != null) {
                android.util.Log.d("MediaPlayerRepository", "Ensuring player visibility - Current song: " + currentSong.getTitle());
                isPlayerVisible.postValue(true);

                // Update current state
                currentState.setCurrentSong(currentSong);
                currentState.setPlaybackState(mediaService.isPlaying() ?
                    MediaPlayerState.PlaybackState.PLAYING : MediaPlayerState.PlaybackState.PAUSED);
                currentPlaybackState.postValue(currentState);
            }
        }
    }
    
    /**
     * Cleanup method to unbind from MediaPlaybackService
     */
    public void cleanup() {
        if (isServiceBound && serviceConnection != null) {
            try {
                application.unbindService(serviceConnection);
                android.util.Log.d("MediaPlayerRepository", "✅ Properly unbound from MediaPlaybackService");
            } catch (Exception e) {
                android.util.Log.e("MediaPlayerRepository", "❌ Error unbinding service", e);
            }
        }

        mediaService = null;
        isServiceBound = false;
        serviceConnection = null;
    }

    // ========== PLAYBACK STATE LISTENER IMPLEMENTATION ==========
    // Nhận trạng thái thực tế từ ExoPlayer thông qua MediaPlaybackService

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        android.util.Log.d("MediaPlayerRepository", "🎵 REAL STATE from ExoPlayer: isPlaying = " + isPlaying);

        // Cập nhật centralized state với trạng thái THẬT từ ExoPlayer
        currentState.setPlaybackState(isPlaying ?
            MediaPlayerState.PlaybackState.PLAYING :
            MediaPlayerState.PlaybackState.PAUSED);

        // Notify UI với trạng thái thật
        currentPlaybackState.postValue(currentState);
    }

    @Override
    public void onPositionChanged(long currentPosition, long duration) {
        // Cập nhật position và duration với dữ liệu THẬT từ ExoPlayer
        currentState.setCurrentPosition(currentPosition);
        currentState.setDuration(duration);

        // Notify UI với progress thật
        currentPlaybackState.postValue(currentState);

        // Log để debug (có thể tắt sau)
        if (currentPosition % 5000 < 1000) { // Log mỗi 5 giây
            android.util.Log.d("MediaPlayerRepository", "📍 REAL PROGRESS: " +
                currentPosition + "/" + duration + "ms");
        }
    }

    @Override
    public void onSongChanged(Song song, User artist) {
        android.util.Log.d("MediaPlayerRepository", "🎵 REAL SONG CHANGE: " +
            (song != null ? song.getTitle() : "NULL") + " by " +
            (artist != null ? artist.getDisplayName() : "NULL"));

        // Cập nhật centralized state với song và artist thật
        currentState.setCurrentSong(song);
        currentState.setCurrentArtist(artist);
        currentArtist = artist; // Backward compatibility

        // Notify UI
        currentPlaybackState.postValue(currentState);

        // Ensure player visibility when song changes
        isPlayerVisible.postValue(true);
    }

    @Override
    public void onSongCompleted() {
        android.util.Log.d("MediaPlayerRepository", "🏁 SONG COMPLETED - Auto next");

        // Tự động chuyển bài tiếp theo
        mediaExecutor.execute(() -> {
            try {
                playNext().get();
            } catch (Exception e) {
                android.util.Log.e("MediaPlayerRepository", "Error auto-playing next song", e);
            }
        });
    }

    @Override
    public void onPlayerError(String error) {
        android.util.Log.e("MediaPlayerRepository", "🚨 PLAYER ERROR: " + error);

        // Set error state
        currentState.setPlaybackState(MediaPlayerState.PlaybackState.ERROR);
        currentPlaybackState.postValue(currentState);

        // Could notify UI about error
        handlePlaybackError("ExoPlayer error", new Exception(error));
    }

    @Override
    public void shutdown() {
        cleanup();
        super.shutdown();
        if (mediaExecutor != null) {
            mediaExecutor.shutdown();
        }
    }
}
