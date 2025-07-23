package com.g3.soundify_musicplayer.utils;

import android.app.Application;

import com.g3.soundify_musicplayer.data.repository.MediaPlayerRepository;
import com.g3.soundify_musicplayer.data.repository.SongDetailRepository;
import com.g3.soundify_musicplayer.data.repository.SongRepository;

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
    private SongRepository songRepository;
    
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
        
        // Tạo MediaPlayerRepository trước (quan trọng cho service binding)
        mediaPlayerRepository = new MediaPlayerRepository(application);
        songDetailRepository = new SongDetailRepository(application);
        songRepository = new SongRepository(application);
    }
    
    /**
     * Get singleton MediaPlayerRepository instance
     * Đây là instance duy nhất quản lý playback state trong toàn bộ app
     */
    public MediaPlayerRepository getMediaPlayerRepository() {
        if (mediaPlayerRepository == null) {
            throw new IllegalStateException("MediaPlayerRepository not initialized!");
        }
        return mediaPlayerRepository;
    }
    
    /**
     * Get singleton SongDetailRepository instance
     */
    public SongDetailRepository getSongDetailRepository() {
        if (songDetailRepository == null) {
            throw new IllegalStateException("SongDetailRepository not initialized!");
        }
        return songDetailRepository;
    }

    /**
     * Get singleton SongRepository instance
     */
    public SongRepository getSongRepository() {
        if (songRepository == null) {
            throw new IllegalStateException("SongRepository not initialized!");
        }
        return songRepository;
    }
    
    /**
     * Cleanup method - call when application is destroyed
     */
    public void cleanup() {
        if (mediaPlayerRepository != null) {
            mediaPlayerRepository.shutdown();
        }
        
        if (songDetailRepository != null) {
            songDetailRepository.shutdown();
        }
        
        // Reset instance
        INSTANCE = null;
    }
    
    /**
     * Check repository states
     */
    public void debugRepositoryStates() {
        if (mediaPlayerRepository != null) {
            // Check service binding status
            mediaPlayerRepository.checkServiceStatus();
        }
    }
}
