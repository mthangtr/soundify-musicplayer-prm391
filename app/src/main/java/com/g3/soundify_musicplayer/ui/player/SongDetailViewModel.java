package com.g3.soundify_musicplayer.ui.player;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Comment;
import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.repository.MusicPlayerRepository;
import com.g3.soundify_musicplayer.data.repository.SongDetailRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel cho Song Detail Screen
 * Sử dụng SongDetailRepository để quản lý tất cả data operations
 */
public class SongDetailViewModel extends AndroidViewModel {
    
    private SongDetailRepository repository;
    private ExecutorService executor;
    
    // LiveData cho UI
    private MutableLiveData<Song> currentSong = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLiked = new MutableLiveData<>();
    private MutableLiveData<Integer> likeCount = new MutableLiveData<>();
    private MutableLiveData<Integer> commentCount = new MutableLiveData<>();
    private MutableLiveData<List<Playlist>> userPlaylists = new MutableLiveData<>();
    private MutableLiveData<List<Long>> playlistsContainingSong = new MutableLiveData<>();
    private MutableLiveData<List<Song>> relatedSongs = new MutableLiveData<>();
    private MutableLiveData<List<Song>> moreSongsByArtist = new MutableLiveData<>();
    
    // Status LiveData
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    public SongDetailViewModel(@NonNull Application application) {
        super(application);
        repository = new SongDetailRepository(application);
        executor = Executors.newFixedThreadPool(2);
        
        // Initialize values
        isLoading.setValue(false);
        isLiked.setValue(false);
        likeCount.setValue(0);
        commentCount.setValue(0);
    }
    
    // ========== PUBLIC METHODS ==========
    
    /**
     * Load song detail data
     */
    public void loadSongDetail(long songId, long userId) {
        isLoading.setValue(true);
        
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
        executor.execute(() -> {
            try {
                Boolean newLikeStatus = repository.toggleSongLike(songId, userId).get();
                isLiked.postValue(newLikeStatus);
                
                // Update like count
                MusicPlayerRepository.SongLikeInfo likeInfo = repository.getSongLikeInfo(songId, userId).get();
                likeCount.postValue(likeInfo.likeCount);
                
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi thực hiện like: " + e.getMessage());
            }
        });
    }
    
    /**
     * Add comment to song
     */
    public void addComment(long songId, long userId, String content) {
        if (content == null || content.trim().isEmpty()) {
            errorMessage.setValue("Nội dung comment không được để trống");
            return;
        }
        
        executor.execute(() -> {
            try {
                Long commentId = repository.addComment(songId, userId, content.trim()).get();
                if (commentId != null && commentId > 0) {
                    // Update comment count
                    Integer newCount = repository.getCommentCountBySong(songId).get();
                    commentCount.postValue(newCount);
                } else {
                    errorMessage.postValue("Không thể thêm comment");
                }
                
            } catch (Exception e) {
                errorMessage.postValue("Lỗi khi thêm comment: " + e.getMessage());
            }
        });
    }
    
    /**
     * Add song to selected playlists
     */
    public void addSongToPlaylists(long songId, List<Long> selectedPlaylistIds) {
        if (selectedPlaylistIds == null || selectedPlaylistIds.isEmpty()) {
            errorMessage.setValue("Vui lòng chọn ít nhất một playlist");
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
    public void createPlaylistWithSong(String playlistName, String description, boolean isPublic, long ownerId, long songId) {
        if (playlistName == null || playlistName.trim().isEmpty()) {
            errorMessage.setValue("Tên playlist không được để trống");
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
        // TODO: Get current user ID from session/preferences
        return 1L; // Mock user ID for now
    }
    
    // ========== GETTERS FOR LIVEDATA ==========
    
    public LiveData<Song> getCurrentSong() { return currentSong; }
    public LiveData<Boolean> getIsLiked() { return isLiked; }
    public LiveData<Integer> getLikeCount() { return likeCount; }
    public LiveData<Integer> getCommentCount() { return commentCount; }
    public LiveData<List<Playlist>> getUserPlaylists() { return userPlaylists; }
    public LiveData<List<Long>> getPlaylistsContainingSong() { return playlistsContainingSong; }
    public LiveData<List<Song>> getRelatedSongs() { return relatedSongs; }
    public LiveData<List<Song>> getMoreSongsByArtist() { return moreSongsByArtist; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    
    // Get LiveData from repository
    public LiveData<List<Comment>> getCommentsBySong(long songId) {
        return repository.getCommentsBySong(songId);
    }
    
    public LiveData<List<User>> getUsersWhoLikedSong(long songId) {
        return repository.getUsersWhoLikedSong(songId);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        if (repository != null) {
            repository.shutdown();
        }
        if (executor != null) {
            executor.shutdown();
        }
    }
}
