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
 */
public class MiniPlayerFragment extends Fragment {

    private View rootView;
    private ImageView imageAlbumArt;
    private TextView textSongTitle;
    private TextView textArtistName;
    private ImageButton btnPlayPause;
    private ImageButton btnNext;
    private ImageButton btnClose;
    private ProgressBar progressBar;

    private SongDetailViewModel viewModel;

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
        viewModel = new ViewModelProvider(requireActivity()).get(SongDetailViewModel.class);
    }

    private void setupClickListeners() {
        // Click để chuyển sang FullPlayer
        rootView.setOnClickListener(v -> expandToFullPlayer());

        // Play/Pause button
        btnPlayPause.setOnClickListener(v -> {
            try {
                viewModel.togglePlayPause();
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
        // lắng nghe trạng thái hiển thị của mini player ở trong ViewModel
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
                btnNext.setEnabled(true);
            }
        });
    }

    private void updateSongInfo(Song song) {
        textSongTitle.setText(song.getTitle());
        textArtistName.setText(song.getUploaderName());

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
        textArtistName.setText(finalName);
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

    private void expandToFullPlayer() {
        if (getActivity() == null || currentSong == null) {
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
        //Khi user mở  FullPlayerActivity từ  MiniPlayerFragment, fragment sẽ gọi onPause() nhưng KHÔNG nên dừng progress updates.
        //MiniPlayer vẫn cần hoạt động khi FullPlayer mở
        //User có thể minimize FullPlayer về MiniPlayer bất cứ lúc nào
        //Progress bar phải tiếp tục cập nhật để sync với nhạc đang phát
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
