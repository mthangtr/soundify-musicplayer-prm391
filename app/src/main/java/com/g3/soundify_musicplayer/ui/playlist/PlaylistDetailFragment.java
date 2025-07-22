package com.g3.soundify_musicplayer.ui.playlist;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;

import com.g3.soundify_musicplayer.ui.player.SongDetailViewModel;
import com.g3.soundify_musicplayer.ui.player.SongDetailViewModelFactory;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fragment for displaying playlist details and managing songs
 */
public class PlaylistDetailFragment extends Fragment implements PlaylistSongCardAdapter.OnSongActionListener {

    private PlaylistDetailViewModel viewModel;
    private SongDetailViewModel songDetailViewModel;
    private PlaylistSongCardAdapter adapter;
    
    // UI Components
    private ShapeableImageView playlistCover;
    private TextView playlistName;
    private TextView playlistInfo;
    private Button playAllButton;
    private Button shuffleButton;
    private Button editPlaylistButton;
    private TextView songsHeader;
    private RecyclerView songsRecyclerView;
    private LinearLayout emptyStateLayout;
    
    // Constants
    private static final String ARG_PLAYLIST_ID = "playlist_id";
    
    // Activity result launchers
    private ActivityResultLauncher<Intent> songSelectionLauncher;
    
    /**
     * Create new instance of PlaylistDetailFragment
     */
    public static PlaylistDetailFragment newInstance(long playlistId) {
        PlaylistDetailFragment fragment = new PlaylistDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PLAYLIST_ID, playlistId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ViewModels
        viewModel = new ViewModelProvider(this).get(PlaylistDetailViewModel.class);

        // ✅ CRITICAL: Initialize SongDetailViewModel with proper factory for mini player integration
        SongDetailViewModelFactory factory = new SongDetailViewModelFactory(requireActivity().getApplication());
        songDetailViewModel = new ViewModelProvider(requireActivity(), factory).get(SongDetailViewModel.class);

        // Initialize activity result launchers
        initializeActivityResultLaunchers();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist_detail, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI
        initializeViews(view);
        setupRecyclerView();
        setupClickListeners();

        // FIX: Handle arguments BEFORE setting up observers to avoid race condition
        handleArguments();
        setupObservers();
    }
    
    /**
     * Initialize all UI components
     */
    private void initializeViews(View view) {
        playlistCover = view.findViewById(R.id.image_view_playlist_cover);
        playlistName = view.findViewById(R.id.text_view_playlist_name);
        playlistInfo = view.findViewById(R.id.text_view_playlist_info);
        playAllButton = view.findViewById(R.id.button_play_all);
        shuffleButton = view.findViewById(R.id.button_shuffle);
        editPlaylistButton = view.findViewById(R.id.button_edit_playlist);
        songsHeader = view.findViewById(R.id.text_view_songs_header);
        songsRecyclerView = view.findViewById(R.id.recycler_view_songs);
        emptyStateLayout = view.findViewById(R.id.layout_empty_state);
    }
    
    /**
     * Setup RecyclerView with adapter
     */
    private void setupRecyclerView() {
        if (getContext() == null) return;

        adapter = new PlaylistSongCardAdapter(getContext());
        adapter.setOnSongActionListener(this);

        songsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        songsRecyclerView.setAdapter(adapter);
    }
    
    /**
     * Setup click listeners for all interactive elements
     */
    private void setupClickListeners() {
        playAllButton.setOnClickListener(v -> playAllSongs());
        shuffleButton.setOnClickListener(v -> shufflePlaySongs());
        editPlaylistButton.setOnClickListener(v -> showEditPlaylistDialog());
    }
    
