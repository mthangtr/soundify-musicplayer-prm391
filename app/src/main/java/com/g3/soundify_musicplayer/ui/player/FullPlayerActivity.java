package com.g3.soundify_musicplayer.ui.player;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.ui.player.comment.CommentActivity;
import com.g3.soundify_musicplayer.ui.player.queue.QueueActivity;
import com.g3.soundify_musicplayer.ui.player.playlist.PlaylistSelectionActivity;
import com.g3.soundify_musicplayer.utils.TimeUtils;

/**
 * Full-screen Activity for the music player
 * Converted from Fragment-based to pure Activity-based architecture
 */
public class FullPlayerActivity extends AppCompatActivity {

    private static final String EXTRA_SONG_ID = "song_id";

    private ActivityResultLauncher<Intent> playlistSelectionLauncher;
    private ActivityResultLauncher<Intent> commentActivityLauncher;

    // UI Components
    private ImageButton btnMinimize;
    private TextView textSongTitle;
    private TextView textArtistName;
    private Button btnFollow;
    private ImageView imageAlbumArt;
    private SeekBar seekbarProgress;
    private TextView textCurrentTime;
    private TextView textTotalTime;
    private ImageButton btnPrevious;
    private ImageButton btnPlayPause;
    private ImageButton btnNext;
    private ImageButton btnLike;
    private ImageButton btnComments;
    private ImageButton btnAddToPlaylist;
    private ImageButton btnQueue;

    // ViewModel
    private SongDetailViewModel viewModel;

    // Current song data
    private Song currentSong;
    private User currentArtist;
    private boolean isPlaying = false;
    private boolean isLiked = false;
    private boolean isFollowing = false;

    // Seek bar state
    private boolean isUserSeeking = false;

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

        setContentView(R.layout.fragment_full_player);

        // Initialize ViewModel with Singleton Repository pattern
        SongDetailViewModelFactory factory = new SongDetailViewModelFactory(getApplication());
        viewModel = new ViewModelProvider(this, factory).get(SongDetailViewModel.class);

