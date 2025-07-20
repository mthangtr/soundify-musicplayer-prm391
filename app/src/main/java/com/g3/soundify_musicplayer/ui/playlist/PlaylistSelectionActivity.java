package com.g3.soundify_musicplayer.ui.playlist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Playlist;

/**
 * Activity for selecting a playlist to add a song to
 * UI ONLY - Uses mock data for demonstration
 */
public class PlaylistSelectionActivity extends AppCompatActivity implements PlaylistSelectionAdapter.OnPlaylistClickListener {

    // UI Components
    private ImageButton btnBack;
    private RecyclerView playlistsRecyclerView;
    
    // ViewModel and Adapter
    private PlaylistSelectionViewModel viewModel;
    private PlaylistSelectionAdapter adapter;
    
    // Constants
    private static final String EXTRA_SONG_ID = "song_id";
    public static final String RESULT_PLAYLIST_NAME = "playlist_name";
    public static final String RESULT_PLAYLIST_ID = "playlist_id";
    
    private long songId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_selection);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(PlaylistSelectionViewModel.class);

        // Handle intent
        handleIntent();

        // Initialize UI
        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        setupObservers();

        // Load playlists
        viewModel.loadPlaylists();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        btnBack = findViewById(R.id.btn_back);
        playlistsRecyclerView = findViewById(R.id.recycler_view_playlists);
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
    }

    /**
     * Setup RecyclerView with adapter
     */
    private void setupRecyclerView() {
        adapter = new PlaylistSelectionAdapter(this);
        adapter.setOnPlaylistClickListener(this);
        
        playlistsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        playlistsRecyclerView.setAdapter(adapter);
    }

    /**
     * Setup LiveData observers
     */
    private void setupObservers() {
        viewModel.getPlaylists().observe(this, playlists -> {
            if (playlists != null) {
                adapter.updateData(playlists);
            }
        });
        
        viewModel.getIsLoading().observe(this, isLoading -> {
            // Could add loading indicator here if needed
        });
    }

    /**
     * Handle intent to get song ID
     */
    private void handleIntent() {
        Intent intent = getIntent();
        songId = intent.getLongExtra(EXTRA_SONG_ID, -1);
        
        if (songId == -1) {
            Toast.makeText(this, "Invalid song ID", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onPlaylistClick(Playlist playlist) {
        // Simulate adding song to playlist
        viewModel.addSongToPlaylist(songId, playlist.getId());
        
        // Return result to calling activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra(RESULT_PLAYLIST_NAME, playlist.getName());
        resultIntent.putExtra(RESULT_PLAYLIST_ID, playlist.getId());
        setResult(Activity.RESULT_OK, resultIntent);
        
        // Show brief confirmation and finish
        Toast.makeText(this, "Added to " + playlist.getName(), Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }

    /**
     * Static method to create intent for playlist selection
     */
    public static Intent createIntent(Context context, long songId) {
        Intent intent = new Intent(context, PlaylistSelectionActivity.class);
        intent.putExtra(EXTRA_SONG_ID, songId);
        return intent;
    }
}
