package com.g3.soundify_musicplayer.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.g3.soundify_musicplayer.ui.playlist.PlaylistWithSongCountAdapter;
import com.g3.soundify_musicplayer.ui.song.SongAdapter;
import com.g3.soundify_musicplayer.ui.song.SongWithUploaderInfoAdapter;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.dto.PlaylistWithSongCount;
import com.g3.soundify_musicplayer.data.dto.SongWithUploaderInfo;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.model.NavigationContext;
import com.g3.soundify_musicplayer.ui.player.SongDetailViewModel;
import com.g3.soundify_musicplayer.ui.playlist.PlaylistDetailFragment;
import com.g3.soundify_musicplayer.viewmodel.HomeViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Library Fragment - Contains 3 sub-tabs: My Songs, My Playlists, Liked Songs
 * Updated to use real database data through LibraryViewModel
 */
public class LibraryFragment extends Fragment {

    // UI Components
    private TabLayout tabLayout;
    private RecyclerView mySongsRecyclerView;
    private RecyclerView myPlaylistsRecyclerView;
    private RecyclerView likedSongsRecyclerView;
    private LinearLayout emptyStateLayout;
    private TextView emptyStateTitle;
    private TextView emptyStateSubtitle;
    private Button buttonCreatePlaylist;

    // Adapters and ViewModel
    private SongWithUploaderInfoAdapter mySongsAdapter;
    private PlaylistWithSongCountAdapter myPlaylistsAdapter;
    private SongAdapter likedSongsAdapter;
    private LibraryViewModel libraryViewModel;
    private SongDetailViewModel songDetailViewModel;
    private HomeViewModel homeViewModel;

    // Current tab state
    private int currentTab = 0; // 0: My Songs, 1: My Playlists, 2: Liked Songs

    // Tab constants
    private static final int TAB_MY_SONGS = 0;
    private static final int TAB_MY_PLAYLISTS = 1;
    private static final int TAB_LIKED_SONGS = 2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        libraryViewModel = new ViewModelProvider(this).get(LibraryViewModel.class);
        songDetailViewModel = new ViewModelProvider(requireActivity()).get(SongDetailViewModel.class);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        initViews(view);
        setupTabs();
        setupRecyclerViews();
        observeViewModel();

