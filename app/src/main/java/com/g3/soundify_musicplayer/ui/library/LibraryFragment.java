package com.g3.soundify_musicplayer.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.materialswitch.MaterialSwitch;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.ui.playlist.PlaylistWithSongCountAdapter;
import com.g3.soundify_musicplayer.ui.song.SongAdapter;
import com.g3.soundify_musicplayer.ui.song.SongWithUploaderInfoAdapter;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.dto.PlaylistWithSongCount;
import com.g3.soundify_musicplayer.data.dto.SongWithUploaderInfo;
import com.g3.soundify_musicplayer.data.entity.User;

// REMOVED: SimplePlaybackHandler import - using Zero Queue Rule
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

                // ✅ CONSISTENT: Use playFromView for all fragments
                List<Song> allSongs = convertToSongs(mySongsAdapter.getCurrentData());
                int position = findSongPosition(songInfo, mySongsAdapter.getCurrentData());
                songDetailViewModel.playFromView(allSongs, "My Songs", position);
            }

            @Override
            public void onOpenDetail(SongWithUploaderInfo songInfo) {
                // Track recently played
                homeViewModel.trackRecentlyPlayed(songInfo.getId());

                showToast("Open detail: " + songInfo.getTitle() + " by " + songInfo.getDisplayUploaderName());

                // ✅ CONSISTENT: Use playFromView for all fragments
                List<Song> allSongs = convertToSongs(mySongsAdapter.getCurrentData());
                int position = findSongPosition(songInfo, mySongsAdapter.getCurrentData());
                songDetailViewModel.playFromView(allSongs, "My Songs", position);
            }

            @Override
            public void onEditSong(SongWithUploaderInfo songInfo) {
                android.util.Log.d("LibraryFragment", "Edit song requested: " + songInfo.getTitle());
                showEditSongDialog(songInfo);
            }

            @Override
            public void onDeleteSong(SongWithUploaderInfo songInfo) {
                android.util.Log.d("LibraryFragment", "Delete song requested: " + songInfo.getTitle());
                showDeleteSongConfirmation(songInfo);
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

                @Override
                public void onEditPlaylist(PlaylistWithSongCount playlistWithSongCount) {
                    android.util.Log.d("LibraryFragment", "Edit playlist requested: " + playlistWithSongCount.getName());
                    showEditPlaylistDialog(playlistWithSongCount);
                }

                @Override
                public void onDeletePlaylist(PlaylistWithSongCount playlistWithSongCount) {
                    android.util.Log.d("LibraryFragment", "Delete playlist requested: " + playlistWithSongCount.getName());
                    showDeletePlaylistConfirmation(playlistWithSongCount);
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

                // Enable owner options for My Songs (user owns all songs in this tab)
                long currentUserId = libraryViewModel.getCurrentUserId();
                mySongsAdapter.setOwnerOptions(true, currentUserId);

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

                // Enable owner options for My Playlists (user owns all playlists in this tab)
                long currentUserId = libraryViewModel.getCurrentUserId();
                myPlaylistsAdapter.setOwnerOptions(true, currentUserId);

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

        // Observe success messages from SongDetailViewModel
        songDetailViewModel.getSuccessMessage().observe(getViewLifecycleOwner(), successMessage -> {
            if (successMessage != null && !successMessage.isEmpty()) {
                android.util.Log.d("LibraryFragment", "Success message observed: " + successMessage);
                showToast(successMessage);
                songDetailViewModel.clearSuccessMessage(); // Clear after showing

                // Refresh My Songs data after successful operations
                libraryViewModel.refreshTab(TAB_MY_SONGS);
            }
        });

        // Observe error messages from SongDetailViewModel
        songDetailViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                android.util.Log.e("LibraryFragment", "Error message observed: " + errorMessage);
                showToast(errorMessage);
                // Note: Don't clear error message here as it's handled by SongDetailViewModel
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

        // ✅ CONSISTENT: Use playFromView for all fragments
        List<Song> allSongs = getCurrentTabSongs();
        int position = findSongPositionInSongList(song, allSongs);
        songDetailViewModel.playFromView(allSongs, getCurrentTabTitle(), position);
    }


    /**
     * ✅ Get current tab songs as Song objects
     */
    private List<Song> getCurrentTabSongs() {
        int currentTab = tabLayout.getSelectedTabPosition();
        switch (currentTab) {
            case 0: // My Songs
                return convertToSongs(mySongsAdapter != null ? mySongsAdapter.getCurrentData() : new ArrayList<>());
            case 2: // Liked Songs
                return likedSongsAdapter != null ? likedSongsAdapter.getCurrentData() : new ArrayList<>();
            default:
                return new ArrayList<>();
        }
    }

    /**
     * ✅ Get current tab title
     */
    private String getCurrentTabTitle() {
        int currentTab = tabLayout.getSelectedTabPosition();
        switch (currentTab) {
            case 0: return "My Songs";
            case 1: return "My Playlists";
            case 2: return "Liked Songs";
            default: return "Library";
        }
    }

    // ========== 🗑️ REMOVED: Complex NavigationContext method ==========
    // Replaced by SimplePlaybackHandler.playFromCurrentView() pattern

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
        if (getActivity() == null) {
            android.util.Log.e("LibraryFragment", "Activity is null, cannot navigate");
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
    }

    // ========== 🔥 ZERO QUEUE RULE - HELPER METHODS ==========

    /**
     * ✅ Convert list of SongWithUploaderInfo to Song objects
     */
    private List<Song> convertToSongs(List<SongWithUploaderInfo> songInfoList) {
        List<Song> songs = new ArrayList<>();
        if (songInfoList != null) {
            for (SongWithUploaderInfo songInfo : songInfoList) {
                songs.add(convertToSong(songInfo));
            }
        }
        return songs;
    }

    /**
     * Helper method để tìm position của song trong list
     */
    private int findSongPosition(SongWithUploaderInfo targetSong, List<SongWithUploaderInfo> songList) {
        if (targetSong == null || songList == null) return 0;

        for (int i = 0; i < songList.size(); i++) {
            SongWithUploaderInfo song = songList.get(i);
            if (song != null && song.getId() == targetSong.getId()) {
                return i;
            }
        }
        return 0; // Default to first position if not found
    }

    /**
     * Helper method để tìm position của Song trong Song list
     */
    private int findSongPositionInSongList(Song targetSong, List<Song> songList) {
        if (targetSong == null || songList == null) return 0;

        for (int i = 0; i < songList.size(); i++) {
            Song song = songList.get(i);
            if (song != null && song.getId() == targetSong.getId()) {
                return i;
            }
        }
        return 0; // Default to first position if not found
    }

    // REMOVED: getCurrentViewSongsInfo - replaced by getCurrentTabSongsInfo

    /**
     * Convert SongWithUploaderInfo to Song
     * Made public to match interface requirement
     */
    public Song convertToSong(SongWithUploaderInfo songInfo) {
        if (songInfo == null) return null;

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
     * Show edit song dialog
     */
    private void showEditSongDialog(SongWithUploaderInfo songInfo) {
        if (getContext() == null) return;

        android.util.Log.d("LibraryFragment", "Showing edit dialog for song: " + songInfo.getTitle());

        // Inflate dialog layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_song, null);

        // Get views
        TextInputLayout titleInputLayout = dialogView.findViewById(R.id.text_input_layout_song_title);
        TextInputEditText titleEditText = dialogView.findViewById(R.id.edit_text_song_title);
        TextInputLayout descriptionInputLayout = dialogView.findViewById(R.id.text_input_layout_song_description);
        TextInputEditText descriptionEditText = dialogView.findViewById(R.id.edit_text_song_description);
        TextInputLayout genreInputLayout = dialogView.findViewById(R.id.text_input_layout_song_genre);
        TextInputEditText genreEditText = dialogView.findViewById(R.id.edit_text_song_genre);
        MaterialSwitch publicSwitch = dialogView.findViewById(R.id.switch_song_public);
        Button cancelButton = dialogView.findViewById(R.id.button_cancel);
        Button saveButton = dialogView.findViewById(R.id.button_save);

        // Pre-fill current song data
        titleEditText.setText(songInfo.getTitle());
        descriptionEditText.setText(songInfo.getDescription());
        genreEditText.setText(songInfo.getGenre());
        publicSwitch.setChecked(songInfo.isPublic());

        // Move cursor to end
        titleEditText.setSelection(titleEditText.getText().length());

        // Create dialog
        AlertDialog dialog = new MaterialAlertDialogBuilder(getContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Setup validation
        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateSongInput(titleEditText.getText().toString(),
                                descriptionEditText.getText().toString(),
                                titleInputLayout, descriptionInputLayout, saveButton);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        titleEditText.addTextChangedListener(validationWatcher);
        descriptionEditText.addTextChangedListener(validationWatcher);

        // Setup button listeners
        cancelButton.setOnClickListener(v -> {
            android.util.Log.d("LibraryFragment", "Edit dialog cancelled");
            dialog.dismiss();
        });

        saveButton.setOnClickListener(v -> {
            String newTitle = titleEditText.getText().toString().trim();
            String newDescription = descriptionEditText.getText().toString().trim();
            String newGenre = genreEditText.getText().toString().trim();
            boolean isPublic = publicSwitch.isChecked();

            android.util.Log.d("LibraryFragment", "Saving song with new data - Title: " + newTitle);

            if (validateSongInput(newTitle, newDescription, titleInputLayout, descriptionInputLayout, saveButton)) {
                // Update song via ViewModel
                long currentUserId = libraryViewModel.getCurrentUserId();
                songDetailViewModel.updateSongInfo(songInfo.getId(), currentUserId, newTitle, newDescription, newGenre, isPublic, null);
                dialog.dismiss();
            }
        });

        // Initial validation
        validateSongInput(songInfo.getTitle(), songInfo.getDescription(), titleInputLayout, descriptionInputLayout, saveButton);

        dialog.show();

        // Focus on title field
        titleEditText.requestFocus();
    }

    /**
     * Show delete song confirmation dialog
     */
    private void showDeleteSongConfirmation(SongWithUploaderInfo songInfo) {
        if (getContext() == null) return;

        String message = getString(R.string.confirm_delete_song_message, songInfo.getTitle());

        new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.confirm_delete_song)
                .setMessage(message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    // Perform deletion
                    performDeleteSong(songInfo);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Actually delete the song
     */
    private void performDeleteSong(SongWithUploaderInfo songInfo) {
        android.util.Log.d("LibraryFragment", "Performing delete for song: " + songInfo.getTitle());

        try {
            long currentUserId = libraryViewModel.getCurrentUserId();
            songDetailViewModel.deleteSong(songInfo.getId(), currentUserId);

            android.util.Log.d("LibraryFragment", "Successfully initiated song deletion");

        } catch (Exception e) {
            android.util.Log.e("LibraryFragment", "Error deleting song", e);
            showToast(getString(R.string.error_deleting_song));
        }
    }

    /**
     * Validate song input and update UI accordingly
     */
    private boolean validateSongInput(String title, String description,
                                    TextInputLayout titleLayout, TextInputLayout descriptionLayout,
                                    Button saveButton) {
        boolean isValid = true;

        // Validate title
        if (title == null || title.trim().isEmpty()) {
            titleLayout.setError(getString(R.string.song_title_empty));
            isValid = false;
        } else if (title.trim().length() > 100) {
            titleLayout.setError(getString(R.string.song_title_too_long));
            isValid = false;
        } else {
            titleLayout.setError(null);
        }

        // Validate description
        if (description != null && description.length() > 500) {
            descriptionLayout.setError(getString(R.string.song_description_too_long));
            isValid = false;
        } else {
            descriptionLayout.setError(null);
        }

        saveButton.setEnabled(isValid);
        return isValid;
    }

    /**
     * Show edit playlist dialog for playlist card
     */
    private void showEditPlaylistDialog(PlaylistWithSongCount playlistWithSongCount) {
        if (getContext() == null) return;

        android.util.Log.d("LibraryFragment", "Showing edit dialog for playlist: " + playlistWithSongCount.getName());

        // Inflate dialog layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_playlist, null);

        // Get views
        TextInputLayout nameInputLayout = dialogView.findViewById(R.id.text_input_layout_playlist_name);
        TextInputEditText nameEditText = dialogView.findViewById(R.id.edit_text_playlist_name);
        Button cancelButton = dialogView.findViewById(R.id.button_cancel);
        Button saveButton = dialogView.findViewById(R.id.button_save);

        // Pre-fill current playlist data
        nameEditText.setText(playlistWithSongCount.getName());

        // Update button text
        saveButton.setText(getString(R.string.save));

        // Move cursor to end
        nameEditText.setSelection(nameEditText.getText().length());

        // Create dialog
        AlertDialog dialog = new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.edit_playlist_title)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Setup validation
        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePlaylistInputSimple(nameEditText.getText().toString(), nameInputLayout, saveButton);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        nameEditText.addTextChangedListener(validationWatcher);

        // Setup button listeners
        cancelButton.setOnClickListener(v -> {
            android.util.Log.d("LibraryFragment", "Edit playlist dialog cancelled");
            dialog.dismiss();
        });

        saveButton.setOnClickListener(v -> {
            String newName = nameEditText.getText().toString().trim();

            android.util.Log.d("LibraryFragment", "Saving playlist with new data - Name: " + newName);

            if (validatePlaylistInputSimple(newName, nameInputLayout, saveButton)) {
                // Update playlist via ViewModel (keep existing description and public status)
                libraryViewModel.updatePlaylist(playlistWithSongCount.getId(), newName,
                    playlistWithSongCount.getDescription(), playlistWithSongCount.isPublic());
                dialog.dismiss();
            }
        });

        // Initial validation
        validatePlaylistInputSimple(playlistWithSongCount.getName(), nameInputLayout, saveButton);

        dialog.show();

        // Focus on name field
        nameEditText.requestFocus();
    }

    /**
     * Show delete playlist confirmation dialog for playlist card
     */
    private void showDeletePlaylistConfirmation(PlaylistWithSongCount playlistWithSongCount) {
        if (getContext() == null) return;

        String message = getString(R.string.confirm_delete_playlist_message, playlistWithSongCount.getName());

        new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.confirm_delete_playlist)
                .setMessage(message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    // Perform deletion
                    performDeletePlaylist(playlistWithSongCount);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Actually delete the playlist from card
     */
    private void performDeletePlaylist(PlaylistWithSongCount playlistWithSongCount) {
        android.util.Log.d("LibraryFragment", "Performing delete for playlist: " + playlistWithSongCount.getName());

        try {
            libraryViewModel.deletePlaylist(playlistWithSongCount.getId());
            android.util.Log.d("LibraryFragment", "Successfully initiated playlist deletion");

        } catch (Exception e) {
            android.util.Log.e("LibraryFragment", "Error deleting playlist", e);
            showToast(getString(R.string.error_deleting_playlist));
        }
    }

    /**
     * Simple validation for playlist name only
     */
    private boolean validatePlaylistInputSimple(String name, TextInputLayout nameLayout, Button saveButton) {
        boolean isValid = true;

        // Validate name
        if (name == null || name.trim().isEmpty()) {
            nameLayout.setError(getString(R.string.playlist_name_empty));
            isValid = false;
        } else if (name.trim().length() > 50) {
            nameLayout.setError(getString(R.string.playlist_name_too_long));
            isValid = false;
        } else {
            nameLayout.setError(null);
        }

        saveButton.setEnabled(isValid);
        return isValid;
    }
}
