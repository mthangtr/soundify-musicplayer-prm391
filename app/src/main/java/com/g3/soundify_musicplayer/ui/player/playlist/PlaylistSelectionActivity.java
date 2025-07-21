package com.g3.soundify_musicplayer.ui.player.playlist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.ui.player.SongDetailViewModel;
import com.g3.soundify_musicplayer.utils.AuthManager;

import java.util.List;

/**
 * Activity for selecting a playlist to add a song to
 * Sử dụng SongDetailViewModel để làm việc với dữ liệu thật từ backend
 */
public class PlaylistSelectionActivity extends AppCompatActivity implements PlaylistSelectionAdapter.OnPlaylistClickListener {

    // UI Components
    private ImageButton btnBack;
    private RecyclerView playlistsRecyclerView;

    // ViewModel and Adapter
    private SongDetailViewModel viewModel;
    private PlaylistSelectionAdapter adapter;
    private AuthManager authManager;

    // Constants
    private static final String EXTRA_SONG_ID = "song_id";
    public static final String RESULT_PLAYLIST_NAME = "playlist_name";
    public static final String RESULT_PLAYLIST_ID = "playlist_id";

    private long songId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_selection);

        // Initialize components
        authManager = new AuthManager(this);
        viewModel = new ViewModelProvider(this).get(SongDetailViewModel.class);

        // Handle intent
        handleIntent();

        // Initialize UI
        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        setupObservers();

        // Load user playlists
        loadUserPlaylists();

        // Setup OnBackPressedDispatcher
        setupBackPressedHandler();
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
        // Observe user playlists
        viewModel.getUserPlaylists().observe(this, playlists -> {
            if (playlists != null) {
                adapter.updateData(playlists);
            }
        });

        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            // Could add loading indicator here if needed
        });

        // Observe error messages
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
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

    /**
     * Load user playlists from backend
     */
    private void loadUserPlaylists() {
        long currentUserId = authManager.getCurrentUserId();
        if (currentUserId == -1) {
            Toast.makeText(this, "Bạn cần đăng nhập để xem playlist", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Load song detail để trigger việc load user playlists
        viewModel.loadSongDetail(songId, currentUserId);
    }

    @Override
    public void onPlaylistClick(Playlist playlist) {
        // Thêm bài hát vào playlist đã chọn
        viewModel.addSongToPlaylists(songId, List.of(playlist.getId()));

        // Return result to calling activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra(RESULT_PLAYLIST_NAME, playlist.getName());
        resultIntent.putExtra(RESULT_PLAYLIST_ID, playlist.getId());
        setResult(Activity.RESULT_OK, resultIntent);

        // Show brief confirmation and finish
        Toast.makeText(this, "Đã thêm vào \"" + playlist.getName() + "\"", Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * Setup OnBackPressedDispatcher để thay thế deprecated onBackPressed()
     */
    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
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
