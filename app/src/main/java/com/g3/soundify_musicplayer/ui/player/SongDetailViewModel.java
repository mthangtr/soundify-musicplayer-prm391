package com.g3.soundify_musicplayer.ui.player;

import android.app.Application;
// REMOVED: ServiceConnection imports - không cần thiết nữa
import android.os.Handler;
// REMOVED: IBinder import - không cần thiết nữa
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.g3.soundify_musicplayer.data.entity.Comment;
import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.model.MediaPlayerState;
import com.g3.soundify_musicplayer.data.model.NavigationContext;
import com.g3.soundify_musicplayer.data.repository.MediaPlayerRepository;
import com.g3.soundify_musicplayer.data.repository.MusicPlayerRepository;
import com.g3.soundify_musicplayer.data.repository.SongDetailRepository;
// REMOVED: MediaPlaybackService import - không tương tác trực tiếp nữa
import com.g3.soundify_musicplayer.utils.AuthManager;
import com.g3.soundify_musicplayer.utils.RepositoryManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel THỐNG NHẤT cho cả MiniPlayer và FullPlayer
 * Kết hợp SongDetailRepository (song data) + MediaPlaybackService (playback)
 */
public class SongDetailViewModel extends AndroidViewModel {

    private final SongDetailRepository repository;
    private final MediaPlayerRepository mediaPlayerRepository;
    private final AuthManager authManager;
    private final ExecutorService executor;

    // REMOVED: MediaPlaybackService integration - chỉ MediaPlayerRepository được phép bind service
    // Tất cả tương tác với service sẽ đi qua MediaPlayerRepository
    
    // LiveData cho UI
    private final MutableLiveData<Song> currentSong = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLiked = new MutableLiveData<>();
    private final MutableLiveData<Integer> likeCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> commentCount = new MutableLiveData<>();
    private final MutableLiveData<List<Playlist>> userPlaylists = new MutableLiveData<>();
    private final MutableLiveData<List<Long>> playlistsContainingSong = new MutableLiveData<>();
    private final MutableLiveData<List<Song>> relatedSongs = new MutableLiveData<>();
    private final MutableLiveData<List<Song>> moreSongsByArtist = new MutableLiveData<>();
    
    // Status LiveData
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Playback LiveData
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    // Note: isVisible is now managed by MediaPlayerRepository, accessed via getIsVisible()
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private final MutableLiveData<Long> currentPosition = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> duration = new MutableLiveData<>(0L);
    private final MutableLiveData<Boolean> isFollowing = new MutableLiveData<>(false);

    // Navigation context
    private NavigationContext currentNavigationContext;

    // Handler để update progress
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    
    // REFACTORED: Constructor now accepts Repository instances (Singleton pattern)
    public SongDetailViewModel(@NonNull Application application,
                              SongDetailRepository repository,
                              MediaPlayerRepository mediaPlayerRepository) {
        super(application);
        this.repository = repository;
        this.mediaPlayerRepository = mediaPlayerRepository;
        this.authManager = new AuthManager(application);
        this.executor = Executors.newFixedThreadPool(2);

        // Initialize values
        isLoading.setValue(false);
        isLiked.setValue(false);
        likeCount.setValue(0);
        commentCount.setValue(0);

        // REMOVED: Repository creation - now injected as Singleton instances
        // Service binding sẽ được xử lý bởi MediaPlayerRepository singleton

        // Setup observers để sync state từ MediaPlayerRepository
        setupMediaPlayerObservers();
    }

    // DEPRECATED: Keep old constructor for backward compatibility (will be removed later)
    @Deprecated
    public SongDetailViewModel(@NonNull Application application) {
        this(application,
             RepositoryManager.getInstance(application).getSongDetailRepository(),
             RepositoryManager.getInstance(application).getMediaPlayerRepository());
    }
    
    // ========== PUBLIC METHODS ==========
    