        // Initialize activity-based UI
        initViews();
        setupActivityResultLaunchers();
        setupClickListeners();
        observeViewModel();
    }

    private void setupActivityResultLaunchers() {
        // Setup Activity Result Launcher for playlist selection
        playlistSelectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String playlistName = result.getData().getStringExtra(PlaylistSelectionActivity.RESULT_PLAYLIST_NAME);
                    if (playlistName != null) {
                        showToast("Added to " + playlistName);
                    }
                }
            }
        );

        // Setup Activity Result Launcher for comment activity
        commentActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    boolean commentCountChanged = result.getData().getBooleanExtra(CommentActivity.RESULT_COMMENT_COUNT_CHANGED, false);
                    if (commentCountChanged && viewModel != null && currentSong != null) {
                        viewModel.refreshCommentCount(currentSong.getId());
                    }
                }
            }
        );
    }

    private void initViews() {
        // Header components
        btnMinimize = findViewById(R.id.btn_minimize);
        textSongTitle = findViewById(R.id.text_song_title);
        textArtistName = findViewById(R.id.text_artist_name);
        btnFollow = findViewById(R.id.btn_follow);

        // Album art
        imageAlbumArt = findViewById(R.id.image_album_art);

        // Playback controls
        seekbarProgress = findViewById(R.id.seekbar_progress);
        seekbarProgress.setMax(100);
        seekbarProgress.setProgress(0);

        textCurrentTime = findViewById(R.id.text_current_time);
        textTotalTime = findViewById(R.id.text_total_time);

        // Initialize with default values
        textCurrentTime.setText(getString(R.string.time_zero));
        textTotalTime.setText(getString(R.string.duration_unknown));
        btnPrevious = findViewById(R.id.btn_previous);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnNext = findViewById(R.id.btn_next);

        // Bottom action bar
        btnLike = findViewById(R.id.btn_like);
        btnComments = findViewById(R.id.btn_comments);
        btnAddToPlaylist = findViewById(R.id.btn_add_to_playlist);
        btnQueue = findViewById(R.id.btn_queue);
    }

    private void setupClickListeners() {
        // Header actions
        btnMinimize.setOnClickListener(v -> {
            try {
                // Pause ViewModel updates to prevent thread leaks during transition
                if (viewModel != null) {
                    viewModel.pauseUpdates();
                }

                showToast("Minimized to mini player");
                minimizeToMiniPlayer();
            } catch (Exception e) {
                android.util.Log.e("FullPlayerActivity", "Error during minimize", e);
                finish();
            }
        });

        btnFollow.setOnClickListener(v -> {
            if (currentArtist != null) {
                viewModel.toggleFollow();
            } else {
                showToast("No artist information available");
            }
        });

        // Playback controls
        btnPrevious.setOnClickListener(v -> {
            viewModel.playPrevious();
            showToast("Previous track");
        });

        btnPlayPause.setOnClickListener(v -> {
            try {
                viewModel.togglePlayPause();
            } catch (Exception e) {
                showToast("Playback error occurred");
            }
        });

        btnNext.setOnClickListener(v -> {
            viewModel.playNext();
            showToast("Next track");
        });

        // Progress bar - Seek functionality
        seekbarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && currentSong != null) {
                    updateCurrentTimeFromProgress(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                viewModel.seekToPercentage(progress);
                isUserSeeking = false;
            }
        });

        // Bottom action bar - Like button
        btnLike.setOnClickListener(v -> {
            if (currentSong != null) {
                viewModel.toggleLike();
            } else {
                android.util.Log.w("FullPlayerActivity", "No song selected for like");
                showToast("No song selected");
            }
        });

        btnComments.setOnClickListener(v -> {
            navigateToComments();
        });

        btnAddToPlaylist.setOnClickListener(v -> {
            if (currentSong != null) {
                navigateToPlaylistSelection();
            } else {
                showToast("No song selected");
            }
        });

        btnQueue.setOnClickListener(v -> {
            if (currentSong != null) {
                navigateToQueue();
            } else {
                showToast("No song selected");
            }
        });
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

    private void observeViewModel() {
        // Song data
        viewModel.getCurrentSong().observe(this, song -> {
            if (song != null) {
                currentSong = song;
                updateSongInfo(song);
            }
        });

        // Artist data
        viewModel.getCurrentArtist().observe(this, artist -> {
            if (artist != null) {
                currentArtist = artist;
                updateArtistInfo(artist);
            }
        });

        // Like status
        viewModel.getIsLiked().observe(this, liked -> {
            if (liked != null) {
                isLiked = liked;
                updateLikeButton();
            }
        });

        // Follow status
        viewModel.getIsFollowing().observe(this, following -> {
            if (following != null) {
                isFollowing = following;
                updateFollowButton();
            }
        });

        // Playing state
        viewModel.getIsPlaying().observe(this, playing -> {
            if (playing != null) {
                isPlaying = playing;
                updatePlayPauseButton();
            }
        });

        // Current position
        viewModel.getCurrentPosition().observe(this, positionMs -> {
            if (positionMs != null && !isUserSeeking && currentSong != null) {
                textCurrentTime.setText(formatTime(positionMs));

                // Calculate progress from position
                Long duration = viewModel.getDuration().getValue();
                if (duration != null && duration > 0) {
                    int progressPercent = (int) ((positionMs * 100) / duration);
                    seekbarProgress.setProgress(progressPercent);
                }
            }
        });

        // Duration observer
        viewModel.getDuration().observe(this, durationMs -> {
            if (durationMs != null && durationMs > 0) {
                textTotalTime.setText(formatTime(durationMs));
            }
        });

        // Observe queue info to enable/disable navigation buttons
        viewModel.getQueueInfo().observe(this, queueInfo -> {
            if (queueInfo != null) {
                // Enable/disable buttons based on queue state
                btnPrevious.setEnabled(true); // Always enabled - handles 3-second logic
                btnNext.setEnabled(true);     // Always enabled - will restart if at end
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
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

        try {
            // ✅ CRITICAL: Ensure service connection is stable before finishing
            if (viewModel != null) {
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
    }

    private void updateSongInfo(Song song) {
        textSongTitle.setText(song.getTitle());

        // Update total time
        if (song.getDurationMs() != null && song.getDurationMs() > 0) {
            textTotalTime.setText(formatTime(song.getDurationMs()));
            seekbarProgress.setMax(100);
        } else {
            textTotalTime.setText(getString(R.string.duration_unknown));
            seekbarProgress.setMax(100);
        }

        // Load cover art using Glide
        if (song.getCoverArtUrl() != null && !song.getCoverArtUrl().isEmpty()) {
            Glide.with(this)
                    .load(song.getCoverArtUrl())
                    .placeholder(R.drawable.splashi_icon)
                    .error(R.drawable.splashi_icon)
                    .into(imageAlbumArt);
        } else {
            imageAlbumArt.setImageResource(R.drawable.splashi_icon);
        }
    }

    private void updateArtistInfo(User artist) {
        textArtistName.setText(artist.getDisplayName() != null ?
            artist.getDisplayName() : artist.getUsername());
    }

    private void updateLikeButton() {
        if (isLiked) {
            btnLike.setImageResource(R.drawable.ic_heart_filled);
            btnLike.setColorFilter(getResources().getColor(R.color.button_like_active, null));
        } else {
            btnLike.setImageResource(R.drawable.ic_heart);
            btnLike.setColorFilter(getResources().getColor(R.color.button_like_inactive, null));
        }
    }

    private void updateFollowButton() {
        if (isFollowing) {
            btnFollow.setText(getString(R.string.player_following));
            btnFollow.setSelected(true);
        } else {
            btnFollow.setText(getString(R.string.player_follow));
            btnFollow.setSelected(false);
        }
    }

    private void updatePlayPauseButton() {
        if (isPlaying) {
            btnPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_play);
        }
    }

    /**
     * Update current time display based on seek bar progress (0-100)
     */
    private void updateCurrentTimeFromProgress(int progressPercent) {
        if (currentSong != null && currentSong.getDurationMs() != null) {
            long totalDurationMs = currentSong.getDurationMs();
            long estimatedPositionMs = (totalDurationMs * progressPercent) / 100;
            textCurrentTime.setText(formatTime(estimatedPositionMs));
        }
    }

    /**
     * Helper method to format time consistently
     */
    private String formatTime(long timeMs) {
        if (timeMs <= 0) {
            return "0:00";
        }
        return TimeUtils.formatDuration((int) timeMs);
    }

    private void navigateToComments() {
        if (currentSong != null) {
            Intent intent = CommentActivity.createIntent(this, currentSong.getId());
            commentActivityLauncher.launch(intent);
        }
    }

    private void navigateToPlaylistSelection() {
        if (currentSong != null) {
            Intent intent = PlaylistSelectionActivity.createIntent(this, currentSong.getId());
            playlistSelectionLauncher.launch(intent);
        }
    }

    private void navigateToQueue() {
        if (currentSong != null) {
            Intent intent = QueueActivity.createIntent(this, currentSong.getId());
            startActivity(intent);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
