package com.g3.soundify_musicplayer.utils;

import android.app.Application;

import com.g3.soundify_musicplayer.data.repository.MediaPlayerRepository;
import com.g3.soundify_musicplayer.data.repository.SongDetailRepository;

/**
 * Singleton Repository Manager
 * Đảm bảo chỉ có MỘT instance của mỗi Repository trong toàn bộ application
 * Giải quyết vấn đề state isolation giữa MiniPlayer và FullPlayer
 */
public class RepositoryManager {
    
    private static volatile RepositoryManager INSTANCE;
    
    // Singleton Repository instances
    private MediaPlayerRepository mediaPlayerRepository;
    private SongDetailRepository songDetailRepository;
    
    // Application context
    private final Application application;
    
    private RepositoryManager(Application application) {
        this.application = application;
        initializeRepositories();
    }
    
    /**
     * Get singleton instance of RepositoryManager
     */
    public static RepositoryManager getInstance(Application application) {
        if (INSTANCE == null) {
            synchronized (RepositoryManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RepositoryManager(application);
                    android.util.Log.d("RepositoryManager", "✅ RepositoryManager singleton created");
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Initialize all Repository instances
     * QUAN TRỌNG: MediaPlayerRepository phải được tạo trước SongDetailRepository
     * vì SongDetailRepository có thể phụ thuộc vào MediaPlayerRepository
     */
    private void initializeRepositories() {
        android.util.Log.d("RepositoryManager", "🔄 Initializing singleton repositories...");
        
        // Tạo MediaPlayerRepository trước (quan trọng cho service binding)
        mediaPlayerRepository = new MediaPlayerRepository(application);
        android.util.Log.d("RepositoryManager", "✅ MediaPlayerRepository singleton created: " + 
            mediaPlayerRepository.hashCode());
        
        // Tạo SongDetailRepository
        songDetailRepository = new SongDetailRepository(application);
        android.util.Log.d("RepositoryManager", "✅ SongDetailRepository singleton created: " + 
            songDetailRepository.hashCode());
        
        android.util.Log.d("RepositoryManager", "🎉 All singleton repositories initialized successfully!");
    }
    
    /**
     * Get singleton MediaPlayerRepository instance
     * Đây là instance duy nhất quản lý playback state trong toàn bộ app
     */
    public MediaPlayerRepository getMediaPlayerRepository() {
        if (mediaPlayerRepository == null) {
            throw new IllegalStateException("MediaPlayerRepository not initialized!");
        }
        android.util.Log.d("RepositoryManager", "📱 Providing MediaPlayerRepository singleton: " + 
            mediaPlayerRepository.hashCode());
        return mediaPlayerRepository;
    }
    
    /**
     * Get singleton SongDetailRepository instance
     */
    public SongDetailRepository getSongDetailRepository() {
        if (songDetailRepository == null) {
            throw new IllegalStateException("SongDetailRepository not initialized!");
        }
        android.util.Log.d("RepositoryManager", "📱 Providing SongDetailRepository singleton: " + 
            songDetailRepository.hashCode());
        return songDetailRepository;
    }
    
    /**
     * Cleanup method - call when application is destroyed
     */
    public void cleanup() {
        android.util.Log.d("RepositoryManager", "🧹 Cleaning up repositories...");
        
        if (mediaPlayerRepository != null) {
            mediaPlayerRepository.shutdown();
            android.util.Log.d("RepositoryManager", "✅ MediaPlayerRepository cleaned up");
        }
        
        if (songDetailRepository != null) {
            songDetailRepository.shutdown();
            android.util.Log.d("RepositoryManager", "✅ SongDetailRepository cleaned up");
        }
        
        // Reset instance
        INSTANCE = null;
        android.util.Log.d("RepositoryManager", "🎉 RepositoryManager cleanup completed");
    }
    
    /**
     * Debug method to check repository states
     */
    public void debugRepositoryStates() {
        android.util.Log.d("RepositoryManager", "=== REPOSITORY DEBUG INFO ===");
        android.util.Log.d("RepositoryManager", "RepositoryManager instance: " + this.hashCode());
        android.util.Log.d("RepositoryManager", "MediaPlayerRepository: " + 
            (mediaPlayerRepository != null ? mediaPlayerRepository.hashCode() : "NULL"));
        android.util.Log.d("RepositoryManager", "SongDetailRepository: " + 
            (songDetailRepository != null ? songDetailRepository.hashCode() : "NULL"));
        
        if (mediaPlayerRepository != null) {
            // Check service binding status
            mediaPlayerRepository.checkServiceStatus();
        }
    }
}
