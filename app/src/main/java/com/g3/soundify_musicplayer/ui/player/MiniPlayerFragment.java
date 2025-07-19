package com.g3.soundify_musicplayer.ui.player;

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

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;

/**
 * Mini Player Fragment - Persistent component that appears on all screens.
 * Provides basic playback controls and expands to full player when tapped.
 * UI ONLY - No backend integration, uses mock data for demo purposes.
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

    // ViewModel
    private MiniPlayerViewModel viewModel;

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
        viewModel = new ViewModelProvider(this).get(MiniPlayerViewModel.class);
    }

    private void setupClickListeners() {
        // Expand to full player when main area is tapped
        rootView.setOnClickListener(v -> expandToFullPlayer());

        // Play/Pause button
        btnPlayPause.setOnClickListener(v -> {
            viewModel.togglePlayPause();
            showToast(isPlaying ? "Paused" : "Playing");
        });

        // Next button
        btnNext.setOnClickListener(v -> {
            viewModel.playNext();
            showToast("Next track");
        });

        // Close button
        btnClose.setOnClickListener(v -> {
            viewModel.hideMiniPlayer();
            showToast("Mini player closed");
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
            if (artist != null) {
                currentArtist = artist;
                updateArtistInfo(artist);
            }
        });

        // Observe playing state
        viewModel.getIsPlaying().observe(getViewLifecycleOwner(), playing -> {
            if (playing != null) {
                isPlaying = playing;
                updatePlayPauseButton(playing);
            }
        });

        // Observe progress
        viewModel.getProgress().observe(getViewLifecycleOwner(), progress -> {
            if (progress != null && currentSong != null) {
                updateProgress(progress);
            }
        });
    }

    private void updateSongInfo(Song song) {
        textSongTitle.setText(song.getTitle());
        // TODO: Load album art using image loading library (Glide/Coil)
        // For now, keep the placeholder
    }

    private void updateArtistInfo(User artist) {
        String displayName = artist.getDisplayName();
        String username = artist.getUsername();
        textArtistName.setText(displayName != null && !displayName.isEmpty() ? displayName : username);
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        if (isPlaying) {
            btnPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_play);
        }
    }

    private void updateProgress(int progressMs) {
        if (currentSong != null && currentSong.getDurationMs() != null && currentSong.getDurationMs() > 0) {
            int progressPercent = (int) ((progressMs * 100.0) / currentSong.getDurationMs());
            progressBar.setProgress(progressPercent);
        }
    }

    private void expandToFullPlayer() {
        if (getContext() != null && currentSong != null) {
            // Navigate to PlayerDemoActivity
            Intent intent = new Intent(getContext(), PlayerDemoActivity.class);
            intent.putExtra("song_id", currentSong.getId());
            startActivity(intent);
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
