package com.g3.soundify_musicplayer.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.utils.AuthManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class FollowersFollowingActivity extends AppCompatActivity implements UserFollowAdapter.OnUserActionListener {

    // Constants
    private static final String EXTRA_USER_ID = "user_id";
    private static final String EXTRA_USERNAME = "username";
    private static final String EXTRA_INITIAL_TAB = "initial_tab";
    public static final int TAB_FOLLOWERS = 0;
    public static final int TAB_FOLLOWING = 1;

    // UI Components
    private MaterialToolbar toolbar;
    private TextInputEditText editTextSearch;
    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerViewUsers;
    private LinearLayout emptyStateLayout;
    private LinearLayout loadingLayout;
    private LinearLayout errorStateLayout;
    private TextView emptyStateTitle;
    private TextView emptyStateSubtitle;
    private MaterialButton buttonRetry;

    // ViewModel and Adapter
    private FollowersFollowingViewModel viewModel;
    private UserFollowAdapter adapter;
    private AuthManager authManager;

    // State
    private long targetUserId;
    private String targetUsername;
    private int currentTab = TAB_FOLLOWERS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use simple layout if SwipeRefreshLayout causes issues
        // setContentView(R.layout.activity_followers_following_simple);
        setContentView(R.layout.activity_followers_following);

        authManager = new AuthManager(this);
        
        initializeViews();
        setupViewModel();
        setupToolbar();
        setupRecyclerView();
        setupTabs();
        setupSearch();
        setupSwipeRefresh();
        setupClickListeners();
        setupObservers();

        handleIntent();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        editTextSearch = findViewById(R.id.edit_text_search);
        tabLayout = findViewById(R.id.tab_layout);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        recyclerViewUsers = findViewById(R.id.recycler_view_users);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        loadingLayout = findViewById(R.id.loading_layout);
        errorStateLayout = findViewById(R.id.error_state_layout);
        emptyStateTitle = findViewById(R.id.empty_state_title);
        emptyStateSubtitle = findViewById(R.id.empty_state_subtitle);
        buttonRetry = findViewById(R.id.button_retry);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(FollowersFollowingViewModel.class);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        long currentUserId = authManager.getCurrentUserId();
        adapter = new UserFollowAdapter(this, currentUserId);
        adapter.setOnUserActionListener(this);
        
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewUsers.setAdapter(adapter);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.followers));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.following));
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                viewModel.setCurrentTab(currentTab);
                updateCurrentTabData();
                clearSearch();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSearch() {
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filterUsers(s.toString());
                updateEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                viewModel.refreshData();
                clearSearch();
            });
        }
    }

    private void setupClickListeners() {
        buttonRetry.setOnClickListener(v -> {
            viewModel.refreshData();
        });
    }

    private void setupObservers() {
        // Observe target user
        viewModel.getTargetUser().observe(this, user -> {
            if (user != null) {
                updateToolbarTitle(user);
            }
        });

        // Observe followers
        viewModel.getFollowers().observe(this, followers -> {
            if (currentTab == TAB_FOLLOWERS) {
                updateUsersList(followers);
                updateTabTitle(TAB_FOLLOWERS, followers != null ? followers.size() : 0);
            }
        });

        // Observe following
        viewModel.getFollowing().observe(this, following -> {
            if (currentTab == TAB_FOLLOWING) {
                updateUsersList(following);
                updateTabTitle(TAB_FOLLOWING, following != null ? following.size() : 0);
            }
        });

        // Observe current user's following for follow button states
        viewModel.getCurrentUserFollowing().observe(this, currentUserFollowing -> {
            List<Long> followingIds = new ArrayList<>();
            if (currentUserFollowing != null) {
                for (User user : currentUserFollowing) {
                    followingIds.add(user.getId());
                }
            }
            adapter.setFollowingIds(followingIds);
        });

        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(isLoading);
            }
            if (isLoading) {
                showLoadingState();
            } else {
                hideLoadingState();
            }
        });

        // Observe error messages
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                showErrorState();
                viewModel.clearErrorMessage();
            }
        });

        // Observe success messages
        viewModel.getSuccessMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                viewModel.clearSuccessMessage();
            }
        });
    }

    private void handleIntent() {
        Intent intent = getIntent();
        targetUserId = intent.getLongExtra(EXTRA_USER_ID, -1);
        targetUsername = intent.getStringExtra(EXTRA_USERNAME);
        int initialTab = intent.getIntExtra(EXTRA_INITIAL_TAB, TAB_FOLLOWERS);

        if (targetUserId == -1) {
            Toast.makeText(this, R.string.error_invalid_user, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set initial tab
        currentTab = initialTab;
        TabLayout.Tab tab = tabLayout.getTabAt(initialTab);
        if (tab != null) {
            tab.select();
        }

        // Load data
        viewModel.setTargetUserId(targetUserId);
        viewModel.setCurrentTab(currentTab);
    }

    private void updateToolbarTitle(User user) {
        if (getSupportActionBar() != null) {
            String title = user.getDisplayName() != null ? 
                user.getDisplayName() : "@" + user.getUsername();
            getSupportActionBar().setTitle(title);
        }
    }

    private void updateTabTitle(int tabPosition, int count) {
        TabLayout.Tab tab = tabLayout.getTabAt(tabPosition);
        if (tab != null) {
            String title = viewModel.getTabTitle(tabPosition, count);
            tab.setText(title);
        }
    }

    private void updateCurrentTabData() {
        if (currentTab == TAB_FOLLOWERS) {
            List<User> followers = viewModel.getFollowers().getValue();
            updateUsersList(followers);
        } else {
            List<User> following = viewModel.getFollowing().getValue();
            updateUsersList(following);
        }
    }

    private void updateUsersList(List<User> users) {
        adapter.setUsers(users);
        updateEmptyState();
        hideLoadingState();
        hideErrorState();
    }

    private void updateEmptyState() {
        boolean isEmpty = adapter.getItemCount() == 0;
        boolean isSearching = !editTextSearch.getText().toString().trim().isEmpty();

        if (isEmpty && !isSearching) {
            emptyStateTitle.setText(viewModel.getEmptyStateTitle(currentTab));
            emptyStateSubtitle.setText(viewModel.getEmptyStateSubtitle(currentTab, viewModel.isOwnProfile()));
            showEmptyState();
        } else if (isEmpty && isSearching) {
            emptyStateTitle.setText(R.string.no_search_results_title);
            emptyStateSubtitle.setText(getString(R.string.no_search_results_subtitle, editTextSearch.getText().toString()));
            showEmptyState();
        } else {
            hideEmptyState();
        }
    }

    private void showEmptyState() {
        emptyStateLayout.setVisibility(View.VISIBLE);
        recyclerViewUsers.setVisibility(View.GONE);
        loadingLayout.setVisibility(View.GONE);
        errorStateLayout.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyStateLayout.setVisibility(View.GONE);
        recyclerViewUsers.setVisibility(View.VISIBLE);
    }

    private void showLoadingState() {
        if (adapter.getItemCount() == 0) {
            loadingLayout.setVisibility(View.VISIBLE);
            recyclerViewUsers.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
            errorStateLayout.setVisibility(View.GONE);
        }
    }

    private void hideLoadingState() {
        loadingLayout.setVisibility(View.GONE);
        recyclerViewUsers.setVisibility(View.VISIBLE);
    }

    private void showErrorState() {
        if (adapter.getItemCount() == 0) {
            errorStateLayout.setVisibility(View.VISIBLE);
            recyclerViewUsers.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
            loadingLayout.setVisibility(View.GONE);
        }
    }

    private void hideErrorState() {
        errorStateLayout.setVisibility(View.GONE);
        recyclerViewUsers.setVisibility(View.VISIBLE);
    }

    private void clearSearch() {
        editTextSearch.setText("");
        adapter.filterUsers("");
    }

    // UserFollowAdapter.OnUserActionListener implementation
    @Override
    public void onUserClick(User user, int position) {
        // Navigate to user profile
        Intent intent = UserProfileActivity.createIntent(this, user.getId(), user.getUsername());
        startActivity(intent);
    }

    @Override
    public void onFollowClick(User user, int position, boolean isCurrentlyFollowing) {
        viewModel.toggleFollowStatus(user);

        // Update adapter immediately for better UX
        adapter.updateFollowStatus(user.getId(), !isCurrentlyFollowing);
    }

    // Static methods to create intents
    public static Intent createIntent(Context context, long userId) {
        Intent intent = new Intent(context, FollowersFollowingActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        return intent;
    }

    public static Intent createIntent(Context context, long userId, String username) {
        Intent intent = new Intent(context, FollowersFollowingActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_USERNAME, username);
        return intent;
    }

    public static Intent createIntent(Context context, long userId, String username, int initialTab) {
        Intent intent = new Intent(context, FollowersFollowingActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_USERNAME, username);
        intent.putExtra(EXTRA_INITIAL_TAB, initialTab);
        return intent;
    }
}
