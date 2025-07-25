package com.g3.soundify_musicplayer.ui.library;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.dto.SongWithUploaderInfo;
import com.g3.soundify_musicplayer.data.repository.MusicPlayerRepository;
import com.g3.soundify_musicplayer.utils.AuthManager;

import java.util.List;

/**
 * ViewModel for LikedSongPlaylistFragment
 * Manages liked songs data from song_likes table
 */
public class LikedSongPlaylistViewModel extends AndroidViewModel {

    private MusicPlayerRepository musicPlayerRepository;
    private AuthManager authManager;
    
    // LiveData cho UI
    private LiveData<List<SongWithUploaderInfo>> likedSongs;
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Integer> songCount = new MutableLiveData<>(0);

    public LikedSongPlaylistViewModel(@NonNull Application application) {
        super(application);
        
        // Initialize repositories
        musicPlayerRepository = new MusicPlayerRepository(application);
        authManager = new AuthManager(application);
        
        // Load liked songs data
        loadLikedSongs();
    }

    /**
     * Load liked songs for current user
     */
    private void loadLikedSongs() {
        long currentUserId = authManager.getCurrentUserId();
        if (currentUserId != -1) {
            isLoading.setValue(true);
            
            // Get liked songs with uploader info from repository - this returns LiveData
            likedSongs = musicPlayerRepository.getLikedSongsWithUploaderInfoByUser(currentUserId);
            
            // Note: songCount will be updated via observer in Fragment
            isLoading.setValue(false);
        } else {
            android.util.Log.e("LikedSongPlaylistViewModel", "❌ User not logged in or invalid userId: " + currentUserId);
            errorMessage.setValue("Bạn cần đăng nhập để xem bài hát đã thích");
            songCount.setValue(0);
        }
    }

    /**
     * Refresh liked songs data
     */
    public void refreshLikedSongs() {
        loadLikedSongs();
    }

    /**
     * Test method to verify database connection and data
     */
    public void testDatabaseConnection() {
        long currentUserId = authManager.getCurrentUserId();
        
        if (currentUserId != -1) {
            // Test with executor to check database directly
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    // Try to get song like info directly
                    int likeCount = musicPlayerRepository.getLikeCountForSong(1L).get(); // Test with songId = 1
                    
                    boolean isLiked = musicPlayerRepository.isSongLikedByUser(1L, currentUserId).get();
                    
                } catch (Exception e) {
                    android.util.Log.e("LikedSongPlaylistViewModel", "❌ Database test failed", e);
                }
            });
        }
    }

    /**
     * Update song count when songs list changes
     */
    public void updateSongCount(int count) {
        songCount.setValue(count);
    }

    // Getters for LiveData
    public LiveData<List<SongWithUploaderInfo>> getLikedSongs() {
        return likedSongs;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Integer> getSongCount() {
        return songCount;
    }

    /**
     * Clear error message after showing to user
     */
    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }

    /**
     * Get current user ID
     */
    public long getCurrentUserId() {
        return authManager.getCurrentUserId();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (musicPlayerRepository != null) {
            musicPlayerRepository.shutdown();
        }
    }
} 