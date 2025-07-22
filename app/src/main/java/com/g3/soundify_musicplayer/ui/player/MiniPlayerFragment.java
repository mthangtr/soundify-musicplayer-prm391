package com.g3.soundify_musicplayer.ui.player;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;


/**
 * Mini Player Fragment - Persistent component that appears on all screens.
 * Provides basic playback controls and expands to full player when tapped.
 * Sử dụng SongDetailViewModel để kết nối với backend thật qua MediaPlaybackService.
 */
public class MiniPlayerFragment extends Fragment {

    // UI Components
    private View rootView;
    private ImageView imageAlbumArt;
    private TextView textSongTitle;
    private TextView textArtistName;
    private ImageButton btnPlayPause;
    private ImageButton btnNext;
    private ImageButton btnClose;
    private ProgressBar progressBar;

    // ViewModel THỐNG NHẤT
    private SongDetailViewModel viewModel;

    // Current data
    private Song currentSong;
    private User currentArtist;
    private boolean isPlaying = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mini_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        rootView = view;
        initViews(view);
        setupViewModel();
        setupClickListeners();
        observeViewModel();
    }

    private void initViews(View view) {
        imageAlbumArt = view.findViewById(R.id.image_album_art);
        textSongTitle = view.findViewById(R.id.text_song_title);
        textArtistName = view.findViewById(R.id.text_artist_name);
        btnPlayPause = view.findViewById(R.id.btn_play_pause);
        btnNext = view.findViewById(R.id.btn_next);
        btnClose = view.findViewById(R.id.btn_close);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupViewModel() {
        // Sử dụng SongDetailViewModel THỐNG NHẤT cho cả Mini và Full Player
        viewModel = new ViewModelProvider(requireActivity()).get(SongDetailViewModel.class);
    }

    private void setupClickListeners() {
        // Expand to full player when main area is tapped
        rootView.setOnClickListener(v -> expandToFullPlayer());

        // Play/Pause button
        btnPlayPause.setOnClickListener(v -> {
            // ✅ SAFE: Add error handling for play/pause
            try {
                viewModel.togglePlayPause();
                showToast(isPlaying ? "Paused" : "Playing");
            } catch (Exception e) {
                android.util.Log.e("MiniPlayerFragment", "Error during play/pause", e);
                showToast("Playback error occurred");
            }
        });

        // Next button
        btnNext.setOnClickListener(v -> {
            // ✅ SAFE: Add error handling for next track
            try {
                viewModel.playNext();
                showToast("Next track");
            } catch (Exception e) {
                android.util.Log.e("MiniPlayerFragment", "Error during next track", e);
                showToast("Navigation error occurred");
            }
        });

        // Close button
        btnClose.setOnClickListener(v -> {
            // ✅ SAFE: Add error handling for close
            try {
                viewModel.hideMiniPlayer();
                showToast("Mini player closed");
            } catch (Exception e) {
                android.util.Log.e("MiniPlayerFragment", "Error closing mini player", e);
                // Fallback: hide view directly
                if (rootView != null) {
                    rootView.setVisibility(View.GONE);
                }
            }
        });
    }

    private void observeViewModel() {
        // Observe visibility
        viewModel.getIsVisible().observe(getViewLifecycleOwner(), isVisible -> {
            if (isVisible != null) {
                rootView.setVisibility(isVisible ? View.VISIBLE : View.GONE);
            }
        });

        // Observe current song
        viewModel.getCurrentSong().observe(getViewLifecycleOwner(), song -> {
            if (song != null) {
                currentSong = song;
                updateSongInfo(song);
            }
        });

        // Observe current artist
        viewModel.getCurrentArtist().observe(getViewLifecycleOwner(), artist -> {
            android.util.Log.d("MiniPlayerFragment", "Artist changed to: " +
                (artist != null ? artist.getDisplayName() + " (" + artist.getUsername() + ")" : "NULL"));
            if (artist != null) {
                currentArtist = artist;
                updateArtistInfo(artist);
            } else {
                android.util.Log.w("MiniPlayerFragment", "Artist is NULL - not updating UI");
            }
        });

        // Observe playing state
        viewModel.getIsPlaying().observe(getViewLifecycleOwner(), playing -> {
            if (playing != null) {
                isPlaying = playing;
                updatePlayPauseButton(playing);
            }
        });

        // XÓA OBSERVER PROGRESS RIÊNG - chỉ dùng currentPosition
        viewModel.getCurrentPosition().observe(getViewLifecycleOwner(), positionMs -> {
            if (positionMs != null && currentSong != null) {
                // Tính progress từ position
                Long duration = viewModel.getDuration().getValue();
                if (duration != null && duration > 0) {
                    int progressPercent = (int) ((positionMs * 100) / duration);
                    updateProgress(progressPercent);
                }
            }
        });

        // Observe queue info to enable/disable navigation buttons
        viewModel.getQueueInfo().observe(getViewLifecycleOwner(), queueInfo -> {
            if (queueInfo != null) {
                // Enable/disable Next button based on queue state
                // Note: We always allow Next (it will restart current song if at end)
                // But we can show visual feedback about queue state
                btnNext.setEnabled(true); // Always enabled - will restart if needed
            }
        });
    }

    private void updateSongInfo(Song song) {
        textSongTitle.setText(song.getTitle());

        // Load cover art using Glide
        if (song.getCoverArtUrl() != null && !song.getCoverArtUrl().isEmpty()) {
            Glide.with(imageAlbumArt.getContext())
                    .load(song.getCoverArtUrl())
                    .placeholder(R.drawable.splashi_icon)
                    .error(R.drawable.splashi_icon)
                    .into(imageAlbumArt);
        } else {
            imageAlbumArt.setImageResource(R.drawable.splashi_icon);
        }
    }

    private void updateArtistInfo(User artist) {
        String displayName = artist.getDisplayName();
        String username = artist.getUsername();
        String finalName = displayName != null && !displayName.isEmpty() ? displayName : username;

        android.util.Log.d("MiniPlayerFragment", "updateArtistInfo called - displayName: " + displayName +
            ", username: " + username + ", finalName: " + finalName);

        textArtistName.setText(finalName);

        android.util.Log.d("MiniPlayerFragment", "Artist name set to TextView: " + finalName);
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        if (isPlaying) {
            btnPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_play);
        }
    }

    private void updateProgress(int progressPercent) {
        progressBar.setProgress(progressPercent);
    }

    // Xóa updateProgressFromPosition - không sử dụng

    private void expandToFullPlayer() {
        if (getActivity() == null || currentSong == null) {
            android.util.Log.w("MiniPlayerFragment", "Cannot expand to full player - activity or song is null");
            return;
        }

        try {
            Intent intent = FullPlayerActivity.createIntent(getActivity(), currentSong.getId());
            try {
                ActivityOptions options = ActivityOptions.makeCustomAnimation(
                    getContext(), R.anim.slide_up_in, R.anim.fade_in);
                startActivity(intent, options.toBundle());
            } catch (Exception animationError) {
                startActivity(intent);
            }

        } catch (Exception e) {
            showToast("Cannot open full player");
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();

        if (viewModel != null) {
            viewModel.resumeUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // ✅ IMPORTANT: Don't pause updates here unless activity is finishing
        // MiniPlayer should continue updating even when FullPlayer is open
        // Only pause if parent activity is actually finishing
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