        // Show initial tab
        switchTab(TAB_MY_SONGS);
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tab_layout);
        mySongsRecyclerView = view.findViewById(R.id.recycler_my_songs);
        myPlaylistsRecyclerView = view.findViewById(R.id.recycler_my_playlists);
        likedSongsRecyclerView = view.findViewById(R.id.recycler_liked_songs);
        emptyStateLayout = view.findViewById(R.id.empty_state_layout);
        emptyStateTitle = view.findViewById(R.id.empty_state_title);
        emptyStateSubtitle = view.findViewById(R.id.empty_state_subtitle);
        buttonCreatePlaylist = view.findViewById(R.id.button_create_playlist);

        // Setup create playlist button click listener
        buttonCreatePlaylist.setOnClickListener(v -> showCreatePlaylistDialog());
    }



    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_my_songs));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_my_playlists));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_liked_songs));

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

    private void setupRecyclerViews() {
        // Setup My Songs RecyclerView with Uploader Info
        mySongsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mySongsAdapter = new SongWithUploaderInfoAdapter(new ArrayList<>(), new SongWithUploaderInfoAdapter.OnSongClick() {
            @Override
            public void onPlay(SongWithUploaderInfo songInfo) {
                // Track recently played
                homeViewModel.trackRecentlyPlayed(songInfo.getId());

                showToast("Playing: " + songInfo.getTitle() + " by " + songInfo.getDisplayUploaderName());
                showMiniPlayerWithSongInfo(songInfo);
            }

            @Override
            public void onOpenDetail(SongWithUploaderInfo songInfo) {
                // Track recently played
                homeViewModel.trackRecentlyPlayed(songInfo.getId());

                showToast("Open detail: " + songInfo.getTitle() + " by " + songInfo.getDisplayUploaderName());

                // QUAN TRỌNG: Gọi method để phát nhạc với queue
                showMiniPlayerWithSongInfo(songInfo);
            }
        });
        mySongsRecyclerView.setAdapter(mySongsAdapter);

        // Setup My Playlists RecyclerView with Song Count
        myPlaylistsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        myPlaylistsAdapter = new PlaylistWithSongCountAdapter(new ArrayList<>(),
            new PlaylistWithSongCountAdapter.OnPlaylistClickListener() {
                @Override
                public void onPlaylistClick(PlaylistWithSongCount playlistWithSongCount) {
                    navigateToPlaylistDetail(playlistWithSongCount.getId());
                }

                @Override
                public void onPlayButtonClick(PlaylistWithSongCount playlistWithSongCount) {
                    if (playlistWithSongCount.getSongCount() > 0) {
                        showToast("Play playlist: " + playlistWithSongCount.getName());
                        // TODO: Start playing playlist
                    } else {
                        showToast("Playlist is empty");
                    }
                }
            });
        myPlaylistsRecyclerView.setAdapter(myPlaylistsAdapter);

        // Setup Liked Songs RecyclerView
        likedSongsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        likedSongsAdapter = new SongAdapter(new ArrayList<>(), new SongAdapter.OnSongClick() {
            @Override
            public void onPlay(Song song) {
                // Track recently played
                homeViewModel.trackRecentlyPlayed(song.getId());

                showToast("Playing liked song: " + song.getTitle());
                showMiniPlayer(song);
            }

            @Override
            public void onOpenDetail(Song song) {
                // Track recently played
                homeViewModel.trackRecentlyPlayed(song.getId());

                showToast("Open detail: " + song.getTitle());

                // QUAN TRỌNG: Gọi method để phát nhạc với queue
                showMiniPlayer(song);
            }
        });
        likedSongsRecyclerView.setAdapter(likedSongsAdapter);
    }

    private void observeViewModel() {
        // Observe My Songs with Uploader Info
        libraryViewModel.getMySongsWithUploaderInfo().observe(getViewLifecycleOwner(), songsWithInfo -> {
            if (songsWithInfo != null) {
                mySongsAdapter.updateData(songsWithInfo);
                // Update empty state if this is the current tab
                if (currentTab == TAB_MY_SONGS) {
                    updateEmptyState(songsWithInfo.isEmpty(), "No songs uploaded", "Upload your first song to see it here");
                }
            }
        });

        // Observe My Playlists with Song Count
        libraryViewModel.getMyPlaylistsWithSongCount().observe(getViewLifecycleOwner(), playlistsWithSongCount -> {
            if (playlistsWithSongCount != null) {
                myPlaylistsAdapter.updateData(playlistsWithSongCount);
                // Update empty state if this is the current tab
                if (currentTab == TAB_MY_PLAYLISTS) {
                    updateEmptyState(playlistsWithSongCount.isEmpty(), "No playlists created", "Create your first playlist to organize your music");
                }
            }
        });

        // Observe Liked Songs
        libraryViewModel.getLikedSongs().observe(getViewLifecycleOwner(), songs -> {
            if (songs != null) {
                likedSongsAdapter.updateData(songs);
                // Update empty state if this is the current tab
                if (currentTab == TAB_LIKED_SONGS) {
                    updateEmptyState(songs.isEmpty(), "No liked songs", "Like songs to see them here");
                }
            }
        });

        // Observe loading state
        libraryViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                // Show loading indicator - disable create button and show loading text
                buttonCreatePlaylist.setEnabled(false);
                buttonCreatePlaylist.setText("Creating...");
            } else {
                buttonCreatePlaylist.setEnabled(true);
                buttonCreatePlaylist.setText(getString(R.string.create_playlist));
            }
        });

        // Observe error messages
        libraryViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showToast(errorMessage);
                libraryViewModel.clearErrorMessage();
            }
        });

        // Observe success messages
        libraryViewModel.getSuccessMessage().observe(getViewLifecycleOwner(), successMessage -> {
            if (successMessage != null && !successMessage.isEmpty()) {
                showToast(successMessage);
                libraryViewModel.clearSuccessMessage();
            }
        });
    }

    private void switchTab(int tabIndex) {
        // Hide all RecyclerViews
        mySongsRecyclerView.setVisibility(View.GONE);
        myPlaylistsRecyclerView.setVisibility(View.GONE);
        likedSongsRecyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);

        // Show the selected tab's RecyclerView and check empty state
        switch (tabIndex) {
            case TAB_MY_SONGS:
                mySongsRecyclerView.setVisibility(View.VISIBLE);
                checkEmptyStateForCurrentTab();
                break;
            case TAB_MY_PLAYLISTS:
                myPlaylistsRecyclerView.setVisibility(View.VISIBLE);
                checkEmptyStateForCurrentTab();
                break;
            case TAB_LIKED_SONGS:
                // Navigate to LikedSongPlaylistFragment instead of showing local RecyclerView
                navigateToLikedSongsPlaylist();
                break;
        }
    }

    private void checkEmptyStateForCurrentTab() {
        switch (currentTab) {
            case TAB_MY_SONGS:
                if (mySongsAdapter.getItemCount() == 0) {
                    updateEmptyState(true, "No songs uploaded", "Upload your first song to see it here");
                }
                break;
            case TAB_MY_PLAYLISTS:
                if (myPlaylistsAdapter.getItemCount() == 0) {
                    updateEmptyState(true, "No playlists created", "Create your first playlist to organize your music");
                }
                break;
            case TAB_LIKED_SONGS:
                if (likedSongsAdapter.getItemCount() == 0) {
                    updateEmptyState(true, "No liked songs", "Like songs to see them here");
                }
                break;
        }
    }

    private void updateEmptyState(boolean isEmpty, String title, String subtitle) {
        if (isEmpty) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            emptyStateTitle.setText(title);
            emptyStateSubtitle.setText(subtitle);

            // Show create playlist button only for playlist tab
            if (currentTab == TAB_MY_PLAYLISTS && title.contains("playlist")) {
                buttonCreatePlaylist.setVisibility(View.VISIBLE);
                emptyStateSubtitle.setVisibility(View.GONE); // Hide subtitle when button is shown
            } else {
                buttonCreatePlaylist.setVisibility(View.GONE);
                emptyStateSubtitle.setVisibility(View.VISIBLE);
            }
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            buttonCreatePlaylist.setVisibility(View.GONE);
        }
    }

    private void showMiniPlayer(Song song) {
        // Use SongWithUploaderInfo approach instead of mock artist
        // This method is deprecated - use showMiniPlayerWithSongInfo instead
        android.util.Log.w("LibraryFragment", "showMiniPlayer with mock artist is deprecated");

        // Create basic user info from song
        User basicUser = new User("user_" + song.getUploaderId(), "User " + song.getUploaderId(), "user@example.com", "");
        basicUser.setId(song.getUploaderId());

        // TẠO NAVIGATION CONTEXT dựa trên tab hiện tại
        createLibraryNavigationContextAndPlay(song, basicUser);
    }

    private void showMiniPlayerWithSongInfo(SongWithUploaderInfo songInfo) {
        // Convert SongWithUploaderInfo to Song and User
        Song song = convertToSong(songInfo);
        User uploader = convertToUser(songInfo);

        // TẠO NAVIGATION CONTEXT dựa trên tab hiện tại
        createLibraryNavigationContextAndPlay(song, uploader);
    }

    /**
     * Tạo NavigationContext từ Library tab hiện tại và phát bài hát với queue
     */
    private void createLibraryNavigationContextAndPlay(Song song, User artist) {
        java.util.List<Song> currentSongs = null;
        String contextTitle = "";

        // Lấy danh sách songs dựa trên tab hiện tại
        switch (currentTab) {
            case 0: // My Songs
                // Convert SongWithUploaderInfo to Song for navigation context
                List<SongWithUploaderInfo> songsWithInfo = mySongsAdapter.getCurrentData();
                currentSongs = new ArrayList<>();
                if (songsWithInfo != null) {
                    for (SongWithUploaderInfo songInfo : songsWithInfo) {
                        currentSongs.add(convertToSong(songInfo));
                    }
                }
                contextTitle = "My Songs";
                break;
            case 2: // Liked Songs
                currentSongs = likedSongsAdapter.getCurrentData();
                contextTitle = "Liked Songs";
                break;
            default:
                // Fallback cho tab Playlists hoặc unknown
                java.util.List<Long> singleSongIds = new java.util.ArrayList<>();
                singleSongIds.add(song.getId());
                NavigationContext fallbackContext = NavigationContext.fromGeneral(
                    "Library", singleSongIds, 0);
                songDetailViewModel.playSongWithContext(song, artist, fallbackContext);
                return;
        }

        if (currentSongs == null || currentSongs.isEmpty()) {
            // Fallback: phát bài đơn lẻ nếu không có danh sách
            songDetailViewModel.playSong(song, artist);
            android.util.Log.w("LibraryFragment", "No songs list available, playing single song");
            return;
        }

        // Tạo danh sách song IDs và tìm vị trí của bài hát được click
        java.util.List<Long> songIds = new java.util.ArrayList<>();
        int clickedPosition = 0;

        for (int i = 0; i < currentSongs.size(); i++) {
            Song s = currentSongs.get(i);
            songIds.add(s.getId());

            if (s.getId() == song.getId()) {
                clickedPosition = i;
            }
        }

        // Tạo NavigationContext từ Library
        NavigationContext context = NavigationContext.fromGeneral(
            "Library - " + contextTitle,
            songIds,
            clickedPosition
        );

        // Phát bài hát với context để tạo queue
        android.util.Log.d("LibraryFragment", "About to play song: " + song.getTitle() +
            " by " + artist.getDisplayName() + ", Audio URL: " + song.getAudioUrl());

        songDetailViewModel.playSongWithContext(song, artist, context);

        android.util.Log.d("LibraryFragment", "Playing song with context - Tab: " +
            contextTitle + ", Queue size: " + songIds.size() +
            ", Position: " + clickedPosition);
    }

    // Mock artist method removed - using real user data from database

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show dialog to create new playlist
     */
    private void showCreatePlaylistDialog() {
        if (getContext() == null) return;

        // Create dialog with input field
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Create New Playlist");

        // Create input field
        final android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint("Playlist name");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);

        // Add padding to input
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        builder.setView(input);

        // Set buttons
        builder.setPositiveButton("Create", (dialog, which) -> {
            String playlistName = input.getText().toString().trim();
            if (!playlistName.isEmpty()) {
                createNewPlaylist(playlistName);
            } else {
                showToast("Please enter a playlist name");
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        // Show dialog
        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        // Focus on input and show keyboard
        input.requestFocus();
        android.view.inputmethod.InputMethodManager imm =
            (android.view.inputmethod.InputMethodManager) getContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Create new playlist with given name
     */
    private void createNewPlaylist(String playlistName) {
        if (libraryViewModel != null) {
            libraryViewModel.createPlaylist(playlistName);
        } else {
            showToast("Error: ViewModel not initialized");
        }
    }

    /**
     * Navigate to playlist detail fragment
     */
    private void navigateToPlaylistDetail(long playlistId) {
        if (getActivity() == null) return;

        PlaylistDetailFragment fragment = PlaylistDetailFragment.newInstance(playlistId);

        getActivity().getSupportFragmentManager()
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,  // enter
                R.anim.slide_out_left,  // exit
                R.anim.slide_in_left,   // popEnter
                R.anim.slide_out_right  // popExit
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("playlist_detail")
            .commit();
    }

    /**
     * Navigate to liked songs playlist fragment
     */
    private void navigateToLikedSongsPlaylist() {
        android.util.Log.d("LibraryFragment", "🔄 Navigating to LikedSongPlaylistFragment");
        
        if (getActivity() == null) {
            android.util.Log.e("LibraryFragment", "❌ Activity is null, cannot navigate");
            return;
        }

        LikedSongPlaylistFragment fragment = LikedSongPlaylistFragment.newInstance();

        getActivity().getSupportFragmentManager()
            .beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,  // enter
                R.anim.slide_out_left,  // exit
                R.anim.slide_in_left,   // popEnter
                R.anim.slide_out_right  // popExit
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack("liked_songs_playlist")
            .commit();
            
        android.util.Log.d("LibraryFragment", "✅ Navigation to LikedSongPlaylistFragment completed");
    }

    /**
     * Convert SongWithUploaderInfo to Song
     */
    private Song convertToSong(SongWithUploaderInfo songInfo) {
        Song song = new Song(songInfo.getUploaderId(), songInfo.getTitle(), songInfo.getAudioUrl());
        song.setId(songInfo.getId());
        song.setDescription(songInfo.getDescription());
        song.setCoverArtUrl(songInfo.getCoverArtUrl());
        song.setGenre(songInfo.getGenre());
        song.setDurationMs(songInfo.getDurationMs());
        song.setPublic(songInfo.isPublic());
        song.setCreatedAt(songInfo.getCreatedAt());
        return song;
    }

    /**
     * Convert SongWithUploaderInfo to User
     */
    private User convertToUser(SongWithUploaderInfo songInfo) {
        User user = new User(songInfo.getUploaderUsername(), songInfo.getUploaderDisplayName(), "dummy@email.com", "password");
        user.setId(songInfo.getUploaderId());
        user.setAvatarUrl(songInfo.getUploaderAvatarUrl());
        return user;
    }
}
