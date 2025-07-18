package com.g3.soundify_musicplayer.ui.playlist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.Set;

/**
 * Activity for selecting songs to add to a playlist
 */
public class SelectSongsActivity extends AppCompatActivity implements SelectableSongAdapter.OnSongSelectionListener {
    
    private SelectSongsViewModel viewModel;
    private SelectableSongAdapter adapter;
    
    // UI Components
    private Toolbar toolbar;
    private TextInputEditText searchEditText;
    private ChipGroup filterChipGroup;
    private Chip chipAllSongs;
    private Chip chipMySongs;
    private Chip chipPublicSongs;
    private RecyclerView songsRecyclerView;
    private LinearLayout loadingLayout;
    private LinearLayout emptyStateLayout;
    private TextView emptyTitle;
    private TextView emptySubtitle;
    private TextView selectionCountText;
    private Button cancelButton;
    private Button doneButton;
    
    // Constants
    private static final String EXTRA_PLAYLIST_ID = "playlist_id";
    private static final String EXTRA_PLAYLIST_NAME = "playlist_name";
    public static final String RESULT_ADDED_COUNT = "added_count";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_songs);
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(SelectSongsViewModel.class);
        
        // Initialize UI
        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupSearchAndFilter();
        setupClickListeners();
        setupObservers();
        
        // Handle intent
        handleIntent();
    }
    
    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        searchEditText = findViewById(R.id.edit_text_search);
        filterChipGroup = findViewById(R.id.chip_group_filters);
        chipAllSongs = findViewById(R.id.chip_all_songs);
        chipMySongs = findViewById(R.id.chip_my_songs);
        chipPublicSongs = findViewById(R.id.chip_public_songs);
        songsRecyclerView = findViewById(R.id.recycler_view_songs);
        loadingLayout = findViewById(R.id.layout_loading);
        emptyStateLayout = findViewById(R.id.layout_empty_state);
        emptyTitle = findViewById(R.id.text_view_empty_title);
        emptySubtitle = findViewById(R.id.text_view_empty_subtitle);
        selectionCountText = findViewById(R.id.text_view_selection_count);
        cancelButton = findViewById(R.id.button_cancel);
        doneButton = findViewById(R.id.button_done);
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
        adapter = new SelectableSongAdapter(this);
        adapter.setOnSongSelectionListener(this);
        
        songsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        songsRecyclerView.setAdapter(adapter);
    }
    
    /**
     * Setup search and filter functionality
     */
    private void setupSearchAndFilter() {
        // Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Filter chips
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return; // Prevent no selection
            }
            
            int checkedId = checkedIds.get(0);
            SelectSongsViewModel.FilterType filter;
            
            if (checkedId == R.id.chip_all_songs) {
                filter = SelectSongsViewModel.FilterType.ALL_SONGS;
            } else if (checkedId == R.id.chip_my_songs) {
                filter = SelectSongsViewModel.FilterType.MY_SONGS;
            } else if (checkedId == R.id.chip_public_songs) {
                filter = SelectSongsViewModel.FilterType.PUBLIC_SONGS;
            } else {
                filter = SelectSongsViewModel.FilterType.ALL_SONGS;
            }
            
            viewModel.setFilter(filter);
        });
    }
    
    /**
     * Setup click listeners for buttons
     */
    private void setupClickListeners() {
        cancelButton.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
        
        doneButton.setOnClickListener(v -> {
            viewModel.addSelectedSongsToPlaylist();
        });
    }
    
    /**
     * Setup LiveData observers
     */
    private void setupObservers() {
        // Observe filtered songs
        viewModel.getFilteredSongs().observe(this, this::updateSongsList);
        
        // Observe selected song IDs
        viewModel.getSelectedSongIds().observe(this, this::updateSelection);
        
        // Observe selection count
        viewModel.getSelectionCount().observe(this, count -> {
            if (count != null) {
                selectionCountText.setText(viewModel.getSelectionCountString());
                doneButton.setEnabled(count > 0);
            }
        });
        
        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                loadingLayout.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                doneButton.setEnabled(!isLoading && viewModel.getSelectionCount().getValue() != null && 
                                    viewModel.getSelectionCount().getValue() > 0);
            }
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
                
                // Return success result
                Intent resultIntent = new Intent();
                Integer count = viewModel.getSelectionCount().getValue();
                resultIntent.putExtra(RESULT_ADDED_COUNT, count != null ? count : 0);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
        
        // Observe search query for empty state
        viewModel.getSearchQuery().observe(this, query -> {
            updateEmptyState(query);
        });
    }
    
    /**
     * Handle intent to get playlist information
     */
    private void handleIntent() {
        Intent intent = getIntent();
        long playlistId = intent.getLongExtra(EXTRA_PLAYLIST_ID, -1);
        String playlistName = intent.getStringExtra(EXTRA_PLAYLIST_NAME);
        
        if (playlistId != -1) {
            viewModel.initializeForPlaylist(playlistId);
            
            // Update toolbar title if playlist name is provided
            if (playlistName != null && getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Add to " + playlistName);
            }
        } else {
            Toast.makeText(this, "Invalid playlist ID", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    /**
     * Update songs list and empty state
     */
    private void updateSongsList(List<Song> songs) {
        adapter.setSongs(songs);
        
        boolean isEmpty = songs == null || songs.isEmpty();
        boolean isLoading = Boolean.TRUE.equals(viewModel.getIsLoading().getValue());
        
        songsRecyclerView.setVisibility(!isEmpty && !isLoading ? View.VISIBLE : View.GONE);
        emptyStateLayout.setVisibility(isEmpty && !isLoading ? View.VISIBLE : View.GONE);
    }
    
    /**
     * Update selection state in adapter
     */
    private void updateSelection(Set<Long> selectedIds) {
        adapter.setSelectedSongIds(selectedIds);
    }
    
    /**
     * Update empty state message based on search query
     */
    private void updateEmptyState(String query) {
        if (query != null && !query.trim().isEmpty()) {
            emptyTitle.setText(getString(R.string.select_songs_no_results, query));
            emptySubtitle.setText(R.string.select_songs_empty_subtitle);
        } else {
            emptyTitle.setText(R.string.select_songs_empty_message);
            emptySubtitle.setText(R.string.select_songs_empty_subtitle);
        }
    }
    
    // SelectableSongAdapter.OnSongSelectionListener implementation
    
    @Override
    public void onSongSelectionChanged(long songId, boolean isSelected) {
        viewModel.toggleSongSelection(songId);
    }
    
    @Override
    public void onSongClick(Song song, int position) {
        // Song click is handled by selection toggle
        // Could add additional functionality here if needed
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }
    
    /**
     * Static method to create intent for selecting songs
     */
    public static Intent createIntent(Context context, long playlistId, String playlistName) {
        Intent intent = new Intent(context, SelectSongsActivity.class);
        intent.putExtra(EXTRA_PLAYLIST_ID, playlistId);
        intent.putExtra(EXTRA_PLAYLIST_NAME, playlistName);
        return intent;
    }
}
