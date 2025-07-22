package com.g3.soundify_musicplayer.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;

import com.g3.soundify_musicplayer.ui.player.FullPlayerFragment;
import com.g3.soundify_musicplayer.data.entity.User;
// REMOVED: NavigationContext import - using Zero Queue Rule
import com.g3.soundify_musicplayer.ui.player.FullPlayerActivity;
import com.g3.soundify_musicplayer.ui.player.SongDetailViewModel;
import com.g3.soundify_musicplayer.ui.profile.UserProfileFragment;
import com.g3.soundify_musicplayer.viewmodel.HomeViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

/**
 * Fragment for the Search screen.
 * Provides comprehensive search functionality for songs, artists, and playlists.
 * UI ONLY - No backend integration, uses mock data for demo purposes.
 */
public class SearchFragment extends Fragment implements SearchAdapter.OnSearchResultClickListener {

    // UI Components
    private TextInputEditText editTextSearch;
    private ChipGroup chipGroupFilters;
    private Chip chipAll, chipSongs, chipArtists, chipPlaylists;
    private TextView textResultsCount;
    private RecyclerView recyclerSearchResults;
    private LinearLayout layoutLoading;
    private LinearLayout layoutEmpty;
    private TextView textEmptyTitle;
    private TextView textEmptySubtitle;

    // ViewModel and Adapter
    private SearchViewModel viewModel;
    private SearchAdapter adapter;
    private SongDetailViewModel songDetailViewModel;
    private HomeViewModel homeViewModel;

