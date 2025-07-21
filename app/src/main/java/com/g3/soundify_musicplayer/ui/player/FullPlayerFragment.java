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
import com.g3.soundify_musicplayer.data.model.NavigationContext;
import com.g3.soundify_musicplayer.ui.player.comment.CommentsFragment;
import com.g3.soundify_musicplayer.ui.player.queue.QueueFragment;
import com.g3.soundify_musicplayer.ui.playlist.PlaylistSelectionActivity;
import com.g3.soundify_musicplayer.utils.TimeUtils;

/**
 * Full Player Screen Fragment
 * UI ONLY - Uses mock data for testing, no backend integration
 */
public class FullPlayerFragment extends Fragment {

    private static final String ARG_SONG_ID = "song_id";
    private static final String ARG_NAVIGATION_CONTEXT = "navigation_context";
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

    public static FullPlayerFragment newInstanceWithContext(long songId, NavigationContext context) {
        FullPlayerFragment fragment = new FullPlayerFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SONG_ID, songId);
        args.putSerializable(ARG_NAVIGATION_CONTEXT, context);
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
            // Data sẽ được sync qua syncCurrentSongData() và observers
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

        // SỬA LỖI: Load song detail data để đảm bảo like status và progress được đồng bộ
        if (getArguments() != null) {
            long songId = getArguments().getLong(ARG_SONG_ID);
            android.util.Log.d("FullPlayerFragment", "Loading song detail for ID: " + songId);
            // Load song detail để có like status và thiết lập service connection
            viewModel.loadSongDetail(songId, 1L); // Default userId = 1
        }

        // Sync current data từ service (nếu đã có)
        syncCurrentSongData();

        android.util.Log.d("FullPlayerFragment", "FullPlayer UI setup completed");
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
        // SỬA LỖI: Khởi tạo SeekBar với max = 100 để sử dụng percentage system
        android.util.Log.d("FullPlayerFragment", "Initializing SeekBar - current max: " +
            seekbarProgress.getMax() + ", setting to 100");
        seekbarProgress.setMax(100);
        seekbarProgress.setProgress(0);

        textCurrentTime = view.findViewById(R.id.text_current_time);
        textTotalTime = view.findViewById(R.id.text_total_time);

        // Khởi tạo với giá trị mặc định
        textCurrentTime.setText("0:00");
        textTotalTime.setText("--:--");
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
            // SỬA LỖI: Sử dụng FullPlayerActivity.minimizeToMiniPlayer()
            // thay vì tạo Intent mới
            showToast("Minimized to mini player");

            if (getActivity() instanceof FullPlayerActivity) {
                ((FullPlayerActivity) getActivity()).minimizeToMiniPlayer();
            } else {
                // Fallback for fragment-based implementation
                if (getActivity() != null) {
                    getActivity().onBackPressed();
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
            // Sử dụng viewModel (cùng instance với MiniPlayer)
            viewModel.seekToPercentage(0);
            showToast("Song restarted");
        });

        btnPlayPause.setOnClickListener(v -> {
            // Sử dụng viewModel (cùng instance với MiniPlayer)
            viewModel.togglePlayPause();
        });

        btnNext.setOnClickListener(v -> {
            showToast("Next track not available (single song mode)");
        });
        
