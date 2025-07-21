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
import com.g3.soundify_musicplayer.data.Adapter.PlaylistWithSongCountAdapter;
import com.g3.soundify_musicplayer.data.Adapter.SongAdapter;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.dto.PlaylistWithSongCount;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.ui.player.SongDetailViewModel;
import com.g3.soundify_musicplayer.ui.playlist.PlaylistDetailFragment;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Arrays;
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
    private SongAdapter mySongsAdapter;
    private PlaylistWithSongCountAdapter myPlaylistsAdapter;
    private SongAdapter likedSongsAdapter;
    private LibraryViewModel libraryViewModel;
    private SongDetailViewModel songDetailViewModel; // UNIFIED ViewModel for mini player

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

        // Initialize ViewModels
        libraryViewModel = new ViewModelProvider(this).get(LibraryViewModel.class);
        // Initialize ViewModel THỐNG NHẤT - Activity-scoped SongDetailViewModel
        songDetailViewModel = new ViewModelProvider(requireActivity()).get(SongDetailViewModel.class);

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
        // Setup My Songs RecyclerView
        mySongsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mySongsAdapter = new SongAdapter(new ArrayList<>(), new SongAdapter.OnSongClick() {
            @Override
            public void onPlay(Song song) {
                showToast("Playing: " + song.getTitle());
                showMiniPlayer(song);
            }

            @Override
            public void onOpenDetail(Song song) {
                showToast("Open detail: " + song.getTitle());
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
                showToast("Playing liked song: " + song.getTitle());
                showMiniPlayer(song);
            }

            @Override
            public void onOpenDetail(Song song) {
                showToast("Open detail: " + song.getTitle());
            }
        });
        likedSongsRecyclerView.setAdapter(likedSongsAdapter);
    }

    private void observeViewModel() {
        // Observe My Songs
        libraryViewModel.getMySongs().observe(getViewLifecycleOwner(), songs -> {
            if (songs != null) {
                mySongsAdapter.updateData(songs);
                // Update empty state if this is the current tab
                if (currentTab == TAB_MY_SONGS) {
                    updateEmptyState(songs.isEmpty(), "No songs uploaded", "Upload your first song to see it here");
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
                likedSongsRecyclerView.setVisibility(View.VISIBLE);
                checkEmptyStateForCurrentTab();
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
        // Create a mock artist for the song
        User mockArtist = createMockArtist(song.getUploaderId());

        // Show mini player using UNIFIED SongDetailViewModel
        songDetailViewModel.playSong(song, mockArtist);
    }

    private User createMockArtist(long artistId) {
        User artist = new User();
        artist.setId(artistId);
        artist.setUsername("demo_artist");
        artist.setDisplayName("Demo Artist");
        artist.setAvatarUrl("mock://avatar/demo_artist.jpg");
        artist.setCreatedAt(System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)); // 30 days ago
        return artist;
    }

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
}