    // Current state
    private String currentQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupViewModel();
        setupSearchInput();
        setupFilterChips();
        observeViewModel();
    }

    private void initViews(View view) {
        editTextSearch = view.findViewById(R.id.edit_text_search);
        chipGroupFilters = view.findViewById(R.id.chip_group_filters);
        chipAll = view.findViewById(R.id.chip_all);
        chipSongs = view.findViewById(R.id.chip_songs);
        chipArtists = view.findViewById(R.id.chip_artists);
        chipPlaylists = view.findViewById(R.id.chip_playlists);
        textResultsCount = view.findViewById(R.id.text_results_count);
        recyclerSearchResults = view.findViewById(R.id.recycler_search_results);
        layoutLoading = view.findViewById(R.id.layout_loading);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        textEmptyTitle = view.findViewById(R.id.text_empty_title);
        textEmptySubtitle = view.findViewById(R.id.text_empty_subtitle);
    }

    private void setupRecyclerView() {
        adapter = new SearchAdapter(requireContext());
        adapter.setOnSearchResultClickListener(this);

        recyclerSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerSearchResults.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);
        songDetailViewModel = new ViewModelProvider(requireActivity()).get(SongDetailViewModel.class);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        // Set current user ID in adapter
        adapter.setCurrentUserId(viewModel.getCurrentUserId());
    }

    private void setupSearchInput() {
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (!query.equals(currentQuery)) {
                    currentQuery = query;
                    viewModel.search(query);
                }
            }
        });
    }

    private void setupFilterChips() {
        // Set default selection
        chipAll.setChecked(true);

        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                // If no chip is selected, default to "All"
                chipAll.setChecked(true);
                return;
            }

            int checkedId = checkedIds.get(0);
            SearchViewModel.FilterType filter;

            if (checkedId == R.id.chip_all) {
                filter = SearchViewModel.FilterType.ALL;
            } else if (checkedId == R.id.chip_songs) {
                filter = SearchViewModel.FilterType.SONGS;
            } else if (checkedId == R.id.chip_artists) {
                filter = SearchViewModel.FilterType.ARTISTS;
            } else if (checkedId == R.id.chip_playlists) {
                filter = SearchViewModel.FilterType.PLAYLISTS;
            } else {
                filter = SearchViewModel.FilterType.ALL;
            }

            viewModel.setFilter(filter);
        });
    }

    private void observeViewModel() {
        // Observe search results
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), results -> {
            if (results != null) {
                adapter.setSearchResults(results);
                updateResultsCount(results.size());
                updateUIState(results.isEmpty() && !currentQuery.isEmpty(), false);
            }
        });

        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                layoutLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                if (isLoading) {
                    layoutEmpty.setVisibility(View.GONE);
                    recyclerSearchResults.setVisibility(View.GONE);
                    textResultsCount.setVisibility(View.GONE);
                }
            }
        });

        // Observe current query
        viewModel.getCurrentQuery().observe(getViewLifecycleOwner(), query -> {
            if (query != null) {
                currentQuery = query;
                // Don't show empty state for empty query - let search results handle it
            }
        });

        // Observe errors
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                showToast("Search error: " + error);
            }
        });

        // Observe following user IDs
        viewModel.getFollowingUserIds().observe(getViewLifecycleOwner(), followingIds -> {
            if (followingIds != null) {
                adapter.setFollowingUserIds(followingIds);
            }
        });

        // Observe follow messages
        viewModel.getFollowMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                showToast(message);
            }
        });
    }

    private void updateResultsCount(int count) {
        if (count > 0 && !currentQuery.isEmpty()) {
            textResultsCount.setVisibility(View.VISIBLE);
            if (count == 1) {
                textResultsCount.setText(getString(R.string.search_results_count_single));
            } else {
                textResultsCount.setText(getString(R.string.search_results_count, count));
            }
        } else {
            textResultsCount.setVisibility(View.GONE);
        }
    }

    private void updateUIState(boolean isEmpty, boolean isInitialState) {
        if (isEmpty) {
            // Show empty results state
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerSearchResults.setVisibility(View.GONE);
            textEmptyTitle.setText(getString(R.string.search_empty_title));
            textEmptySubtitle.setText(getString(R.string.search_empty_subtitle));
        } else if (isInitialState) {
            // Show initial state (no search yet)
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerSearchResults.setVisibility(View.GONE);
            textEmptyTitle.setText(getString(R.string.search_empty_query_title));
            textEmptySubtitle.setText(getString(R.string.search_empty_query_subtitle));
        } else {
            // Show results
            layoutEmpty.setVisibility(View.GONE);
            recyclerSearchResults.setVisibility(View.VISIBLE);
        }
    }

    // SearchAdapter.OnSearchResultClickListener implementation
    @Override
    public void onSongClick(SearchResult result) {
        if (result.getSong() != null && result.getUser() != null) {
            // Track recently played
            homeViewModel.trackRecentlyPlayed(result.getSong().getId());

            // ✅ CONSISTENT: Use playFromView for all fragments
            songDetailViewModel.playFromView(List.of(result.getSong()), "Search Result", 0);
            showToast("Playing: " + result.getPrimaryText());
        }
    }

    @Override
    public void onArtistClick(SearchResult result) {
        if (result.getUser() != null) {
            // Navigate to user profile
            navigateToUserProfile(result.getUser());
        }
    }

    @Override
    public void onPlaylistClick(SearchResult result) {
        if (result.getPlaylist() != null) {
            // TODO: Navigate to playlist detail
            showToast("View playlist: " + result.getPrimaryText());
        }
    }

    @Override
    public void onActionClick(SearchResult result) {
        switch (result.getType()) {
            case SONG:
                // Play song using SongDetailViewModel and navigate to full player
                // Play song with context and navigate to full player
                if (result.getSong() != null && result.getUser() != null) {
                    // ✅ CONSISTENT: Use playFromView for all fragments
                    songDetailViewModel.playFromView(List.of(result.getSong()), "Search Result", 0);

                    // Navigate to full player using proper createIntent method
                    Intent intent = FullPlayerActivity.createIntent(getContext(), result.getSong().getId());
                    startActivity(intent);
                }
                break;
            case PLAYLIST:
                // Play playlist
                showToast("Play playlist: " + result.getPrimaryText());
                break;
        }
    }

    // ========== 🗑️ REMOVED: Complex NavigationContext method ==========
    // Replaced by direct song playback - search results don't need queue functionality for demo purposes

    @Override
    public void onFollowClick(SearchResult result, boolean isCurrentlyFollowing) {
        if (result.getUser() != null) {
            // Toggle follow status - optimistic UI update will handle immediate feedback
            viewModel.toggleFollowStatus(result.getUser());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh follow states when returning to search
        if (viewModel != null) {
            viewModel.refreshFollowingUsers();
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Navigate to user profile
     */
    private void navigateToUserProfile(User user) {
        try {
            // Create UserProfileFragment with user data
            UserProfileFragment userProfileFragment = UserProfileFragment.newInstance(user.getId());

            // Navigate to user profile
            getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, userProfileFragment)
                .addToBackStack("user_profile")
                .commit();

            showToast("Opening profile: " + user.getDisplayName());
        } catch (Exception e) {
            android.util.Log.e("SearchFragment", "Error navigating to user profile", e);
            showToast("Error opening profile");
        }
    }
}