        // Progress bar - Seek functionality
        seekbarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                android.util.Log.d("FullPlayerFragment", "onProgressChanged: progress=" + progress +
                    ", fromUser=" + fromUser + ", max=" + seekBar.getMax());
                if (fromUser) {
                    // Cập nhật UI ngay lập tức khi user drag
                    updateCurrentTimeFromProgress(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // User bắt đầu drag - tạm dừng auto update
                android.util.Log.d("FullPlayerFragment", "onStartTrackingTouch: max=" + seekBar.getMax());
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // User thả tay - seek đến vị trí mới
                int progress = seekBar.getProgress();
                android.util.Log.d("FullPlayerFragment", "onStopTrackingTouch: progress=" + progress +
                    ", max=" + seekBar.getMax());

                // Sử dụng viewModel (cùng instance với MiniPlayer)
                viewModel.seekToPercentage(progress);
                showToast("Seeked to " + progress + "%");
                android.util.Log.d("FullPlayerFragment", "Seeking to " + progress + "%");

                // Cho phép auto update trở lại
                isUserSeeking = false;
            }
        });
        
        // Bottom action bar - Like button với visual feedback
        btnLike.setOnClickListener(v -> {
            if (currentSong != null) {
                // SỬA LỖI: Sử dụng method signature đúng (không cần parameter)
                viewModel.toggleLike();
                // Visual feedback sẽ được update qua observer
            } else {
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
        // COPY CHÍNH XÁC PATTERN TỪ MINIPLAYER

        // Observe current song (giống MiniPlayer)
        viewModel.getCurrentSong().observe(getViewLifecycleOwner(), song -> {
            android.util.Log.d("FullPlayerFragment", "Song changed to: " +
                (song != null ? song.getTitle() : "NULL"));
            if (song != null) {
                currentSong = song;
                updateSongInfo(song);
            }
        });

        // Observe current artist (giống MiniPlayer)
        viewModel.getCurrentArtist().observe(getViewLifecycleOwner(), artist -> {
            android.util.Log.d("FullPlayerFragment", "Artist changed to: " +
                (artist != null ? artist.getDisplayName() : "NULL"));
            if (artist != null) {
                currentArtist = artist;
                updateArtistInfo(artist);
            }
        });
        
        // Observe like status từ viewModel (đơn giản hóa)
        viewModel.getIsLiked().observe(getViewLifecycleOwner(), liked -> {
            boolean wasLiked = isLiked;
            isLiked = liked;
            updateLikeButton();

            // Show toast feedback (chỉ khi có thay đổi, không phải lần đầu load)
            if (wasLiked != liked && currentSong != null) {
                showToast(liked ? "Added to liked songs" : "Removed from liked songs");
            }
        });

        // Observe follow status từ viewModel (đơn giản hóa)
        viewModel.getIsFollowing().observe(getViewLifecycleOwner(), following -> {
            boolean wasFollowing = isFollowing;
            isFollowing = following;
            updateFollowButton();

            // Show toast feedback (chỉ khi có thay đổi, không phải lần đầu load)
            if (wasFollowing != following && currentArtist != null) {
                showToast(following ? "Following " + currentArtist.getDisplayName() :
                    "Unfollowed " + currentArtist.getDisplayName());
            }
        });
        
        // Observe playing state (CHÍNH XÁC như MiniPlayer)
        viewModel.getIsPlaying().observe(getViewLifecycleOwner(), playing -> {
            android.util.Log.d("FullPlayerFragment", "Playing state changed to: " + playing);
            if (playing != null) {
                isPlaying = playing;
                updatePlayPauseButton();
            }
        });

        // Observe progress (CHÍNH XÁC như MiniPlayer)
        viewModel.getProgress().observe(getViewLifecycleOwner(), progress -> {
            android.util.Log.d("FullPlayerFragment", "Progress changed to: " + progress + "%");
            if (progress != null && currentSong != null && !isUserSeeking) {
                updateProgress(progress);
            }
        });

        // Observe current position từ viewModel
        viewModel.getCurrentPosition().observe(getViewLifecycleOwner(), positionMs -> {
            if (positionMs != null && !isUserSeeking) {
                textCurrentTime.setText(formatTime(positionMs));
                android.util.Log.d("FullPlayerFragment", "Position update: " + formatTime(positionMs) +
                    " (" + (positionMs / 1000) + "s)");
            }
        });

        // Observe duration từ viewModel
        viewModel.getDuration().observe(getViewLifecycleOwner(), durationMs -> {
            if (durationMs != null && durationMs > 0) {
                textTotalTime.setText(formatTime(durationMs));
                android.util.Log.d("FullPlayerFragment", "Duration update: " + formatTime(durationMs) +
                    " (" + (durationMs / 1000) + "s)");
            }
        });
    }

    private void updateSongInfo(Song song) {
        android.util.Log.d("FullPlayerFragment", "updateSongInfo called for song: " + song.getTitle());
        textSongTitle.setText(song.getTitle());

        // Update total time
        if (song.getDurationMs() != null && song.getDurationMs() > 0) {
            textTotalTime.setText(formatTime(song.getDurationMs()));
            // SỬA LỖI: Set max = 100 để sử dụng percentage system (0-100)
            android.util.Log.d("FullPlayerFragment", "Setting seekBar max to 100 (was " +
                seekbarProgress.getMax() + "), duration: " + formatTime(song.getDurationMs()));
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

    /**
     * Sync song data ngay lập tức từ mediaViewModel khi mở FullPlayer
     */
    private void syncCurrentSongData() {
        // Lấy current values từ viewModel (cùng instance với MiniPlayer)
        Song song = viewModel.getCurrentSong().getValue();
        User artist = viewModel.getCurrentArtist().getValue();
        Boolean playing = viewModel.getIsPlaying().getValue();
        Long currentPosition = viewModel.getCurrentPosition().getValue();
        Long duration = viewModel.getDuration().getValue();
        Integer progress = viewModel.getProgress().getValue();

        android.util.Log.d("FullPlayerFragment", "Syncing current data - Song: " +
            (song != null ? song.getTitle() : "NULL") +
            ", Artist: " + (artist != null ? artist.getDisplayName() : "NULL") +
            ", Playing: " + playing + ", Progress: " + progress + "%");

        // Update UI ngay lập tức nếu có data
        if (song != null) {
            currentSong = song;
            updateSongInfo(song);
        }

        if (artist != null) {
            currentArtist = artist;
            updateArtistInfo(artist);
        }

        if (playing != null) {
            isPlaying = playing;
            updatePlayPauseButton();
        }

        // Sync thời gian và progress
        if (currentPosition != null) {
            textCurrentTime.setText(formatTime(currentPosition));
        }

        if (duration != null && duration > 0) {
            textTotalTime.setText(formatTime(duration));
        }

        if (progress != null) {
            seekbarProgress.setProgress(progress);
        }
    }

    /**
     * Update progress bar (CHÍNH XÁC như MiniPlayer)
     */
    private void updateProgress(int progressPercent) {
        android.util.Log.d("FullPlayerFragment", "updateProgress called with: " + progressPercent +
            "%, isUserSeeking=" + isUserSeeking + ", seekBarMax=" + seekbarProgress.getMax());
        seekbarProgress.setProgress(progressPercent);
    }

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
            commentsFragment.setCommentChangeListener(() -> {
                if (viewModel != null && currentSong != null) {
                    viewModel.refreshCommentCount(currentSong.getId());
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
            startActivityForResult(intent, REQUEST_CODE_PLAYLIST_SELECTION);
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
