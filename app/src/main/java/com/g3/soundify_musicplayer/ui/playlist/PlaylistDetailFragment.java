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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

/**
 * Fragment for displaying playlist details and managing songs
 */
public class PlaylistDetailFragment extends Fragment implements PlaylistSongAdapter.OnSongActionListener {

    private PlaylistDetailViewModel viewModel;
    private SongDetailViewModel songDetailViewModel;
    private PlaylistSongAdapter adapter;
    
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
    private FloatingActionButton fabAddSongs;
    
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
        songDetailViewModel = new ViewModelProvider(requireActivity()).get(SongDetailViewModel.class);

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
        setupObservers();
        
        // Load playlist data
        handleArguments();
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
        fabAddSongs = view.findViewById(R.id.fab_add_songs);
    }
    
    /**
     * Setup RecyclerView with adapter
     */
    private void setupRecyclerView() {
        if (getContext() == null) return;
        
        adapter = new PlaylistSongAdapter(getContext());
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
        fabAddSongs.setOnClickListener(v -> openSongSelection());
    }
    
    /**
     * Setup observers for ViewModel LiveData
     */
    private void setupObservers() {
        // Observe current playlist
        viewModel.getCurrentPlaylist().observe(getViewLifecycleOwner(), this::updatePlaylistInfo);
        
        // Observe playlist songs
        viewModel.getSongsInPlaylist().observe(getViewLifecycleOwner(), this::updateSongsList);
        
        // Observe owner status
        viewModel.getIsOwner().observe(getViewLifecycleOwner(), isOwner -> {
            if (isOwner != null) {
                editPlaylistButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
            }
        });
        
        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Handle loading state if needed
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
        adapter.setSongs(songs);
        viewModel.updatePlaylistStats(songs);
        updatePlaylistInfoText();
        
        // Show/hide empty state
        boolean isEmpty = songs == null || songs.isEmpty();
        emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        songsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        
        // Update action buttons state
        playAllButton.setEnabled(!isEmpty);
        shuffleButton.setEnabled(!isEmpty);
    }
    
    /**
     * Update playlist info text with stats
     */
    private void updatePlaylistInfoText() {
        Playlist playlist = viewModel.getCurrentPlaylist().getValue();
        if (playlist == null) return;
        
        String ownerName = "Unknown"; // TODO: Get from User entity
        int songCount = adapter.getItemCount();
        String duration = viewModel.getTotalDuration().getValue();
        if (duration == null) duration = "0:00";
        
        String infoText = String.format("Created by %s • %d songs • %s", 
                                      ownerName, songCount, duration);
        playlistInfo.setText(infoText);
        
        // Update songs header
        String songsHeaderText = songCount == 1 ? "1 song" : songCount + " songs";
        songsHeader.setText(songsHeaderText);
    }
    
    /**
     * Play all songs in playlist
     */
    private void playAllSongs() {
        // TODO: Implement play all functionality
        showToast("Play All - Not implemented yet");
    }
    
    /**
     * Shuffle and play songs in playlist
     */
    private void shufflePlaySongs() {
        // TODO: Implement shuffle play functionality
        showToast("Shuffle Play - Not implemented yet");
    }
    
    /**
     * Show edit playlist dialog
     */
    private void showEditPlaylistDialog() {
        // TODO: Implement edit playlist dialog
        showToast("Edit Playlist - Not implemented yet");
    }
    
    /**
     * Open song selection activity
     */
    private void openSongSelection() {
        // TODO: Implement song selection
        showToast("Add Songs - Not implemented yet");
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
    
    // PlaylistSongAdapter.OnSongActionListener implementation
    @Override
    public void onSongClick(Song song, int position) {
        // IMPLEMENT: Phát bài hát với NavigationContext từ playlist
        Playlist currentPlaylist = viewModel.getCurrentPlaylist().getValue();
        List<Song> playlistSongs = viewModel.getSongsInPlaylist().getValue();

        if (currentPlaylist == null || playlistSongs == null || playlistSongs.isEmpty()) {
            showToast("Không thể phát bài hát - danh sách trống");
            return;
        }

        // Tạo danh sách song IDs từ playlist
        java.util.List<Long> songIds = new java.util.ArrayList<>();
        for (Song s : playlistSongs) {
            songIds.add(s.getId());
        }

        // ✅ CONSISTENT: Use playFromView with full playlist for navigation
        // This ensures Next/Previous buttons work properly in mini/full player
        songDetailViewModel.playFromView(playlistSongs, currentPlaylist.getName(), position);

        showToast("Playing: " + song.getTitle() + " from playlist: " + currentPlaylist.getName());
    }

    @Override
    public void onRemoveSong(Song song, int position) {
        // TODO: Remove song from playlist
        showToast("Remove song: " + song.getTitle());
    }

    @Override
    public void onMoveSong(int fromPosition, int toPosition) {
        // TODO: Handle song reordering
        showToast("Moved song from " + fromPosition + " to " + toPosition);
    }
}
