package com.g3.soundify_musicplayer.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.ui.playlist.PlaylistDetailFragment;
import com.g3.soundify_musicplayer.data.Adapter.PlaylistAdapter;
import com.g3.soundify_musicplayer.data.Adapter.SongWithUploaderInfoAdapter;
import com.g3.soundify_musicplayer.data.dto.SongWithUploaderInfo;
import com.g3.soundify_musicplayer.utils.AuthManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for displaying user profile
 */
public class UserProfileFragment extends Fragment {
    
    // Constants
    public static final String ARG_USER_ID = "user_id";
    public static final String ARG_USERNAME = "username";
    
    // ViewModels and Managers
    private UserProfileViewModel viewModel;
    private AuthManager authManager;
    
    // UI Components
    private ImageView profileImage;
    private TextView displayName;
    private TextView username;
    private TextView bio;
    private TextView followersCount;
    private TextView followingCount;
    private TextView songsCount;
    private TextView playlistsCount;
    private LinearLayout followersContainer;
    private LinearLayout followingContainer;
    private Button followButton;
    private Button editProfileButton;
    private Button logoutButton;
    
    // Tab system
    private LinearLayout tabSongs;
    private LinearLayout tabPlaylists;
    private View tabSongsIndicator;
    private View tabPlaylistsIndicator;
    private RecyclerView songsRecyclerView;
    private RecyclerView playlistsRecyclerView;
    
    // Adapters
    private SongWithUploaderInfoAdapter songsAdapter;
    private PlaylistAdapter playlistsAdapter;
    
    // Data
    private User currentUser;
    private long userId = -1;
    private int currentTab = 0; // 0 = Songs, 1 = Playlists
    
    public static UserProfileFragment newInstance(long userId) {
        UserProfileFragment fragment = new UserProfileFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }
    
