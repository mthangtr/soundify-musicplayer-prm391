package com.g3.soundify_musicplayer.ui.player;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;

import com.g3.soundify_musicplayer.ui.player.comment.CommentsFragment;
import com.g3.soundify_musicplayer.ui.player.queue.QueueFragment;
import com.g3.soundify_musicplayer.ui.player.playlist.PlaylistSelectionActivity;
import com.g3.soundify_musicplayer.utils.TimeUtils;

/**
 * Full Player Screen Fragment
 * UI ONLY - Uses mock data for testing, no backend integration
 */
public class FullPlayerFragment extends Fragment {

    private static final String ARG_SONG_ID = "song_id";
    private static final String ARG_NAVIGATION_CONTEXT = "navigation_context";

    private ActivityResultLauncher<Intent> playlistSelectionLauncher;
    
    private ImageButton btnMinimize;
    private TextView textSongTitle;
    private TextView textArtistName;
    private Button btnFollow;
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
    
    // THỐNG NHẤT: SỬ DỤNG SONGDETAILVIEWMODEL CHO CẢ MINI VÀ FULL PLAYER
    private SongDetailViewModel viewModel; // Có TẤT CẢ: song detail + media playback
    
    // Current song data
    private Song currentSong;
    private User currentArtist;
    private boolean isPlaying = false;
    private boolean isLiked = false;
    private boolean isFollowing = false;

    // Seek bar state
    private boolean isUserSeeking = false;

