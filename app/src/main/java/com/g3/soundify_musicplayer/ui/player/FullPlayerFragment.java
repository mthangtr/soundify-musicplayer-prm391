package com.g3.soundify_musicplayer.ui.player;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.ui.playlist.PlaylistSelectionActivity;
import com.g3.soundify_musicplayer.utils.TimeUtils;

/**
 * Full Player Screen Fragment
 * UI ONLY - Uses mock data for testing, no backend integration
 */
public class FullPlayerFragment extends Fragment {

    private static final String ARG_SONG_ID = "song_id";
    private static final int REQUEST_CODE_PLAYLIST_SELECTION = 1001;
    
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
    private EditText editComment;
    private ImageButton btnLike;
    private ImageButton btnComments;
    private ImageButton btnAddToPlaylist;
    private ImageButton btnQueue;
    
    // ViewModel
    private FullPlayerViewModel viewModel;
    
    // Current song data
    private Song currentSong;
    private User currentArtist;
    private boolean isPlaying = false;
    private boolean isLiked = false;
    private boolean isFollowing = false;

    public static FullPlayerFragment newInstance(long songId) {
        FullPlayerFragment fragment = new FullPlayerFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SONG_ID, songId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FullPlayerViewModel.class);
        
        if (getArguments() != null) {
            long songId = getArguments().getLong(ARG_SONG_ID);
            viewModel.loadSong(songId);
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
        
        initializeViews(view);
        setupClickListeners();
        observeViewModel();
    }

    private void initializeViews(View view) {
        // Header components
        btnMinimize = view.findViewById(R.id.btn_minimize);
        textSongTitle = view.findViewById(R.id.text_song_title);
        textArtistName = view.findViewById(R.id.text_artist_name);
        btnFollow = view.findViewById(R.id.btn_follow);
        
        // Album art
        imageAlbumArt = view.findViewById(R.id.image_album_art);
        
        // Playback controls
        seekbarProgress = view.findViewById(R.id.seekbar_progress);
        textCurrentTime = view.findViewById(R.id.text_current_time);
        textTotalTime = view.findViewById(R.id.text_total_time);
        btnPrevious = view.findViewById(R.id.btn_previous);
        btnPlayPause = view.findViewById(R.id.btn_play_pause);
        btnNext = view.findViewById(R.id.btn_next);
        
        // Comment input
        editComment = view.findViewById(R.id.edit_comment);
        
        // Bottom action bar
        btnLike = view.findViewById(R.id.btn_like);
        btnComments = view.findViewById(R.id.btn_comments);
        btnAddToPlaylist = view.findViewById(R.id.btn_add_to_playlist);
        btnQueue = view.findViewById(R.id.btn_queue);
    }

    private void setupClickListeners() {
        // Header actions
        btnMinimize.setOnClickListener(v -> {
            // Show mini player with current song when minimizing
            if (currentSong != null && currentArtist != null) {
                MiniPlayerManager.getInstance().showMiniPlayer(currentSong, currentArtist);
            }
            showToast("Minimized to mini player");

            // Navigate to MainActivity to show mini player
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), com.g3.soundify_musicplayer.MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                getActivity().finish();
            }
        });
        
        btnFollow.setOnClickListener(v -> {
            if (currentArtist != null) {
                viewModel.toggleFollow(currentArtist.getId());
                showToast(isFollowing ? "Unfollowed artist" : "Following artist");
            }
        });
        
        // Playback controls
        btnPrevious.setOnClickListener(v -> {
            // TODO: Implement previous track
            showToast("Previous track");
        });
        
        btnPlayPause.setOnClickListener(v -> {
            // TODO: Implement play/pause
            togglePlayPause();
        });
        
        btnNext.setOnClickListener(v -> {
            // TODO: Implement next track
            showToast("Next track");
        });
        
        // Progress bar
        seekbarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // TODO: Implement seek functionality
                    updateCurrentTime(progress);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Comment input
        editComment.setOnEditorActionListener((v, actionId, event) -> {
            String comment = editComment.getText().toString().trim();
            if (!comment.isEmpty() && currentSong != null) {
                viewModel.addComment(currentSong.getId(), comment);
                editComment.setText("");
                showToast("Comment added!");
                return true;
            }
            return false;
        });
        
        // Bottom action bar
        btnLike.setOnClickListener(v -> {
            if (currentSong != null) {
                viewModel.toggleLike(currentSong.getId());
                showToast(isLiked ? "Removed from liked songs" : "Added to liked songs");
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
            // TODO: Implement queue view
            showToast("View queue");
        });
    }

    private void observeViewModel() {
        // Observe song data
        viewModel.getCurrentSong().observe(getViewLifecycleOwner(), song -> {
            if (song != null) {
                currentSong = song;
                updateSongInfo(song);
            }
        });
        
        // Observe artist data
        viewModel.getCurrentArtist().observe(getViewLifecycleOwner(), artist -> {
            if (artist != null) {
                currentArtist = artist;
                updateArtistInfo(artist);
            }
        });
        
        // Observe like status
        viewModel.getIsLiked().observe(getViewLifecycleOwner(), liked -> {
            isLiked = liked;
            updateLikeButton();
        });
        
        // Observe follow status
        viewModel.getIsFollowing().observe(getViewLifecycleOwner(), following -> {
            isFollowing = following;
            updateFollowButton();
        });
        
        // Observe playback state
        viewModel.getIsPlaying().observe(getViewLifecycleOwner(), playing -> {
            isPlaying = playing;
            updatePlayPauseButton();
        });
        
        // Observe progress
        viewModel.getProgress().observe(getViewLifecycleOwner(), progress -> {
            if (progress != null) {
                seekbarProgress.setProgress(progress);
            }
        });
        
        // Observe current time
        viewModel.getCurrentTime().observe(getViewLifecycleOwner(), time -> {
            if (time != null) {
                textCurrentTime.setText(TimeUtils.formatDuration(time));
            }
        });
    }

    private void updateSongInfo(Song song) {
        textSongTitle.setText(song.getTitle());
        
        // Update total time
        if (song.getDurationMs() != null && song.getDurationMs() > 0) {
            textTotalTime.setText(TimeUtils.formatDuration(song.getDurationMs()));
            seekbarProgress.setMax(song.getDurationMs());
        } else {
            textTotalTime.setText(getString(R.string.duration_unknown));
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

    private void togglePlayPause() {
        viewModel.togglePlayPause();
    }

    private void updateCurrentTime(int progress) {
        textCurrentTime.setText(TimeUtils.formatDuration(progress));
    }

    private void navigateToComments() {
        if (getActivity() != null && currentSong != null) {
            // Create and show comments fragment
            CommentsFragment commentsFragment = CommentsFragment.newInstance(currentSong.getId());

            getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, commentsFragment)
                .addToBackStack(null)
                .commit();
        }
    }

    private void navigateToPlaylistSelection() {
        if (getContext() != null && currentSong != null) {
            Intent intent = PlaylistSelectionActivity.createIntent(getContext(), currentSong.getId());
            startActivityForResult(intent, REQUEST_CODE_PLAYLIST_SELECTION);
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PLAYLIST_SELECTION && resultCode == getActivity().RESULT_OK) {
            if (data != null) {
                String playlistName = data.getStringExtra(PlaylistSelectionActivity.RESULT_PLAYLIST_NAME);
                if (playlistName != null) {
                    showToast("Added to " + playlistName);
                } else {
                    showToast("Added to playlist");
                }
            }
        }
    }
}