    public static UserProfileFragment newInstance(long userId, String username) {
        UserProfileFragment fragment = new UserProfileFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        args.putString(ARG_USERNAME, username);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize AuthManager
        authManager = new AuthManager(requireContext());
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(UserProfileViewModel.class);
        
        // Get arguments
        if (getArguments() != null) {
            userId = getArguments().getLong(ARG_USER_ID, -1);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_profile, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupRecyclerViews();
        setupTabs();
        setupClickListeners();
        setupObservers();
        
        // Load user profile
        if (userId != -1) {
            viewModel.loadUserProfile(userId);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this fragment
        if (viewModel != null) {
            android.util.Log.d("UserProfileFragment", "onResume: Refreshing user profile data");
            viewModel.refreshUserData();
        }
    }
    
    /**
     * Initialize all UI components
     */
    private void initViews(View view) {
        // Profile info
        profileImage = view.findViewById(R.id.profile_image);
        displayName = view.findViewById(R.id.display_name);
        username = view.findViewById(R.id.username);
        bio = view.findViewById(R.id.bio);
        
        // Social Stats
        followersCount = view.findViewById(R.id.followers_count);
        followingCount = view.findViewById(R.id.following_count);

        // Stats containers (for click listeners)
        followersContainer = (LinearLayout) followersCount.getParent();
        followingContainer = (LinearLayout) followingCount.getParent();

        // Content counts (in tabs)
        songsCount = view.findViewById(R.id.songs_count);
        playlistsCount = view.findViewById(R.id.playlists_count);
        
        // Buttons
        followButton = view.findViewById(R.id.follow_button);
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        logoutButton = view.findViewById(R.id.logout_button);
        
        // Tabs
        tabSongs = view.findViewById(R.id.tab_songs);
        tabPlaylists = view.findViewById(R.id.tab_playlists);
        tabSongsIndicator = view.findViewById(R.id.tab_songs_indicator);
        tabPlaylistsIndicator = view.findViewById(R.id.tab_playlists_indicator);
        
        // RecyclerViews
        songsRecyclerView = view.findViewById(R.id.songs_recycler_view);
        playlistsRecyclerView = view.findViewById(R.id.playlists_recycler_view);
    }
    
    /**
     * Setup RecyclerViews with adapters
     */
    private void setupRecyclerViews() {
        // Songs RecyclerView with uploader info
        songsAdapter = new SongWithUploaderInfoAdapter(new ArrayList<>(), new SongWithUploaderInfoAdapter.OnSongClick() {
            @Override
            public void onPlay(SongWithUploaderInfo songInfo) {
                showToast("Playing: " + songInfo.getTitle() + " by " + songInfo.getDisplayUploaderName());
            }

            @Override
            public void onOpenDetail(SongWithUploaderInfo songInfo) {
                showToast("Opening: " + songInfo.getTitle() + " by " + songInfo.getDisplayUploaderName());
            }
        });
        songsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        songsRecyclerView.setAdapter(songsAdapter);
        
        // Playlists RecyclerView
        playlistsAdapter = new PlaylistAdapter(new ArrayList<>(), new PlaylistAdapter.OnPlaylistClickListener() {
            @Override
            public void onPlaylistClick(Playlist playlist) {
                showToast("Opening playlist: " + playlist.getName());
            }

            @Override
            public void onPlayButtonClick(Playlist playlist) {
                showToast("Playing playlist: " + playlist.getName());
            }
        });
        playlistsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        playlistsRecyclerView.setAdapter(playlistsAdapter);
    }
    
    /**
     * Setup tab system
     */
    private void setupTabs() {
        switchTab(0); // Default to songs tab
    }
    
    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        // Tab clicks
        tabSongs.setOnClickListener(v -> switchTab(0));
        tabPlaylists.setOnClickListener(v -> switchTab(1));
        
        // Stats clicks
        followersContainer.setOnClickListener(v -> openFollowersList());
        followingContainer.setOnClickListener(v -> openFollowingList());

        // Button clicks
        followButton.setOnClickListener(v -> toggleFollow());
        editProfileButton.setOnClickListener(v -> editProfile());
        logoutButton.setOnClickListener(v -> showLogoutConfirmation());
    }
    
    /**
     * Setup ViewModel observers
     */
    private void setupObservers() {
        // Observe user data
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), this::updateUserInfo);
        
        // Observe own profile status
        viewModel.getIsOwnProfile().observe(getViewLifecycleOwner(), this::updateButtonVisibility);
        