    public static FullPlayerFragment newInstance(long songId) {
        FullPlayerFragment fragment = new FullPlayerFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SONG_ID, songId);
        fragment.setArguments(args);
        return fragment;
    }

    public static FullPlayerFragment newInstanceWithContext(long songId, Object context) {
        FullPlayerFragment fragment = new FullPlayerFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SONG_ID, songId);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // THỐNG NHẤT: SỬ DỤNG SONGDETAILVIEWMODEL GIỐNG MINIPLAYER
        viewModel = new ViewModelProvider(requireActivity()).get(SongDetailViewModel.class);

        android.util.Log.d("FullPlayerFragment", "SongDetailViewModel initialized: " + viewModel.hashCode());

        // SỬA LỖI: KHÔNG sync data ở đây vì UI chưa được khởi tạo

        // ĐƠN GIẢN HÓA: Không cần load song riêng vì FullPlayer chỉ hiển thị data có sẵn
        // Song data đã có sẵn trong SongDetailViewModel (shared instance)
        if (getArguments() != null) {
            long songId = getArguments().getLong(ARG_SONG_ID);
            android.util.Log.d("FullPlayerFragment", "FullPlayer opened for song ID: " + songId);
            // Data sẽ được sync qua observers
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_full_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupActivityResultLaunchers();
        initializeViews(view);
        setupClickListeners();
        observeViewModel();

        // Load song detail
        if (getArguments() != null) {
            long songId = getArguments().getLong(ARG_SONG_ID);
    

            android.util.Log.d("FullPlayerFragment", "FullPlayer opened for song ID: " + songId);

            // FIXED: FullPlayer is now a COMPLETELY PASSIVE VIEW
            // It ONLY observes existing state from singleton MediaPlayerRepository
            // NO database calls, NO network calls, NO playback commands

            android.util.Log.d("FullPlayerFragment", "FullPlayer will display current playback state from singleton repository");
            android.util.Log.d("FullPlayerFragment", "All data will come from MediaPlayerRepository centralized state");
        }

        // XÓA syncCurrentSongData() - THỪA vì data đã được sync qua Observer pattern
        // Khi MediaPlaybackService gọi onSongChanged() → ViewModel update LiveData → UI tự động update

        android.util.Log.d("FullPlayerFragment", "FullPlayer UI setup completed");
    }

    private void setupActivityResultLaunchers() {
        // Setup Activity Result Launcher thay thế cho deprecated startActivityForResult
        playlistSelectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new androidx.activity.result.ActivityResultCallback<androidx.activity.result.ActivityResult>() {
                @Override
                public void onActivityResult(androidx.activity.result.ActivityResult result) {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        String playlistName = result.getData().getStringExtra(PlaylistSelectionActivity.RESULT_PLAYLIST_NAME);
                        if (playlistName != null) {
                            showToast("Added to " + playlistName);
                        }
                    }
                }
            }
        );
    }

    private void initializeViews(View view) {
        // Header components
        btnMinimize = view.findViewById(R.id.btn_minimize);
        textSongTitle = view.findViewById(R.id.text_song_title);
        textArtistName = view.findViewById(R.id.text_artist_name);
        btnFollow = view.findViewById(R.id.btn_follow);
        
        // Album art - xóa vì không sử dụng
        
        // Playback controls
        seekbarProgress = view.findViewById(R.id.seekbar_progress);
        // SỬA LỖI: Khởi tạo SeekBar với max = 100 để sử dụng percentage system
        android.util.Log.d("FullPlayerFragment", "Initializing SeekBar - current max: " +
            seekbarProgress.getMax() + ", setting to 100");
        seekbarProgress.setMax(100);
        seekbarProgress.setProgress(0);

        textCurrentTime = view.findViewById(R.id.text_current_time);
        textTotalTime = view.findViewById(R.id.text_total_time);

        // Khởi tạo với giá trị mặc định
        textCurrentTime.setText(getString(R.string.time_zero));
        textTotalTime.setText(getString(R.string.duration_unknown));
        btnPrevious = view.findViewById(R.id.btn_previous);
        btnPlayPause = view.findViewById(R.id.btn_play_pause);
        btnNext = view.findViewById(R.id.btn_next);

        // Bottom action bar
        btnLike = view.findViewById(R.id.btn_like);
        btnComments = view.findViewById(R.id.btn_comments);
        btnAddToPlaylist = view.findViewById(R.id.btn_add_to_playlist);
        btnQueue = view.findViewById(R.id.btn_queue);
    }

    private void setupClickListeners() {
        // Header actions
        btnMinimize.setOnClickListener(v -> {
            // ✅ FIXED: Safe minimize with state preservation
            android.util.Log.d("FullPlayerFragment", "🔻 Minimize button clicked");
            
            try {
                // Pause ViewModel updates to prevent thread leaks during transition
                if (viewModel != null) {
                    viewModel.pauseUpdates();
                }
                
                showToast("Minimized to mini player");

                if (getActivity() instanceof FullPlayerActivity) {
                    ((FullPlayerActivity) getActivity()).minimizeToMiniPlayer();
                } else {
                    // Fallback for fragment-based implementation
                    if (getActivity() != null) {
                        getActivity().getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("FullPlayerFragment", "Error during minimize", e);
                // Fallback: just finish activity
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });
        
        btnFollow.setOnClickListener(v -> {
            if (currentArtist != null) {
                // SỬA LỖI: Sử dụng method signature đúng (không cần parameter)
                viewModel.toggleFollow();
                // Visual feedback sẽ được update qua observer
            } else {
                showToast("No artist information available");
            }
        });
        
        // Playback controls
        btnPrevious.setOnClickListener(v -> {
            android.util.Log.d("FullPlayerFragment", "Previous button clicked - calling viewModel.playPrevious()");
            viewModel.playPrevious();
            showToast("Previous track");
        });

        btnPlayPause.setOnClickListener(v -> {
            // ✅ SAFE: Add error handling for play/pause
            try {
                android.util.Log.d("FullPlayerFragment", "▶️ Play/Pause button clicked");
                viewModel.togglePlayPause();
            } catch (Exception e) {
                android.util.Log.e("FullPlayerFragment", "Error during play/pause", e);
                showToast("Playback error occurred");
            }
        });

        btnNext.setOnClickListener(v -> {
            android.util.Log.d("FullPlayerFragment", "Next button clicked - calling viewModel.playNext()");
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
        
        // Bottom action bar - Like button với visual feedback
        btnLike.setOnClickListener(v -> {
            if (currentSong != null) {
                android.util.Log.d("FullPlayerFragment", "🔄 Like button clicked for song: " + currentSong.getTitle() + " (ID: " + currentSong.getId() + ")");
                
                // SỬA LỖI: Sử dụng method signature đúng (không cần parameter)
                viewModel.toggleLike();
                // Visual feedback sẽ được update qua observer
                
                android.util.Log.d("FullPlayerFragment", "✅ toggleLike() called");
            } else {
                android.util.Log.w("FullPlayerFragment", "❌ No song selected for like");
                showToast("No song selected");
            }
        });
        
        btnComments.setOnClickListener(v -> {
            // Navigate to comments screen
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

    private void observeViewModel() {
        // IMPORTANT: FullPlayerFragment is now a PASSIVE VIEW
        // It only observes and displays current state from singleton MediaPlayerRepository
        // It does NOT trigger new playback commands to avoid restarting music

        // Song data
        viewModel.getCurrentSong().observe(getViewLifecycleOwner(), song -> {
            if (song != null) {
                currentSong = song;
                updateSongInfo(song);
            }
        });

        // Artist data
        viewModel.getCurrentArtist().observe(getViewLifecycleOwner(), artist -> {
            if (artist != null) {
                currentArtist = artist;
                updateArtistInfo(artist);
            }
        });
        
        // Like status
        viewModel.getIsLiked().observe(getViewLifecycleOwner(), liked -> {
            if (liked != null) {
                isLiked = liked;
                updateLikeButton();
            }
        });

        // Follow status
        viewModel.getIsFollowing().observe(getViewLifecycleOwner(), following -> {
            if (following != null) {
                isFollowing = following;
                updateFollowButton();
            }
        });
        
        // Playing state
        viewModel.getIsPlaying().observe(getViewLifecycleOwner(), playing -> {
            if (playing != null) {
                isPlaying = playing;
                updatePlayPauseButton();
            }
        });

        // XÓA OBSERVER PROGRESS RIÊNG - chỉ dùng currentPosition
        viewModel.getCurrentPosition().observe(getViewLifecycleOwner(), positionMs -> {
            if (positionMs != null && !isUserSeeking && currentSong != null) {
                textCurrentTime.setText(formatTime(positionMs));

                // Tính progress từ position
                Long duration = viewModel.getDuration().getValue();
                if (duration != null && duration > 0) {
                    int progressPercent = (int) ((positionMs * 100) / duration);
                    seekbarProgress.setProgress(progressPercent);
                }
            }
        });

        // Duration observer
        viewModel.getDuration().observe(getViewLifecycleOwner(), durationMs -> {
            if (durationMs != null && durationMs > 0) {
                textTotalTime.setText(formatTime(durationMs));
            }
        });

        // Observe queue info to enable/disable navigation buttons
        viewModel.getQueueInfo().observe(getViewLifecycleOwner(), queueInfo -> {
            if (queueInfo != null) {
                // Enable/disable buttons based on queue state
                // Note: We always allow navigation (will restart/handle boundaries appropriately)
                btnPrevious.setEnabled(true); // Always enabled - handles 3-second logic
                btnNext.setEnabled(true);     // Always enabled - will restart if at end

                android.util.Log.d("FullPlayerFragment", "Queue updated: " +
                    queueInfo.getCurrentIndex() + "/" + queueInfo.getTotalSongs() +
                    " - " + queueInfo.getQueueTitle());
            }
        });
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

        // TODO: Load album art using image loading library (Glide/Coil)
        // For now, keep the placeholder
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

    // XÓA syncCurrentSongData() - THỪA vì data sync tự động qua Observer pattern

    /**
     * Update current time display based on seek bar progress (0-100)
     */
    private void updateCurrentTimeFromProgress(int progressPercent) {
        // Estimate time based on progress percentage and total duration
        if (currentSong != null && currentSong.getDurationMs() != null) {
            long totalDurationMs = currentSong.getDurationMs();
            long estimatedPositionMs = (totalDurationMs * progressPercent) / 100;
            textCurrentTime.setText(formatTime(estimatedPositionMs));
        }
    }

    /**
     * Helper method để format thời gian một cách consistent
     */
    private String formatTime(long timeMs) {
        if (timeMs <= 0) {
            return "0:00";
        }
        return TimeUtils.formatDuration((int) timeMs);
    }

    private void navigateToComments() {
        if (getActivity() != null && currentSong != null) {
            // Create and show comments fragment
            CommentsFragment commentsFragment = CommentsFragment.newInstance(currentSong.getId());

            // Set up comment change listener to refresh comment count
            commentsFragment.setCommentChangeListener(new CommentsFragment.CommentChangeListener() {
                @Override
                public void onCommentCountChanged() {
                    if (viewModel != null && currentSong != null) {
                        viewModel.refreshCommentCount(currentSong.getId());
                    }
                }
            });

            // SỬA LỖI: Sử dụng đúng container ID cho FullPlayerActivity
            int containerId = getActivity() instanceof FullPlayerActivity ?
                R.id.full_player_container : R.id.fragment_container;

            getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(containerId, commentsFragment)
                .addToBackStack(null)
                .commit();
        }
    }

    private void navigateToPlaylistSelection() {
        if (getContext() != null && currentSong != null) {
            Intent intent = PlaylistSelectionActivity.createIntent(getContext(), currentSong.getId());
            playlistSelectionLauncher.launch(intent);
        }
    }

    private void navigateToQueue() {
        if (getActivity() != null && currentSong != null) {
            // Create and show queue fragment
            QueueFragment queueFragment = QueueFragment.newInstance(currentSong.getId());

            // SỬA LỖI: Sử dụng đúng container ID cho FullPlayerActivity
            int containerId = getActivity() instanceof FullPlayerActivity ?
                R.id.full_player_container : R.id.fragment_container;

            getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(containerId, queueFragment)
                .addToBackStack(null)
                .commit();
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    // Xóa deprecated onActivityResult - đã thay thế bằng ActivityResultLauncher
}
