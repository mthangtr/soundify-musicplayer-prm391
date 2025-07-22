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
    }
    
    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SongDetailViewModel.class)) {

            // Get singleton repository instances
            SongDetailRepository songDetailRepository = repositoryManager.getSongDetailRepository();
            MediaPlayerRepository mediaPlayerRepository = repositoryManager.getMediaPlayerRepository();

            // Create SongDetailViewModel with injected singleton repositories
            SongDetailViewModel viewModel = new SongDetailViewModel(
                application,
                songDetailRepository,
                mediaPlayerRepository
            );

            return (T) viewModel;
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }

}
