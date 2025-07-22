package com.g3.soundify_musicplayer.ui.base;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.ui.player.MiniPlayerFragment;
import com.g3.soundify_musicplayer.ui.player.SongDetailViewModel;
import com.g3.soundify_musicplayer.ui.player.SongDetailViewModelFactory;
import androidx.lifecycle.ViewModelProvider;

/**
 * Base Activity that includes mini player functionality.
 * All activities that should show the mini player should extend this class.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private FrameLayout miniPlayerContainer;
    private MiniPlayerFragment miniPlayerFragment;
    private SongDetailViewModel songDetailViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set the content view first
        setContentView(getLayoutResourceId());
        
        // Initialize mini player after content view is set
        initMiniPlayer();
    }

    /**
     * Subclasses must provide their layout resource ID
     */
    protected abstract int getLayoutResourceId();

    /**
     * Initialize the mini player component
     */
    private void initMiniPlayer() {
        // Find or create mini player container
        miniPlayerContainer = findViewById(R.id.mini_player_container);
        
        if (miniPlayerContainer != null) {
            // Create and add mini player fragment
            miniPlayerFragment = new MiniPlayerFragment();
            
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.mini_player_container, miniPlayerFragment);
            transaction.commit();

            // Initialize ViewModel THỐNG NHẤT với Singleton Repository pattern
            SongDetailViewModelFactory factory = new SongDetailViewModelFactory(getApplication());
            songDetailViewModel = new ViewModelProvider(this, factory).get(SongDetailViewModel.class);

            songDetailViewModel.getIsVisible().observe(this, isVisible -> {
                if (shouldShowMiniPlayer() && isVisible != null && isVisible) {
                    miniPlayerContainer.setVisibility(android.view.View.VISIBLE);
                } else {
                    miniPlayerContainer.setVisibility(android.view.View.GONE);
                }
            });
        }
    }

    /**
     * Get the mini player fragment instance
     */
    protected MiniPlayerFragment getMiniPlayerFragment() {
        return miniPlayerFragment;
    }

    /**
     * Check if this activity should show mini player
     * Subclasses can override to hide mini player on specific screens
     */
    protected boolean shouldShowMiniPlayer() {
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Update mini player visibility based on activity preference
        if (miniPlayerContainer != null) {
            if (shouldShowMiniPlayer()) {
                miniPlayerContainer.setVisibility(android.view.View.VISIBLE);
            } else {
                miniPlayerContainer.setVisibility(android.view.View.GONE);
            }
        }
    }
}