    /**
     * Load song detail data
     */
    public void loadSongDetail(long songId, long userId) {
        isLoading.postValue(true); // FIXED: Use postValue() to avoid IllegalStateException
        
        executor.execute(() -> {
            try {
                // Get comprehensive song detail data
                SongDetailRepository.SongDetailData data = repository.getSongDetailData(songId, userId).get();
                
                if (data != null) {
                    // Update UI data
                    currentSong.postValue(data.song);
                    isLiked.postValue(data.isLiked);
                    likeCount.postValue(data.likeCount);
                    commentCount.postValue(data.commentCount);
                    playlistsContainingSong.postValue(data.playlistIds);
                    
                    // Load related content
                    loadRelatedSongs(data.song.getGenre(), songId);
                    loadMoreSongsByArtist(data.song.getUploaderId(), songId);
                    loadUserPlaylists(userId);
                    
                } else {
                    errorMessage.postValue("Không thể tải thông tin bài hát");
                }
                
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi tải dữ liệu: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }
    
    /**
     * Toggle song like status
     */
    public void toggleLike(long songId, long userId) {
        android.util.Log.d("SongDetailViewModel", "🔄 toggleLike(songId: " + songId + ", userId: " + userId + ") starting...");
        
        executor.execute(() -> {
            try {
                android.util.Log.d("SongDetailViewModel", "🔄 Calling repository.toggleSongLike...");
                Boolean newLikeStatus = repository.toggleSongLike(songId, userId).get();
                android.util.Log.d("SongDetailViewModel", "✅ toggleSongLike result: " + newLikeStatus);
                
                isLiked.postValue(newLikeStatus);
                
                // Update like count
                android.util.Log.d("SongDetailViewModel", "🔄 Getting updated like info...");
                MusicPlayerRepository.SongLikeInfo likeInfo = repository.getSongLikeInfo(songId, userId).get();
                android.util.Log.d("SongDetailViewModel", "✅ Like info - isLiked: " + likeInfo.isLiked + ", count: " + likeInfo.likeCount);
                
                likeCount.postValue(likeInfo.likeCount);
                
            } catch (Exception e) {
                android.util.Log.e("SongDetailViewModel", "❌ Error in toggleLike", e);
                errorMessage.postValue("Lỗi khi thực hiện like: " + e.getMessage());
            }
        });
    }
    
    /**
     * Add comment to song
     */
    @SuppressWarnings("unused") // Method có thể được sử dụng trong tương lai
    public void addComment(long songId, long userId, String content) {
        if (content == null || content.trim().isEmpty()) {
            errorMessage.postValue("Nội dung comment không được để trống"); // FIXED: Use postValue()
            return;
        }

        executor.execute(() -> {
            try {
                Long commentId = repository.addComment(songId, userId, content.trim()).get();
                if (commentId != null && commentId > 0) {
                    // Update comment count
                    refreshCommentCount(songId);
                } else {
                    errorMessage.postValue("Không thể thêm comment");
                }

            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi thêm comment: " + e.getMessage());
            }
        });
    }

    /**
     * Refresh comment count for a song (useful when comments are added/deleted from other screens)
     */
    public void refreshCommentCount(long songId) {
        executor.execute(() -> {
            try {
                Integer newCount = repository.getCommentCountBySong(songId).get();
                commentCount.postValue(newCount);
            } catch (Exception e) {
                android.util.Log.e("SongDetailViewModel", "Error refreshing comment count", e);
            }
        });
    }
    
    /**
     * Add song to selected playlists
     */
    public void addSongToPlaylists(long songId, List<Long> selectedPlaylistIds) {
        if (selectedPlaylistIds == null || selectedPlaylistIds.isEmpty()) {
            errorMessage.postValue("Vui lòng chọn ít nhất một playlist"); // FIXED: Use postValue()
            return;
        }
        
        executor.execute(() -> {
            try {
                repository.addSongToMultiplePlaylists(songId, selectedPlaylistIds).get();
                
                // Update playlists containing song
                List<Long> updatedPlaylistIds = repository.getPlaylistIdsContainingSong(songId, getCurrentUserId()).get();
                playlistsContainingSong.postValue(updatedPlaylistIds);
                
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi thêm vào playlist: " + e.getMessage());
            }
        });
    }
    
    /**
     * Create new playlist and add song to it
     */
    @SuppressWarnings("unused") // Method có thể được sử dụng trong tương lai
    public void createPlaylistWithSong(String playlistName, String description, boolean isPublic, long ownerId, long songId) {
        if (playlistName == null || playlistName.trim().isEmpty()) {
            errorMessage.postValue("Tên playlist không được để trống"); // FIXED: Use postValue()
            return;
        }
        
        executor.execute(() -> {
            try {
                Long playlistId = repository.createPlaylistWithSong(
                    playlistName.trim(), 
                    description != null ? description.trim() : "", 
                    isPublic, 
                    ownerId, 
                    songId
                ).get();
                
                if (playlistId != null && playlistId > 0) {
                    // Reload user playlists and playlists containing song
                    loadUserPlaylists(ownerId);
                    List<Long> updatedPlaylistIds = repository.getPlaylistIdsContainingSong(songId, ownerId).get();
                    playlistsContainingSong.postValue(updatedPlaylistIds);
                } else {
                    errorMessage.postValue("Không thể tạo playlist");
                }
                
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi tạo playlist: " + e.getMessage());
            }
        });
    }
    
    // ========== PRIVATE HELPER METHODS ==========
    
    private void loadRelatedSongs(String genre, long excludeSongId) {
        executor.execute(() -> {
            try {
                List<Song> songs = repository.getRelatedSongsByGenre(genre, excludeSongId, 10).get();
                relatedSongs.postValue(songs);
            } catch (Exception e) {
                // Silent fail for related songs
            }
        });
    }
    
    private void loadMoreSongsByArtist(long uploaderId, long excludeSongId) {
        executor.execute(() -> {
            try {
                List<Song> songs = repository.getMoreSongsByUploader(uploaderId, excludeSongId, 10).get();
                moreSongsByArtist.postValue(songs);
            } catch (Exception e) {
                // Silent fail for more songs
            }
        });
    }
    
    private void loadUserPlaylists(long userId) {
        executor.execute(() -> {
            try {
                List<Playlist> playlists = repository.getUserPlaylistsForAddSong(userId).get();
                userPlaylists.postValue(playlists);
            } catch (Exception e) {
                // Silent fail for playlists
            }
        });
    }
    
    private long getCurrentUserId() {
        if (authManager != null) {
            long userId = authManager.getCurrentUserId();
            android.util.Log.d("SongDetailViewModel", "🔍 getCurrentUserId(): " + userId);
            return userId;
        }
        android.util.Log.w("SongDetailViewModel", "⚠️ AuthManager is null, using fallback userId: 1");
        return 1L; // Fallback user ID
    }
    
    // ========== GETTERS FOR LIVEDATA ==========
    
    public LiveData<Song> getCurrentSong() { return currentSong; }
    public LiveData<Boolean> getIsLiked() { return isLiked; }
    @SuppressWarnings("unused") public LiveData<Integer> getLikeCount() { return likeCount; }
    @SuppressWarnings("unused") public LiveData<Integer> getCommentCount() { return commentCount; }
    @SuppressWarnings("unused") public LiveData<List<Playlist>> getUserPlaylists() { return userPlaylists; }
    @SuppressWarnings("unused") public LiveData<List<Long>> getPlaylistsContainingSong() { return playlistsContainingSong; }
    @SuppressWarnings("unused") public LiveData<List<Song>> getRelatedSongs() { return relatedSongs; }
    @SuppressWarnings("unused") public LiveData<List<Song>> getMoreSongsByArtist() { return moreSongsByArtist; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    
    // Get LiveData from repository
    public LiveData<List<Comment>> getCommentsBySong(long songId) {
        return repository.getCommentsBySong(songId);
    }
    
    @SuppressWarnings("unused") // Method có thể được sử dụng trong tương lai
    public LiveData<List<User>> getUsersWhoLikedSong(long songId) {
        return repository.getUsersWhoLikedSong(songId);
    }

    // Get LiveData from MediaPlayerRepository
    public LiveData<MediaPlayerState.CurrentPlaybackState> getCurrentPlaybackState() {
        return mediaPlayerRepository.getCurrentPlaybackState();
    }

    public LiveData<Boolean> getIsPlayerVisible() {
        return mediaPlayerRepository.getIsPlayerVisible();
    }

    public LiveData<MediaPlayerState.QueueInfo> getQueueInfo() {
        return mediaPlayerRepository.getQueueInfo();
    }

    /**
     * Get current queue as list of songs for Queue screen
     */
    public List<Song> getCurrentQueue() {
        return mediaPlayerRepository.getCurrentQueue();
    }

    /**
     * Get current queue as LiveData for Queue screen (only updates when queue changes)
     */
    public LiveData<List<Song>> getCurrentQueueLiveData() {
        // Transform QueueInfo to extract queue list - only updates when queue actually changes
        return Transformations.map(mediaPlayerRepository.getQueueInfo(),
            queueInfo -> queueInfo != null ? mediaPlayerRepository.getCurrentQueue() : new ArrayList<>());
    }

    /**
     * Get current queue index for Queue screen
     */
    public LiveData<Integer> getCurrentQueueIndex() {
        // Transform CurrentPlaybackState to extract queue index
        return Transformations.map(mediaPlayerRepository.getCurrentPlaybackState(),
            state -> state != null ? state.getCurrentQueueIndex() : -1);
    }

    /**
     * Get current artist directly (for Queue screen)
     */
    public User getCurrentArtistDirect() {
        MediaPlayerState.CurrentPlaybackState currentState = mediaPlayerRepository.getCurrentPlaybackState().getValue();
        return currentState != null ? currentState.getCurrentArtist() : null;
    }

    /**
     * Play song at specific index in queue
     */
    public void playSongAtIndex(int position) {
        executor.execute(() -> {
            try {
                boolean success = mediaPlayerRepository.jumpToSongInQueue(position).get();
                if (!success) {
                    errorMessage.postValue("Không thể phát bài hát tại vị trí " + position);
                }
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi phát bài hát: " + e.getMessage());
                android.util.Log.e("SongDetailViewModel", "Error playing song at index " + position, e);
            }
        });
    }

    /**
     * Move song in queue from one position to another
     */
    public void moveSongInQueue(int fromPosition, int toPosition) {
        executor.execute(() -> {
            try {
                boolean success = mediaPlayerRepository.moveSongInQueue(fromPosition, toPosition).get();
                if (!success) {
                    errorMessage.postValue("Không thể di chuyển bài hát trong queue");
                }
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi di chuyển bài hát: " + e.getMessage());
                android.util.Log.e("SongDetailViewModel", "Error moving song in queue", e);
            }
        });
    }

    // ========== MEDIA PLAYBACK METHODS ==========

    // REMOVED: bindToService method - chỉ MediaPlayerRepository được phép bind service

    // REMOVED: ServiceConnection - chỉ MediaPlayerRepository được phép bind service
    // Tất cả callback từ service sẽ được xử lý thông qua MediaPlayerRepository
    
    /**
     * Phát bài hát mới
     */
    public void playSong(Song song, User artist) {
        playSong(song, artist, null);
    }

    /**
     * Phát bài hát mới với navigation context
     */
    public void playSong(Song song, User artist, NavigationContext context) {
        playSongForced(song, artist, context);
    }

    /**
     * Phát bài hát với NavigationContext - TẠO QUEUE từ context
     * Method mới để tận dụng MediaPlayerRepository.playSongWithContext()
     */
    public void playSongWithContext(Song song, User artist, NavigationContext context) {
        if (song != null && context != null) {
            currentNavigationContext = context;

            // Sử dụng MediaPlayerRepository để tạo queue và phát nhạc
            executor.execute(() -> {
                try {
                    // Check service status before attempting playback
                    mediaPlayerRepository.checkServiceStatus();

                    boolean success = mediaPlayerRepository.playSongWithContext(song, artist, context).get();
                    if (success) {
                        // Update UI state
                        currentSong.postValue(song);
                        setCurrentArtist(artist);
                        // Note: Mini player visibility is managed by MediaPlayerRepository

                        android.util.Log.d("SongDetailViewModel", "✅ playSongWithContext SUCCESS - Song: " +
                            song.getTitle() + ", Context: " + context.getType() +
                            " (" + context.getContextTitle() + ")");
                    } else {
                        errorMessage.postValue("Không thể phát bài hát với context");
                        android.util.Log.e("SongDetailViewModel", "playSongWithContext FAILED");
                    }
                } catch (Exception e) {
                    errorMessage.postValue("Lỗi khi phát bài hát: " + e.getMessage());
                    android.util.Log.e("SongDetailViewModel", "Error in playSongWithContext", e);
                }
            });
        } else {
            errorMessage.postValue("Thông tin bài hát hoặc context không hợp lệ");
        }
    }

    /**
     * Phát bài hát - LUÔN restart từ đầu (dù cùng bài hay khác bài)
     * Đây là method chính được gọi từ UI khi user click vào bài hát (KHÔNG có queue)
     */
    public void playSongForced(Song song, User artist, NavigationContext context) {
        if (song != null) {
            currentNavigationContext = context;

            // Sử dụng MediaPlayerRepository thay vì trực tiếp với service
            executor.execute(() -> {
                try {
                    boolean success = mediaPlayerRepository.playSongWithContext(song, artist, context).get();
                    if (success) {
                        currentSong.postValue(song);
                        setCurrentArtist(artist);
                        android.util.Log.d("SongDetailViewModel", "playSongForced SUCCESS - Song: " + song.getTitle() +
                            ", Artist: " + (artist != null ? artist.getDisplayName() : "NULL"));
                    } else {
                        errorMessage.postValue("Không thể phát bài hát");
                    }
                } catch (Exception e) {
                    errorMessage.postValue("Lỗi khi phát bài hát: " + e.getMessage());
                }
            });
        } else {
            errorMessage.postValue("Bài hát không hợp lệ");
        }
    }

    /**
     * Toggle play/pause
     */
    public void togglePlayPause() {
        executor.execute(() -> {
            try {
                mediaPlayerRepository.togglePlayPause().get();
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi toggle play/pause: " + e.getMessage());
            }
        });
    }

    /**
     * Seek đến vị trí cụ thể (percentage 0-100)
     */
    public void seekToPercentage(int percentage) {
        executor.execute(() -> {
            try {
                // Lấy duration từ MediaPlayerRepository state
                MediaPlayerState.CurrentPlaybackState state = mediaPlayerRepository.getCurrentPlaybackState().getValue();
                if (state != null && state.getDuration() > 0 && percentage >= 0 && percentage <= 100) {
                    long targetPosition = (state.getDuration() * percentage) / 100;
                    mediaPlayerRepository.seekTo(targetPosition).get();
                }
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi seek: " + e.getMessage());
            }
        });
    }

    /**
     * Toggle follow status
     */
    public void toggleFollow() {
        Boolean currentFollowing = isFollowing.getValue();
        boolean newFollowing = currentFollowing == null || !currentFollowing;
        isFollowing.postValue(newFollowing);
    }

    /**
     * Setup observers để sync state từ MediaPlayerRepository
     */
    private void setupMediaPlayerObservers() {
        // Observer cho playback state changes
        mediaPlayerRepository.getCurrentPlaybackState().observeForever(state -> {
            if (state != null) {
                // Update song info
                if (state.getCurrentSong() != null) {
                    currentSong.postValue(state.getCurrentSong());
                }

                // FIXED: Update artist info from centralized state
                if (state.getCurrentArtist() != null) {
                    setCurrentArtist(state.getCurrentArtist());
                }

                // Update playback state
                isPlaying.postValue(state.isPlaying());
                currentPosition.postValue(state.getCurrentPosition());
                duration.postValue(state.getDuration());

                // Update progress
                if (state.getDuration() > 0) {
                    int progressPercent = (int) ((state.getCurrentPosition() * 100) / state.getDuration());
                    progress.postValue(progressPercent);
                }

                // Start/stop progress updates based on playing state
                if (state.isPlaying()) {
                    startProgressUpdates();
                } else {
                    stopProgressUpdates();
                }
            }
        });
    }

    /**
     * Progress updates - sử dụng MediaPlayerRepository state thay vì trực tiếp từ service
     */
    private void startProgressUpdates() {
        stopProgressUpdates(); // Đảm bảo cleanup trước khi tạo mới

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                // Lấy state từ MediaPlayerRepository thay vì trực tiếp từ service
                MediaPlayerState.CurrentPlaybackState state = mediaPlayerRepository.getCurrentPlaybackState().getValue();
                if (state != null && state.isPlaying()) {
                    try {
                        long currentPos = state.getCurrentPosition();
                        long dur = state.getDuration();
                        currentPosition.postValue(currentPos);
                        duration.postValue(dur);
                        if (dur > 0) {
                            int progressPercent = (int) ((currentPos * 100) / dur);
                            progress.postValue(progressPercent);
                        }
                        // Chỉ schedule next update nếu vẫn đang phát
                        if (state.isPlaying()) {
                            progressHandler.postDelayed(this, 500);
                        }
                    } catch (Exception e) {
                        android.util.Log.w("SongDetailViewModel", "Error updating progress: " + e.getMessage());
                        stopProgressUpdates(); // Stop nếu có lỗi để tránh leak
                    }
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdates() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }

    /**
     * Play next song in queue
     */
    public void playNext() {
        executor.execute(() -> {
            try {
                // Always try to navigate - MediaPlayerRepository will handle boundaries
                boolean success = mediaPlayerRepository.playNext().get();
                // success=true means either navigated or restarted current song (both are valid)
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi chuyển bài tiếp theo: " + e.getMessage());
            }
        });
    }

    /**
     * Play previous song in queue
     */
    public void playPrevious() {
        executor.execute(() -> {
            try {
                // Always try to navigate - MediaPlayerRepository will handle boundaries and 3-second logic
                mediaPlayerRepository.playPrevious().get();
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi chuyển bài trước đó: " + e.getMessage());
            }
        });
    }

    /**
     * Ensure queue is setup from NavigationContext when FullPlayer opens
     */
    public void ensureQueueFromContext(long songId, NavigationContext navigationContext) {
        android.util.Log.d("SongDetailViewModel", "ensureQueueFromContext - Song ID: " + songId +
            ", Context: " + navigationContext.getType() + " - " + navigationContext.getContextTitle());

        executor.execute(() -> {
            try {
                // Check if queue is already setup for this context
                MediaPlayerState.QueueInfo currentQueueInfo = mediaPlayerRepository.getQueueInfo().getValue();

                if (currentQueueInfo != null && currentQueueInfo.getTotalSongs() > 0) {
                    android.util.Log.d("SongDetailViewModel", "Queue already exists: " +
                        currentQueueInfo.getQueueTitle() + " (" + currentQueueInfo.getCurrentIndex() + "/" + currentQueueInfo.getTotalSongs() + ")");

                    // Queue exists - just load song detail for UI, don't restart playback
                    loadSongDetail(songId, 1L);
                    return;
                }

                // No queue - need to setup from NavigationContext
                android.util.Log.d("SongDetailViewModel", "No queue found - setting up from NavigationContext");

                // Get current song and artist info first
                Song song = mediaPlayerRepository.getSongByIdSync(songId).get();
                if (song == null) {
                    android.util.Log.e("SongDetailViewModel", "Song not found: " + songId);
                    errorMessage.postValue("Không tìm thấy bài hát");
                    return;
                }

                // Create a simple User object for artist info (we can get more details later if needed)
                User artist = new User();
                artist.setId(song.getUploaderId());
                artist.setDisplayName("Artist"); // Placeholder - will be updated when song detail loads

                // Check if this song is already playing - if so, just setup queue without restarting
                MediaPlayerState.CurrentPlaybackState currentState = mediaPlayerRepository.getCurrentPlaybackState().getValue();
                boolean isCurrentlyPlaying = currentState != null &&
                    currentState.getCurrentSong() != null &&
                    currentState.getCurrentSong().getId() == songId &&
                    currentState.isPlaying();

                if (isCurrentlyPlaying) {
                    android.util.Log.d("SongDetailViewModel", "Song is already playing - setting up queue without restart");
                    // Just setup the queue context without restarting playback
                    boolean success = mediaPlayerRepository.setupQueueFromContext(song, artist, navigationContext).get();
                    if (success) {
                        // Ensure mini player visibility after queue setup
                        mediaPlayerRepository.ensurePlayerVisibility();
                        loadSongDetail(songId, 1L);
                    }
                } else {
                    android.util.Log.d("SongDetailViewModel", "Song not playing - setting up queue and starting playback");
                    // Setup queue with context and start playback
                    boolean success = mediaPlayerRepository.playSongWithContext(song, artist, navigationContext).get();

                    if (success) {
                        android.util.Log.d("SongDetailViewModel", "Successfully setup queue from NavigationContext");
                        loadSongDetail(songId, 1L);
                    } else {
                        android.util.Log.e("SongDetailViewModel", "Failed to setup queue from NavigationContext");
                        errorMessage.postValue("Không thể thiết lập hàng đợi phát nhạc");
                    }
                }

            } catch (Exception e) {
                android.util.Log.e("SongDetailViewModel", "Error setting up queue from context", e);
                errorMessage.postValue("Lỗi khi thiết lập hàng đợi: " + e.getMessage());
            }
        });
    }

    /**
     * Reuse existing queue or create single-song queue if none exists
     */
    public void reuseExistingQueueOrInit() {
        executor.execute(() -> {
            try {
                MediaPlayerState.QueueInfo queueInfo = mediaPlayerRepository.getQueueInfo().getValue();

                if (queueInfo == null || queueInfo.getTotalSongs() == 0) {
                    android.util.Log.d("SongDetailViewModel", "No queue found - creating single song queue");

                    // Get current song from MediaPlayerRepository
                    MediaPlayerState.CurrentPlaybackState currentState = mediaPlayerRepository.getCurrentPlaybackState().getValue();
                    if (currentState != null && currentState.getCurrentSong() != null) {
                        // Create single-song queue
                        mediaPlayerRepository.setSingleSongQueue(currentState.getCurrentSong());
                        // Ensure mini player visibility
                        mediaPlayerRepository.ensurePlayerVisibility();
                    }
                } else {
                    android.util.Log.d("SongDetailViewModel", "Queue already exists: " +
                        queueInfo.getQueueTitle() + " (" + queueInfo.getCurrentIndex() + "/" + queueInfo.getTotalSongs() + ")");
                }
            } catch (Exception e) {
                android.util.Log.e("SongDetailViewModel", "Error setting up queue", e);
            }
        });
    }

    /**
     * Ensure queue context is properly setup
     */
    public void ensureQueueContext() {
        executor.execute(() -> {
            try {
                MediaPlayerState.QueueInfo queueInfo = mediaPlayerRepository.getQueueInfo().getValue();
                android.util.Log.d("SongDetailViewModel", "Current queue context: " +
                    (queueInfo != null ? queueInfo.getQueueTitle() + " (" + queueInfo.getCurrentIndex() + "/" + queueInfo.getTotalSongs() + ")" : "null"));

                if (queueInfo == null || queueInfo.getTotalSongs() == 0) {
                    android.util.Log.w("SongDetailViewModel", "No queue context available - navigation may not work");
                }
            } catch (Exception e) {
                android.util.Log.e("SongDetailViewModel", "Error checking queue context", e);
            }
        });
    }

    /**
     * Hide mini player
     */
    public void hideMiniPlayer() {
        mediaPlayerRepository.hidePlayer();
        // Pause thông qua MediaPlayerRepository thay vì trực tiếp
        executor.execute(() -> {
            try {
                mediaPlayerRepository.pause().get();
            } catch (Exception e) {
                android.util.Log.w("SongDetailViewModel", "Error pausing: " + e.getMessage());
            }
        });
    }

    // Current artist LiveData (thêm field riêng để tránh memory leak)
    private final MutableLiveData<User> currentArtist = new MutableLiveData<>();

    /**
     * Get current artist (compatibility method)
     */
    public LiveData<User> getCurrentArtist() {
        return currentArtist;
    }

    /**
     * Set current artist (internal method)
     */
    private void setCurrentArtist(User artist) {
        currentArtist.postValue(artist);
    }

    /**
     * Toggle like without parameters (compatibility method)
     */
    public void toggleLike() {
        Song song = currentSong.getValue();
        android.util.Log.d("SongDetailViewModel", "🔄 toggleLike() called - Song: " + (song != null ? song.getTitle() + " (ID: " + song.getId() + ")" : "NULL"));
        
        if (song != null) {
            // FIXED: Get real current user ID instead of hardcoded 1
            long currentUserId = getCurrentUserId(); // This should get the actual logged-in user
            android.util.Log.d("SongDetailViewModel", "🔄 Calling toggleLike with songId: " + song.getId() + ", userId: " + currentUserId);
            toggleLike(song.getId(), currentUserId);
        } else {
            android.util.Log.e("SongDetailViewModel", "❌ No song selected to like");
            errorMessage.postValue("No song selected to like");
        }
    }

    // Getters cho playback LiveData
    public LiveData<Boolean> getIsPlaying() { return isPlaying; }
    public LiveData<Boolean> getIsVisible() { return mediaPlayerRepository.getIsPlayerVisible(); }
    public LiveData<Integer> getProgress() { return progress; }
    public LiveData<Long> getCurrentPosition() { return currentPosition; }
    public LiveData<Long> getDuration() { return duration; }
    public LiveData<Boolean> getIsFollowing() { return isFollowing; }
    public NavigationContext getCurrentNavigationContext() { return currentNavigationContext; }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopProgressUpdates();
        // REMOVED: service unbinding - chỉ MediaPlayerRepository được phép bind/unbind service
        if (repository != null) {
            repository.shutdown();
        }
        if (executor != null) {
            executor.shutdown();
        }
        if (mediaPlayerRepository != null) {
            mediaPlayerRepository.shutdown();
        }
    }
}