        // Observe follow status
        viewModel.getIsFollowing().observe(getViewLifecycleOwner(), this::updateFollowButton);

        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Handle loading state if needed
            // For now, we'll just disable click listeners during loading
            if (isLoading != null) {
                followersContainer.setEnabled(!isLoading);
                followingContainer.setEnabled(!isLoading);
            }
        });

        // Observe error messages
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showToast(errorMessage);
                viewModel.clearErrorMessage();
            }
        });

        // Observe success messages
        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), successMessage -> {
            if (successMessage != null && !successMessage.isEmpty()) {
                showToast(successMessage);
                viewModel.clearSuccessMessage();
            }
        });
        
        // Observe user songs with uploader info
        viewModel.getPublicSongsWithUploaderInfo().observe(getViewLifecycleOwner(), songsWithInfo -> {
            if (songsWithInfo != null) {
                songsAdapter.updateData(songsWithInfo);
            }
        });

        // Observe user playlists
        viewModel.getPublicPlaylists().observe(getViewLifecycleOwner(), playlists -> {
            if (playlists != null) {
                playlistsAdapter.updateData(playlists);
                // Update playlists count
                playlistsCount.setText(String.valueOf(playlists.size()));
            }
        });
        
        // Observe stats
        viewModel.getFollowersCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                followersCount.setText(viewModel.getFollowersCountString());
            }
        });

        viewModel.getFollowingCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                followingCount.setText(viewModel.getFollowingCountString());
            }
        });

        viewModel.getSongsCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                songsCount.setText(viewModel.getSongsCountString());
            }
        });

    }
    
    /**
     * Switch between tabs
     */
    private void switchTab(int tabIndex) {
        currentTab = tabIndex;
        
        if (tabIndex == 0) {
            // Songs tab
            tabSongsIndicator.setVisibility(View.VISIBLE);
            tabPlaylistsIndicator.setVisibility(View.INVISIBLE);
            songsRecyclerView.setVisibility(View.VISIBLE);
            playlistsRecyclerView.setVisibility(View.GONE);
        } else {
            // Playlists tab
            tabSongsIndicator.setVisibility(View.INVISIBLE);
            tabPlaylistsIndicator.setVisibility(View.VISIBLE);
            songsRecyclerView.setVisibility(View.GONE);
            playlistsRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Update user info in UI
     */
    private void updateUserInfo(User user) {
        if (user == null) return;
        
        currentUser = user;
        displayName.setText(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
        username.setText("@" + user.getUsername());
        bio.setText(user.getBio() != null ? user.getBio() : "No bio available");
        
        // Load profile image if available
        // TODO: Implement image loading with Glide/Picasso
    }
    
    /**
     * Update button visibility based on own profile status
     */
    private void updateButtonVisibility(Boolean isOwnProfile) {
        if (isOwnProfile != null && isOwnProfile) {
            // Own profile: show Edit Profile and Logout buttons
            followButton.setVisibility(View.GONE);
            editProfileButton.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.VISIBLE);
        } else {
            // Other user's profile: show Follow button only
            followButton.setVisibility(View.VISIBLE);
            editProfileButton.setVisibility(View.GONE);
            logoutButton.setVisibility(View.GONE);
        }
    }
    
    /**
     * Update follow button state
     */
    private void updateFollowButton(Boolean isFollowing) {
        if (isFollowing != null) {
            followButton.setText(isFollowing ? "Unfollow" : "Follow");
        }
    }
    
    /**
     * Toggle follow status
     */
    private void toggleFollow() {
        if (currentUser != null) {
            viewModel.toggleFollowStatus();
        }
    }
    
    /**
     * Edit profile - Navigate to EditProfileFragment
     */
    private void editProfile() {
        // Navigate to EditProfileFragment
        EditProfileFragment editFragment = EditProfileFragment.newInstance();
        
        getParentFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, editFragment)
            .addToBackStack(null)
            .commit();
    }
    
    /**
     * Show logout confirmation dialog
     */
    private void showLogoutConfirmation() {
        if (getContext() == null) return;

        new android.app.AlertDialog.Builder(getContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout", (dialog, which) -> performLogout())
            .setNegativeButton("Cancel", null)
            .show();
    }

    /**
     * Perform logout operation
     */
    private void performLogout() {
        // Clear user session
        authManager.logout();

        // Show success message
        showToast("Logged out successfully");

        // Navigate to login screen
        navigateToLogin();
    }

    /**
     * Navigate to login screen
     */
    private void navigateToLogin() {
        if (getActivity() == null) return;

        // Create intent to LoginActivity
        Intent intent = new Intent(getActivity(), com.g3.soundify_musicplayer.data.Activity.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Finish current activity
        getActivity().finish();
    }

    /**
     * Open followers list
     */
    private void openFollowersList() {
        if (currentUser == null) {
            showToast("User data not loaded");
            return;
        }

        Intent intent = FollowersFollowingActivity.createIntent(
            requireContext(),
            currentUser.getId(),
            currentUser.getUsername(),
            FollowersFollowingActivity.TAB_FOLLOWERS
        );
        startActivity(intent);
    }

    /**
     * Open following list
     */
    private void openFollowingList() {
        if (currentUser == null) {
            showToast("User data not loaded");
            return;
        }

        Intent intent = FollowersFollowingActivity.createIntent(
            requireContext(),
            currentUser.getId(),
            currentUser.getUsername(),
            FollowersFollowingActivity.TAB_FOLLOWING
        );
        startActivity(intent);
    }

    /**
     * Show toast message
     */
    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