    /**
     * Setup observers for ViewModel LiveData
     */
    private void setupObservers() {
        // Observe current playlist
        viewModel.getCurrentPlaylist().observe(getViewLifecycleOwner(), playlist -> {
            android.util.Log.d("PlaylistDetailFragment", "Playlist observed: " + (playlist != null ? playlist.getName() : "null"));
            updatePlaylistInfo(playlist);
        });

        // Observe playlist songs
        viewModel.getSongsInPlaylist().observe(getViewLifecycleOwner(), songs -> {
            android.util.Log.d("PlaylistDetailFragment", "Songs observed: " + (songs != null ? songs.size() + " songs" : "null"));
            updateSongsList(songs);
        });

        // Observe owner status
        viewModel.getIsOwner().observe(getViewLifecycleOwner(), isOwner -> {
            android.util.Log.d("PlaylistDetailFragment", "Owner status observed: " + isOwner);
            if (isOwner != null) {
                editPlaylistButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);

                // Update adapter remove option visibility
                if (adapter != null) {
                    adapter.setShowRemoveOption(isOwner);
                    android.util.Log.d("PlaylistDetailFragment", "Updated adapter remove option: " + isOwner);
                }
            }
        });

        // Observe playlist owner info
        viewModel.getPlaylistOwner().observe(getViewLifecycleOwner(), owner -> {
            android.util.Log.d("PlaylistDetailFragment", "Playlist owner observed: " + (owner != null ? owner.getDisplayName() : "null"));
            updatePlaylistInfoText(); // Refresh info when owner is loaded
        });

        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            android.util.Log.d("PlaylistDetailFragment", "Loading state observed: " + isLoading);
            // Handle loading state if needed
        });

        // Observe error messages
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                android.util.Log.e("PlaylistDetailFragment", "Error message observed: " + errorMessage);
                showToast(errorMessage);
                viewModel.clearErrorMessage(); // Clear after showing
            }
        });
        
        // Observe error messages
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showToast(errorMessage);
            }
        });
        
        // Observe success messages
        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), successMessage -> {
            if (successMessage != null && !successMessage.isEmpty()) {
                showToast(successMessage);
            }
        });
    }
    
    /**
     * Handle fragment arguments to load playlist
     */
    private void handleArguments() {
        Bundle args = getArguments();

        if (args != null) {
            long playlistId = args.getLong(ARG_PLAYLIST_ID, -1);

            if (playlistId != -1) {
                viewModel.loadPlaylist(playlistId);
            } else {
                showToast("Invalid playlist ID");
                // Navigate back or handle error
            }
        }
    }


    
    /**
     * Initialize activity result launchers
     */
    private void initializeActivityResultLaunchers() {
        // Song selection launcher
        songSelectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    // Handle selected songs
                    handleSongSelectionResult(result.getData());
                }
            }
        );
    }
    
    /**
     * Update playlist information display
     */
    private void updatePlaylistInfo(Playlist playlist) {
        if (playlist == null) return;
        
        playlistName.setText(playlist.getName());
        // Update playlist info will be handled by updatePlaylistInfoText()
    }
    
    /**
     * Update songs list and stats
     */
    private void updateSongsList(List<Song> songs) {
        android.util.Log.d("PlaylistDetailFragment", "updateSongsList called with " + (songs != null ? songs.size() + " songs" : "null songs"));

        // Update adapter
        if (adapter != null) {
            adapter.setSongs(songs);
            android.util.Log.d("PlaylistDetailFragment", "Adapter updated with songs");
        } else {
            android.util.Log.e("PlaylistDetailFragment", "Adapter is null when trying to update songs");
        }

        // Update stats
        viewModel.updatePlaylistStats(songs);
        updatePlaylistInfoText();

        // Show/hide empty state
        boolean isEmpty = songs == null || songs.isEmpty();
        android.util.Log.d("PlaylistDetailFragment", "Songs list isEmpty: " + isEmpty);

        emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        songsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        // Update action buttons state
        playAllButton.setEnabled(!isEmpty);
        shuffleButton.setEnabled(!isEmpty);
        android.util.Log.d("PlaylistDetailFragment", "Action buttons enabled: " + !isEmpty);

        // Update adapter remove option visibility based on owner status
        Boolean isOwner = viewModel.getIsOwner().getValue();
        if (adapter != null && isOwner != null) {
            adapter.setShowRemoveOption(isOwner);
            android.util.Log.d("PlaylistDetailFragment", "Remove option visibility: " + isOwner);
        }
    }
    
    /**
     * Update playlist info text with stats
     */
    private void updatePlaylistInfoText() {
        Playlist playlist = viewModel.getCurrentPlaylist().getValue();
        if (playlist == null) return;

        // Get owner name from ViewModel
        User owner = viewModel.getPlaylistOwner().getValue();
        String ownerName = "Unknown";
        if (owner != null) {
            ownerName = owner.getDisplayName();
            if (ownerName == null || ownerName.trim().isEmpty()) {
                ownerName = owner.getUsername();
            }
            if (ownerName == null || ownerName.trim().isEmpty()) {
                ownerName = "Unknown";
            }
        }

        int songCount = adapter.getItemCount();

        // Format: "Created by [owner] • [X] songs" (removed duration)
        String infoText = String.format("Created by %s • %d songs", ownerName, songCount);
        playlistInfo.setText(infoText);

        // Update songs header
        String songsHeaderText = songCount == 1 ? "1 song" : songCount + " songs";
        songsHeader.setText(songsHeaderText);
    }
    
    /**
     * Play all songs in playlist
     */
    private void playAllSongs() {
        android.util.Log.d("PlaylistDetailFragment", "playAllSongs called");

        // Validation with detailed logging
        if (adapter == null) {
            android.util.Log.e("PlaylistDetailFragment", "Adapter is null");
            showToast("Lỗi: Adapter chưa được khởi tạo");
            return;
        }

        if (adapter.getItemCount() == 0) {
            android.util.Log.e("PlaylistDetailFragment", "No songs in adapter");
            showToast("Không có bài hát để phát");
            return;
        }

        List<Song> playlistSongs = adapter.getSongs();
        Playlist currentPlaylist = viewModel.getCurrentPlaylist().getValue();

        android.util.Log.d("PlaylistDetailFragment", "Playlist songs count: " + playlistSongs.size());
        android.util.Log.d("PlaylistDetailFragment", "Current playlist: " + (currentPlaylist != null ? currentPlaylist.getName() : "null"));

        if (currentPlaylist != null && !playlistSongs.isEmpty()) {
            try {
                // ✅ CONSISTENT: Use playFromView with full playlist for navigation
                songDetailViewModel.playFromView(playlistSongs, currentPlaylist.getName(), 0);
                showToast("Đang phát tất cả bài hát từ: " + currentPlaylist.getName());
                android.util.Log.d("PlaylistDetailFragment", "Successfully started playing all songs");
            } catch (Exception e) {
                android.util.Log.e("PlaylistDetailFragment", "Error playing all songs", e);
                showToast("Lỗi khi phát playlist: " + e.getMessage());
            }
        } else {
            android.util.Log.e("PlaylistDetailFragment", "Cannot play - playlist or songs invalid");
            showToast("Không thể phát playlist - dữ liệu không hợp lệ");
        }
    }

    /**
     * Shuffle and play songs in playlist
     */
    private void shufflePlaySongs() {
        android.util.Log.d("PlaylistDetailFragment", "shufflePlaySongs called");

        // Validation with detailed logging
        if (adapter == null) {
            android.util.Log.e("PlaylistDetailFragment", "Adapter is null for shuffle");
            showToast("Lỗi: Adapter chưa được khởi tạo");
            return;
        }

        if (adapter.getItemCount() == 0) {
            android.util.Log.e("PlaylistDetailFragment", "No songs to shuffle");
            showToast("Không có bài hát để shuffle");
            return;
        }

        List<Song> playlistSongs = new ArrayList<>(adapter.getSongs());
        Playlist currentPlaylist = viewModel.getCurrentPlaylist().getValue();

        android.util.Log.d("PlaylistDetailFragment", "Songs to shuffle: " + playlistSongs.size());
        android.util.Log.d("PlaylistDetailFragment", "Current playlist for shuffle: " + (currentPlaylist != null ? currentPlaylist.getName() : "null"));

        if (currentPlaylist != null && !playlistSongs.isEmpty()) {
            try {
                // Shuffle the songs list
                Collections.shuffle(playlistSongs);
                android.util.Log.d("PlaylistDetailFragment", "Songs shuffled successfully");

                // ✅ CONSISTENT: Use playFromView with shuffled playlist
                songDetailViewModel.playFromView(playlistSongs, currentPlaylist.getName() + " (Shuffled)", 0);
                showToast("Đang shuffle và phát: " + currentPlaylist.getName());
                android.util.Log.d("PlaylistDetailFragment", "Successfully started shuffled playback");
            } catch (Exception e) {
                android.util.Log.e("PlaylistDetailFragment", "Error shuffling playlist", e);
                showToast("Lỗi khi shuffle playlist: " + e.getMessage());
            }
        } else {
            android.util.Log.e("PlaylistDetailFragment", "Cannot shuffle - playlist or songs invalid");
            showToast("Không thể shuffle playlist - dữ liệu không hợp lệ");
        }
    }
    
    /**
     * Show edit playlist dialog
     */
    private void showEditPlaylistDialog() {
        if (getContext() == null) return;

        Playlist currentPlaylist = viewModel.getCurrentPlaylist().getValue();
        if (currentPlaylist == null) {
            showToast("Playlist not loaded");
            return;
        }

        android.util.Log.d("PlaylistDetailFragment", "Showing edit dialog for playlist: " + currentPlaylist.getName());

        // Inflate dialog layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_playlist, null);

        // Get views
        TextInputLayout textInputLayout = dialogView.findViewById(R.id.text_input_layout_playlist_name);
        TextInputEditText editTextName = dialogView.findViewById(R.id.edit_text_playlist_name);
        Button buttonCancel = dialogView.findViewById(R.id.button_cancel);
        Button buttonSave = dialogView.findViewById(R.id.button_save);

        // Set current playlist name
        editTextName.setText(currentPlaylist.getName());
        editTextName.setSelection(editTextName.getText().length()); // Move cursor to end

        // Create dialog
        AlertDialog dialog = new MaterialAlertDialogBuilder(getContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Setup text watcher for validation
        editTextName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePlaylistName(s.toString(), textInputLayout, buttonSave);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup button listeners
        buttonCancel.setOnClickListener(v -> {
            android.util.Log.d("PlaylistDetailFragment", "Edit dialog cancelled");
            dialog.dismiss();
        });

        buttonSave.setOnClickListener(v -> {
            String newName = editTextName.getText().toString().trim();
            android.util.Log.d("PlaylistDetailFragment", "Saving playlist with new name: " + newName);

            if (validatePlaylistName(newName, textInputLayout, buttonSave)) {
                // Update playlist name
                viewModel.updatePlaylistName(newName);
                dialog.dismiss();
                showToast(getString(R.string.playlist_updated_successfully));
            }
        });

        // Initial validation
        validatePlaylistName(currentPlaylist.getName(), textInputLayout, buttonSave);

        dialog.show();

        // Focus on text field and show keyboard
        editTextName.requestFocus();
        if (getActivity() != null) {
            android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editTextName, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }
    

    
    /**
     * Handle song selection result
     */
    private void handleSongSelectionResult(Intent data) {
        // TODO: Handle selected songs from song selection activity
    }

    /**
     * Show toast message
     */
    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    // ========== LIFECYCLE METHODS FOR MINI PLAYER INTEGRATION ==========

    @Override
    public void onResume() {
        super.onResume();

        // ✅ SAFE: Resume any paused operations if needed
        // Mini player state is managed by BaseActivity, no action needed here
        android.util.Log.d("PlaylistDetailFragment", "onResume() - Mini player integration active");
    }

    @Override
    public void onPause() {
        super.onPause();

        // ✅ SAFE: Pause any ongoing operations if needed
        // Mini player continues playing in background via MediaPlayerRepository singleton
        android.util.Log.d("PlaylistDetailFragment", "onPause() - Mini player continues in background");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // ✅ IMPORTANT: Clean up resources but don't affect mini player
        // MediaPlayerRepository singleton must persist for mini player functionality
        android.util.Log.d("PlaylistDetailFragment", "onDestroy() - Fragment destroyed, mini player persists");

        // Clear adapter to prevent memory leaks
        if (adapter != null) {
            adapter.setSongs(null);
        }
    }
    
    // PlaylistSongCardAdapter.OnSongActionListener implementation
    @Override
    public void onSongClick(Song song, int position) {
        // When user clicks on song card, play the song (same as play button)
        onPlaySong(song, position);
    }

    @Override
    public void onPlaySong(Song song, int position) {
        android.util.Log.d("PlaylistDetailFragment", "onPlaySong called - Song: " + song.getTitle() + ", Position: " + position);

        // ✅ FIX: Use adapter data instead of ViewModel LiveData to avoid null issues
        Playlist currentPlaylist = viewModel.getCurrentPlaylist().getValue();
        List<Song> playlistSongs = null;

        // Try to get songs from adapter first (more reliable)
        if (adapter != null && adapter.getItemCount() > 0) {
            playlistSongs = adapter.getSongs();
            android.util.Log.d("PlaylistDetailFragment", "Got " + playlistSongs.size() + " songs from adapter");
        } else {
            // Fallback to ViewModel data
            playlistSongs = viewModel.getSongsInPlaylist().getValue();
            android.util.Log.d("PlaylistDetailFragment", "Fallback to ViewModel data: " + (playlistSongs != null ? playlistSongs.size() : "null"));
        }

        // Validation with better error messages
        if (currentPlaylist == null) {
            android.util.Log.e("PlaylistDetailFragment", "Current playlist is null");
            showToast("Lỗi: Không tìm thấy thông tin playlist");
            return;
        }

        if (playlistSongs == null || playlistSongs.isEmpty()) {
            android.util.Log.e("PlaylistDetailFragment", "Playlist songs is null or empty");
            showToast("Lỗi: Danh sách bài hát trống");
            return;
        }

        if (position < 0 || position >= playlistSongs.size()) {
            android.util.Log.e("PlaylistDetailFragment", "Invalid position: " + position + " for playlist size: " + playlistSongs.size());
            showToast("Lỗi: Vị trí bài hát không hợp lệ");
            return;
        }

        try {
            // ✅ CONSISTENT: Use playFromView with full playlist for navigation
            songDetailViewModel.playFromView(playlistSongs, currentPlaylist.getName(), position);

            android.util.Log.d("PlaylistDetailFragment", "Successfully called playFromView");
            showToast("Đang phát: " + song.getTitle() + " từ playlist: " + currentPlaylist.getName());

        } catch (Exception e) {
            android.util.Log.e("PlaylistDetailFragment", "Error playing song", e);
            showToast("Lỗi khi phát bài hát: " + e.getMessage());
        }
    }

    @Override
    public void onRemoveFromPlaylist(Song song, int position) {
        android.util.Log.d("PlaylistDetailFragment", "onRemoveFromPlaylist called - Song: " + song.getTitle() + ", Position: " + position);

        // Show confirmation dialog
        showRemoveConfirmationDialog(song, position);
    }

    /**
     * Show confirmation dialog before removing song
     */
    private void showRemoveConfirmationDialog(Song song, int position) {
        if (getContext() == null) return;

        String message = getString(R.string.confirm_remove_song_message, song.getTitle());

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.confirm_remove_song)
                .setMessage(message)
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    // Perform removal
                    performRemoveSong(song, position);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Actually remove the song from playlist
     */
    private void performRemoveSong(Song song, int position) {
        android.util.Log.d("PlaylistDetailFragment", "Performing remove for song: " + song.getTitle());

        try {
            // Remove from ViewModel (which will update database)
            viewModel.removeSongFromPlaylist(song.getId());

            // Show success message
            showToast(getString(R.string.song_removed_from_playlist));

            android.util.Log.d("PlaylistDetailFragment", "Successfully initiated song removal");

        } catch (Exception e) {
            android.util.Log.e("PlaylistDetailFragment", "Error removing song", e);
            showToast(getString(R.string.error_removing_song));
        }
    }

    /**
     * Validate playlist name and update UI accordingly
     */
    private boolean validatePlaylistName(String name, TextInputLayout textInputLayout, Button saveButton) {
        if (name == null || name.trim().isEmpty()) {
            textInputLayout.setError(getString(R.string.playlist_name_empty));
            saveButton.setEnabled(false);
            return false;
        }

        if (name.trim().length() > 50) {
            textInputLayout.setError(getString(R.string.playlist_name_too_long));
            saveButton.setEnabled(false);
            return false;
        }

        // Valid name
        textInputLayout.setError(null);
        saveButton.setEnabled(true);
        return true;
    }


}
