package com.g3.soundify_musicplayer.ui.playlist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

/**
 * Activity for displaying playlist details and managing songs
 */
public class PlaylistDetailActivity extends AppCompatActivity implements PlaylistSongAdapter.OnSongActionListener {
    
    private PlaylistDetailViewModel viewModel;
    private PlaylistSongAdapter adapter;


    
    // UI Components
    private Toolbar toolbar;
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
    private static final String EXTRA_PLAYLIST_ID = "playlist_id";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(PlaylistDetailViewModel.class);



        // Initialize UI
        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupClickListeners();
        setupObservers();

        // Load playlist data
        handleIntent();
    }


    
    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        playlistCover = findViewById(R.id.image_view_playlist_cover);
        playlistName = findViewById(R.id.text_view_playlist_name);
        playlistInfo = findViewById(R.id.text_view_playlist_info);
        playAllButton = findViewById(R.id.button_play_all);
        shuffleButton = findViewById(R.id.button_shuffle);
        editPlaylistButton = findViewById(R.id.button_edit_playlist);
        songsHeader = findViewById(R.id.text_view_songs_header);
        songsRecyclerView = findViewById(R.id.recycler_view_songs);
        emptyStateLayout = findViewById(R.id.layout_empty_state);
        fabAddSongs = findViewById(R.id.fab_add_songs);
    }
    
    /**
     * Setup toolbar with back navigation
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }
    
    /**
     * Setup RecyclerView with adapter
     */
    private void setupRecyclerView() {
        adapter = new PlaylistSongAdapter(this);
        adapter.setOnSongActionListener(this);
        
        songsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        songsRecyclerView.setAdapter(adapter);
    }
    
    /**
     * Setup click listeners for buttons
     */
    private void setupClickListeners() {
        playAllButton.setOnClickListener(v -> playAllSongs());
        shuffleButton.setOnClickListener(v -> shufflePlay());
        editPlaylistButton.setOnClickListener(v -> editPlaylist());
        fabAddSongs.setOnClickListener(v -> addSongsToPlaylist());
        
        // Cover art click to show full image (optional)
        playlistCover.setOnClickListener(v -> showFullCoverArt());
    }
    
    /**
     * Setup LiveData observers
     */
    private void setupObservers() {
        // Observe playlist data
        viewModel.getCurrentPlaylist().observe(this, this::updatePlaylistInfo);
        
        // Observe playlist owner
        viewModel.getPlaylistOwner().observe(this, this::updateOwnerInfo);
        
        // Observe ownership status
        viewModel.getIsOwner().observe(this, isOwner -> {
            if (isOwner != null) {
                updateOwnershipUI(isOwner);
                adapter.setIsOwner(isOwner);
            }
        });
        
        // Observe songs list
        viewModel.getSongsInPlaylist().observe(this, this::updateSongsList);
        
        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            // You can show/hide progress indicator here
            playAllButton.setEnabled(!Boolean.TRUE.equals(isLoading));
            shuffleButton.setEnabled(!Boolean.TRUE.equals(isLoading));
        });
        
        // Observe error messages
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                viewModel.clearErrorMessage();
            }
        });
        
        // Observe success messages
        viewModel.getSuccessMessage().observe(this, successMessage -> {
            if (successMessage != null && !successMessage.isEmpty()) {
                Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();
                viewModel.clearSuccessMessage();
            }
        });
        
        // Observe song count for header
        viewModel.getSongCount().observe(this, count -> {
            if (count != null) {
                songsHeader.setText(viewModel.getSongsCountString());
            }
        });
    }
    
    /**
     * Handle intent to get playlist ID
     */
    private void handleIntent() {
        Intent intent = getIntent();
        long playlistId = intent.getLongExtra(EXTRA_PLAYLIST_ID, -1);
        
        if (playlistId != -1) {
            viewModel.loadPlaylist(playlistId);
        } else {
            Toast.makeText(this, "Invalid playlist ID", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    /**
     * Update playlist information in UI
     */
    private void updatePlaylistInfo(Playlist playlist) {
        if (playlist != null) {
            playlistName.setText(playlist.getName());
            
            // Set cover art (placeholder for now)
            playlistCover.setImageResource(R.drawable.placeholder_album_art);
            
            // Update toolbar title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(playlist.getName());
            }
        }
    }
    
    /**
     * Update owner information
     */
    private void updateOwnerInfo(User owner) {
        // This will be called when playlist stats are updated
        updatePlaylistInfoText();
    }
    
    /**
     * Update playlist info text with stats
     */
    private void updatePlaylistInfoText() {
        String infoText = viewModel.getPlaylistInfoString();
        playlistInfo.setText(infoText);
    }
    
    /**
     * Update UI based on ownership
     */
    private void updateOwnershipUI(boolean isOwner) {
        editPlaylistButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        fabAddSongs.setVisibility(isOwner ? View.VISIBLE : View.GONE);
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
     * Play all songs in playlist
     */
    private void playAllSongs() {
        // TODO: Implement play all functionality
        // This would typically start the music player with the playlist
        Toast.makeText(this, "Play All - Not implemented yet", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Shuffle play songs
     */
    private void shufflePlay() {
        // TODO: Implement shuffle play functionality
        Toast.makeText(this, "Shuffle Play - Not implemented yet", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Edit playlist (owner only)
     */
    private void editPlaylist() {
        // TODO: Navigate to edit playlist screen
        Toast.makeText(this, "Edit Playlist - Not implemented yet", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Add songs to playlist (owner only)
     * TODO: Implement alternative method for adding songs to playlist
     */
    private void addSongsToPlaylist() {
        // Functionality removed - Select Songs feature has been removed
        Toast.makeText(this, "Add songs functionality not available", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Show full cover art (optional feature)
     */
    private void showFullCoverArt() {
        // TODO: Show full screen cover art
        Toast.makeText(this, "Show full cover art", Toast.LENGTH_SHORT).show();
    }
    
    // PlaylistSongAdapter.OnSongActionListener implementation
    
    @Override
    public void onSongClick(Song song, int position) {
        // TODO: Play selected song
        Toast.makeText(this, "Playing: " + song.getTitle(), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onRemoveSong(Song song, int position) {
        // Show confirmation dialog
        new AlertDialog.Builder(this)
            .setTitle(R.string.remove_song_title)
            .setMessage(getString(R.string.remove_song_message, song.getTitle()))
            .setPositiveButton(R.string.remove_confirm, (dialog, which) -> {
                viewModel.removeSongFromPlaylist(song.getId(), song.getTitle());
            })
            .setNegativeButton(R.string.button_cancel, null)
            .show();
    }
    
    @Override
    public void onMoveSong(int fromPosition, int toPosition) {
        // Update song position in database
        List<Song> songs = adapter.getSongs();
        if (toPosition < songs.size()) {
            Song movedSong = songs.get(toPosition);
            viewModel.updateSongPosition(movedSong.getId(), toPosition + 1);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Static method to create intent for playlist detail
     */
    public static Intent createIntent(Context context, long playlistId) {
        Intent intent = new Intent(context, PlaylistDetailActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playlistId);
        return intent;
    }
}
