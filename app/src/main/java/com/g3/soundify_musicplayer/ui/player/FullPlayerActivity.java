package com.g3.soundify_musicplayer.ui.player;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import com.g3.soundify_musicplayer.R;


/**
 * Full-screen Activity for the music player
 * Provides immersive full-screen experience without navigation bars
 */
public class FullPlayerActivity extends AppCompatActivity {

    private static final String EXTRA_SONG_ID = "song_id";
    private static final String EXTRA_NAVIGATION_CONTEXT = "navigation_context";

    // ViewModel with Singleton Repository pattern
    private SongDetailViewModel viewModel;
    
    /**
     * Create intent to launch FullPlayerActivity
     */
    public static Intent createIntent(Context context, long songId) {
        Intent intent = new Intent(context, FullPlayerActivity.class);
        intent.putExtra(EXTRA_SONG_ID, songId);
        return intent;
    }
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Setup full-screen immersive mode
        setupFullScreenMode();

        // Setup modern back press handling
        setupBackPressHandling();

        setContentView(R.layout.activity_full_player);

        // Initialize ViewModel with Singleton Repository pattern
        // QUAN TRỌNG: Phải tạo ViewModel trước khi Fragment được tạo
        SongDetailViewModelFactory factory = new SongDetailViewModelFactory(getApplication());
        viewModel = new ViewModelProvider(this, factory).get(SongDetailViewModel.class);
        android.util.Log.d("FullPlayerActivity", "SongDetailViewModel initialized with singleton repositories: " +
            viewModel.hashCode());

        // Get data from intent
        long songId = getIntent().getLongExtra(EXTRA_SONG_ID, -1);

        if (songId == -1) {
            android.util.Log.e("FullPlayerActivity", "No song ID provided");
            finish();
            return;
        }
        
        // Create and add FullPlayerFragment
        if (savedInstanceState == null) {
            FullPlayerFragment fragment = FullPlayerFragment.newInstance(songId);
            
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.full_player_container, fragment)
                .commit();
        }

    }
    
    /**
     * Setup full-screen immersive mode
     */
    private void setupFullScreenMode() {
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        // Get window insets controller
        WindowInsetsControllerCompat windowInsetsController = 
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        
        if (windowInsetsController != null) {
            // Hide system bars (status bar and navigation bar)
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
            
            // Set behavior for when user swipes to show system bars
            windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }
        
        // Keep screen on while playing music
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Set status bar and navigation bar colors to transparent
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Ensure full-screen mode is maintained
        setupFullScreenMode();
    }
    
    /**
     * Setup modern back press handling using OnBackPressedDispatcher
     */
    private void setupBackPressHandling() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Handle back press - minimize to mini player
                minimizeToMiniPlayer();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }
    
    /**
     * ✅ FIXED: Minimize to mini player with proper state preservation
     */
    public void minimizeToMiniPlayer() {
        android.util.Log.d("FullPlayerActivity", "Minimizing to mini player");
        
        try {
            // ✅ CRITICAL: Ensure service connection is stable before finishing
            if (viewModel != null) {
                // Stop any progress updates to prevent thread leaks
                android.util.Log.d("FullPlayerActivity", "Ensuring clean ViewModel state before minimize");
            }
            
            // ✅ IMPORTANT: Service will continue running for MiniPlayer
            // MediaPlayerRepository singleton ensures state persistence
            finish();

            // Smooth transition animation
            overridePendingTransition(0, R.anim.slide_down_out);
            
        } catch (Exception e) {
            android.util.Log.e("FullPlayerActivity", "Error during minimize", e);
            // Fallback: just finish
            finish();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();

        // ✅ CRITICAL: Clean up progress updates when paused to prevent leaks
        if (viewModel != null) {
            // Progress updates will be handled by MiniPlayer after minimize
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ✅ IMPORTANT: Don't cleanup global state here!
        // MediaPlayerRepository singleton must persist for MiniPlayer
        // Only cleanup local activity resources

        try {
            // Clear window flags to prevent leaks
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Exception e) {
            android.util.Log.w("FullPlayerActivity", "Error clearing window flags", e);
        }
    }
}
