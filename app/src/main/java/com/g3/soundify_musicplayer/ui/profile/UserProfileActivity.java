package com.g3.soundify_musicplayer.ui.profile;

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
import com.g3.soundify_musicplayer.ui.playlist.PlaylistDetailActivity;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

/**
 * Activity for displaying user profile with public songs and playlists
 */
public class UserProfileActivity extends AppCompatActivity implements 
    PublicSongAdapter.OnSongClickListener, 
    PublicPlaylistAdapter.OnPlaylistClickListener {
    
    private UserProfileViewModel viewModel;
    private PublicSongAdapter songAdapter;
    private PublicPlaylistAdapter playlistAdapter;
    
    // UI Components
    private Toolbar toolbar;
    private ShapeableImageView userAvatar;
    private TextView displayName;
    private TextView username;
    private TextView bio;
    private Button followButton;
    private Button editProfileButton;
    private LinearLayout followersLayout;
    private LinearLayout followingLayout;
    private LinearLayout songsLayout;
    private TextView followersCount;
    private TextView followingCount;
    private TextView songsCount;
    private TabLayout tabLayout;
    private RecyclerView songsRecyclerView;
    private RecyclerView playlistsRecyclerView;
    private LinearLayout loadingLayout;
    private LinearLayout emptyStateLayout;
    private ImageView emptyIcon;
    private TextView emptyTitle;
    private TextView emptySubtitle;
    
    // Constants
    private static final String EXTRA_USER_ID = "user_id";
    private static final String EXTRA_USERNAME = "username";
    
    // Current tab
    private int currentTab = 0; // 0 = Songs, 1 = Playlists
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(UserProfileViewModel.class);
        
        // Initialize UI
        initializeViews();
        setupToolbar();
        setupRecyclerViews();
        setupTabs();
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
        userAvatar = findViewById(R.id.image_view_avatar);
        displayName = findViewById(R.id.text_view_display_name);
        username = findViewById(R.id.text_view_username);
        bio = findViewById(R.id.text_view_bio);
        followButton = findViewById(R.id.button_follow);
        editProfileButton = findViewById(R.id.button_edit_profile);
        followersLayout = findViewById(R.id.layout_followers);
        followingLayout = findViewById(R.id.layout_following);
        songsLayout = findViewById(R.id.layout_songs);
        followersCount = findViewById(R.id.text_view_followers_count);
        followingCount = findViewById(R.id.text_view_following_count);
        songsCount = findViewById(R.id.text_view_songs_count);
        tabLayout = findViewById(R.id.tab_layout);
        songsRecyclerView = findViewById(R.id.recycler_view_songs);
        playlistsRecyclerView = findViewById(R.id.recycler_view_playlists);
        loadingLayout = findViewById(R.id.layout_loading);
        emptyStateLayout = findViewById(R.id.layout_empty_state);
        emptyIcon = findViewById(R.id.image_view_empty_icon);
        emptyTitle = findViewById(R.id.text_view_empty_title);
        emptySubtitle = findViewById(R.id.text_view_empty_subtitle);
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
     * Setup RecyclerViews with adapters
     */
    private void setupRecyclerViews() {
        // Songs RecyclerView
        songAdapter = new PublicSongAdapter(this);
        songAdapter.setOnSongClickListener(this);
        songsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        songsRecyclerView.setAdapter(songAdapter);
        
        // Playlists RecyclerView
        playlistAdapter = new PublicPlaylistAdapter(this);
        playlistAdapter.setOnPlaylistClickListener(this);
        playlistsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        playlistsRecyclerView.setAdapter(playlistAdapter);
    }
    
    /**
     * Setup tab layout
     */
    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                switchTab(currentTab);
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    
    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        followButton.setOnClickListener(v -> toggleFollow());
        editProfileButton.setOnClickListener(v -> editProfile());
        
        // Stats click listeners (for future implementation)
        followersLayout.setOnClickListener(v -> viewFollowers());
        followingLayout.setOnClickListener(v -> viewFollowing());
        songsLayout.setOnClickListener(v -> {
            // Switch to songs tab
            TabLayout.Tab songsTab = tabLayout.getTabAt(0);
            if (songsTab != null) {
                songsTab.select();
            }
        });
    }
    
    /**
     * Setup LiveData observers
     */
    private void setupObservers() {
        // Observe user data
        viewModel.getCurrentUser().observe(this, this::updateUserInfo);
        
        // Observe own profile status
        viewModel.getIsOwnProfile().observe(this, this::updateProfileButtons);
        
        // Observe follow status
        viewModel.getIsFollowing().observe(this, this::updateFollowButton);
        
        // Observe stats
        viewModel.getFollowersCount().observe(this, count -> {
            followersCount.setText(viewModel.getFollowersCountString());
        });
        
        viewModel.getFollowingCount().observe(this, count -> {
            followingCount.setText(viewModel.getFollowingCountString());
        });
        
        viewModel.getSongsCount().observe(this, count -> {
            songsCount.setText(viewModel.getSongsCountString());
        });
        
        // Observe content
        viewModel.getPublicSongs().observe(this, this::updateSongsList);
        viewModel.getPublicPlaylists().observe(this, this::updatePlaylistsList);
        
        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                loadingLayout.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                followButton.setEnabled(!isLoading);
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
            }
        });
    }
    
    /**
     * Handle intent to get user ID
     */
    private void handleIntent() {
        Intent intent = getIntent();
        long userId = intent.getLongExtra(EXTRA_USER_ID, -1);
        String usernameHint = intent.getStringExtra(EXTRA_USERNAME);
        
        if (userId != -1) {
            viewModel.loadUserProfile(userId);
            
            // Update toolbar title if username is provided
            if (usernameHint != null && getSupportActionBar() != null) {
                getSupportActionBar().setTitle("@" + usernameHint);
            }
        } else {
            Toast.makeText(this, "Invalid user ID", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    /**
     * Update user information in UI
     */
    private void updateUserInfo(User user) {
        if (user != null) {
            displayName.setText(user.getDisplayName());
            username.setText("@" + user.getUsername());
            
            // Update toolbar title
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("@" + user.getUsername());
            }
            
            // Set bio if available
            if (user.getBio() != null && !user.getBio().trim().isEmpty()) {
                bio.setText(user.getBio());
                bio.setVisibility(View.VISIBLE);
            } else {
                bio.setVisibility(View.GONE);
            }
            
            // Set avatar (placeholder for now)
            userAvatar.setImageResource(R.drawable.placeholder_avatar);
        }
    }
    
    /**
     * Update profile buttons based on ownership
     */
    private void updateProfileButtons(Boolean isOwnProfile) {
        if (isOwnProfile != null) {
            if (isOwnProfile) {
                followButton.setVisibility(View.GONE);
                editProfileButton.setVisibility(View.VISIBLE);
                
                // Show menu for own content
                songAdapter.setShowMenu(true);
                playlistAdapter.setShowMenu(true);
            } else {
                followButton.setVisibility(View.VISIBLE);
                editProfileButton.setVisibility(View.GONE);
                
                // Hide menu for other user's content
                songAdapter.setShowMenu(false);
                playlistAdapter.setShowMenu(false);
            }
        }
    }
    
    /**
     * Update follow button based on follow status
     */
    private void updateFollowButton(Boolean isFollowing) {
        if (isFollowing != null) {
            if (isFollowing) {
                followButton.setText(R.string.user_profile_following);
                // Note: Regular Button doesn't have setIcon method
                // Using Material Button would require changing layout
            } else {
                followButton.setText(R.string.user_profile_follow);
            }
        }
    }

    /**
     * Update songs list and empty state
     */
    private void updateSongsList(List<Song> songs) {
        songAdapter.setSongs(songs);

        if (currentTab == 0) { // Songs tab is active
            updateEmptyState(songs == null || songs.isEmpty(), true);
        }
    }

    /**
     * Update playlists list and empty state
     */
    private void updatePlaylistsList(List<Playlist> playlists) {
        playlistAdapter.setPlaylists(playlists);

        if (currentTab == 1) { // Playlists tab is active
            updateEmptyState(playlists == null || playlists.isEmpty(), false);
        }
    }

    /**
     * Switch between tabs
     */
    private void switchTab(int tabIndex) {
        if (tabIndex == 0) {
            // Songs tab
            songsRecyclerView.setVisibility(View.VISIBLE);
            playlistsRecyclerView.setVisibility(View.GONE);

            List<Song> songs = viewModel.getPublicSongs().getValue();
            updateEmptyState(songs == null || songs.isEmpty(), true);

        } else if (tabIndex == 1) {
            // Playlists tab
            songsRecyclerView.setVisibility(View.GONE);
            playlistsRecyclerView.setVisibility(View.VISIBLE);

            List<Playlist> playlists = viewModel.getPublicPlaylists().getValue();
            updateEmptyState(playlists == null || playlists.isEmpty(), false);
        }
    }

    /**
     * Update empty state based on content and tab
     */
    private void updateEmptyState(boolean isEmpty, boolean isSongsTab) {
        boolean isLoading = Boolean.TRUE.equals(viewModel.getIsLoading().getValue());
        boolean isOwnProfile = Boolean.TRUE.equals(viewModel.getIsOwnProfile().getValue());

        if (isEmpty && !isLoading) {
            emptyStateLayout.setVisibility(View.VISIBLE);

            if (isSongsTab) {
                emptyIcon.setImageResource(R.drawable.ic_queue_music);
                if (isOwnProfile) {
                    emptyTitle.setText(R.string.profile_own_no_songs_title);
                    emptySubtitle.setText(R.string.profile_own_no_songs_subtitle);
                } else {
                    emptyTitle.setText(R.string.profile_no_songs_title);
                    emptySubtitle.setText(R.string.profile_no_songs_subtitle);
                }
            } else {
                emptyIcon.setImageResource(R.drawable.ic_queue_music);
                if (isOwnProfile) {
                    emptyTitle.setText(R.string.profile_own_no_playlists_title);
                    emptySubtitle.setText(R.string.profile_own_no_playlists_subtitle);
                } else {
                    emptyTitle.setText(R.string.profile_no_playlists_title);
                    emptySubtitle.setText(R.string.profile_no_playlists_subtitle);
                }
            }
        } else {
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Toggle follow status
     */
    private void toggleFollow() {
        Boolean isFollowing = viewModel.getIsFollowing().getValue();
        if (Boolean.TRUE.equals(isFollowing)) {
            // Show unfollow confirmation
            User user = viewModel.getCurrentUser().getValue();
            if (user != null) {
                showUnfollowConfirmation(user);
            }
        } else {
            // Follow directly
            viewModel.toggleFollowStatus();
        }
    }

    /**
     * Show unfollow confirmation dialog
     */
    private void showUnfollowConfirmation(User user) {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.unfollow_confirmation_title, user.getDisplayName()))
            .setMessage(R.string.unfollow_confirmation_message)
            .setPositiveButton(R.string.unfollow_confirm, (dialog, which) -> {
                viewModel.toggleFollowStatus();
            })
            .setNegativeButton(R.string.button_cancel, null)
            .show();
    }

    /**
     * Edit profile (placeholder)
     */
    private void editProfile() {
        // TODO: Navigate to edit profile screen
        Toast.makeText(this, "Edit Profile - Not implemented yet", Toast.LENGTH_SHORT).show();
    }

    /**
     * View followers (placeholder)
     */
    private void viewFollowers() {
        // TODO: Navigate to followers list
        Toast.makeText(this, "View Followers - Not implemented yet", Toast.LENGTH_SHORT).show();
    }

    /**
     * View following (placeholder)
     */
    private void viewFollowing() {
        // TODO: Navigate to following list
        Toast.makeText(this, "View Following - Not implemented yet", Toast.LENGTH_SHORT).show();
    }

    // PublicSongAdapter.OnSongClickListener implementation

    @Override
    public void onSongClick(Song song, int position) {
        // TODO: Navigate to song detail or start playing
        Toast.makeText(this, "Playing: " + song.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSongMenuClick(Song song, int position) {
        // TODO: Show song menu (edit, delete, etc.)
        Toast.makeText(this, "Song menu: " + song.getTitle(), Toast.LENGTH_SHORT).show();
    }

    // PublicPlaylistAdapter.OnPlaylistClickListener implementation

    @Override
    public void onPlaylistClick(Playlist playlist, int position) {
        // Navigate to playlist detail
        Intent intent = PlaylistDetailActivity.createIntent(this, playlist.getId());
        startActivity(intent);
    }

    @Override
    public void onPlaylistMenuClick(Playlist playlist, int position) {
        // TODO: Show playlist menu (edit, delete, etc.)
        Toast.makeText(this, "Playlist menu: " + playlist.getName(), Toast.LENGTH_SHORT).show();
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
     * Static method to create intent for user profile
     */
    public static Intent createIntent(Context context, long userId) {
        Intent intent = new Intent(context, UserProfileActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        return intent;
    }

    /**
     * Static method to create intent for user profile with username hint
     */
    public static Intent createIntent(Context context, long userId, String username) {
        Intent intent = new Intent(context, UserProfileActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_USERNAME, username);
        return intent;
    }
}
