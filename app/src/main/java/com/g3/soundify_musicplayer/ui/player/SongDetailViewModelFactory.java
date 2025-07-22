package com.g3.soundify_musicplayer.ui.player;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.g3.soundify_musicplayer.data.repository.MediaPlayerRepository;
import com.g3.soundify_musicplayer.data.repository.SongDetailRepository;
import com.g3.soundify_musicplayer.utils.RepositoryManager;

/**
 * Custom ViewModelFactory for SongDetailViewModel
 * Đảm bảo tất cả SongDetailViewModel instances sử dụng cùng Singleton Repository instances
 * Giải quyết vấn đề state isolation giữa MiniPlayer và FullPlayer
 */
public class SongDetailViewModelFactory implements ViewModelProvider.Factory {
    
    private final Application application;
    private final RepositoryManager repositoryManager;
    
    public SongDetailViewModelFactory(Application application) {
        this.application = application;
        this.repositoryManager = RepositoryManager.getInstance(application);
        
        android.util.Log.d("SongDetailViewModelFactory", "🏭 ViewModelFactory created with RepositoryManager: " + 
            repositoryManager.hashCode());
    }
    
    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SongDetailViewModel.class)) {
            android.util.Log.d("SongDetailViewModelFactory", "🔄 Creating SongDetailViewModel with singleton repositories...");
            
            // Get singleton repository instances
            SongDetailRepository songDetailRepository = repositoryManager.getSongDetailRepository();
            MediaPlayerRepository mediaPlayerRepository = repositoryManager.getMediaPlayerRepository();
            
            // Create SongDetailViewModel with injected singleton repositories
            SongDetailViewModel viewModel = new SongDetailViewModel(
                application, 
                songDetailRepository, 
                mediaPlayerRepository
            );
            
            android.util.Log.d("SongDetailViewModelFactory", "✅ SongDetailViewModel created: " + viewModel.hashCode());
            android.util.Log.d("SongDetailViewModelFactory", "   → Using SongDetailRepository: " + songDetailRepository.hashCode());
            android.util.Log.d("SongDetailViewModelFactory", "   → Using MediaPlayerRepository: " + mediaPlayerRepository.hashCode());
            
            return (T) viewModel;
        }
        
        // Fallback for other ViewModel types
        android.util.Log.w("SongDetailViewModelFactory", "⚠️ Unknown ViewModel class: " + modelClass.getName());
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }

}
