package com.g3.soundify_musicplayer.data.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.Fragment.HomeFragment;
import com.g3.soundify_musicplayer.ui.base.BaseActivity;
import com.g3.soundify_musicplayer.ui.search.SearchFragment;
import com.g3.soundify_musicplayer.ui.library.LibraryFragment;
import com.g3.soundify_musicplayer.ui.upload.UploadSongFragment;
import com.g3.soundify_musicplayer.ui.profile.UserProfileFragment;
import com.g3.soundify_musicplayer.utils.AuthManager;
import androidx.appcompat.widget.PopupMenu;

public class MainActivity extends BaseActivity {

    private AuthManager authManager;
    private int currentSelectedTab = R.id.nav_home; // Track current tab

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_main;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        
        // Initialize AuthManager
        authManager = new AuthManager(this);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        // Setup Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                currentSelectedTab = R.id.nav_home;
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
                return true;
            } else if (itemId == R.id.nav_search) {
                currentSelectedTab = R.id.nav_search;
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SearchFragment())
                    .commit();
                return true;
            } else if (itemId == R.id.nav_library) {
                currentSelectedTab = R.id.nav_library;
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new LibraryFragment())
                    .commit();
                return true;
            } else if (itemId == R.id.nav_profile) {
                currentSelectedTab = R.id.nav_profile;
                // Navigate to current user's profile
                long currentUserId = authManager.getCurrentUserId();
                if (currentUserId != -1) {
                    UserProfileFragment profileFragment = UserProfileFragment.newInstance(currentUserId);
                    getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, profileFragment)
                        .commit();
                } else {
                    Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (itemId == R.id.nav_upload) {
                currentSelectedTab = R.id.nav_upload;
                // Navigate to UploadSongFragment
                navigateToUploadSong();
                return true;
            }
            return false;
        });
        
        // Set Home as selected by default
        bottomNav.setSelectedItemId(R.id.nav_home);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check if we're returning from UserProfile with a specific tab to restore
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("restore_tab")) {
            int tabToRestore = intent.getIntExtra("restore_tab", R.id.nav_home);
            restoreTab(tabToRestore);
            // Clear the extra to prevent repeated restoration
            intent.removeExtra("restore_tab");
            return; // Skip the default navigation reset
        }

        // Reset bottom navigation selection when returning from other activities
        // This ensures the upload button doesn't stay selected
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            // Check current fragment and set appropriate navigation item
            androidx.fragment.app.Fragment currentFragment = getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);

            if (currentFragment instanceof HomeFragment) {
                bottomNav.setSelectedItemId(R.id.nav_home);
                currentSelectedTab = R.id.nav_home;
            } else if (currentFragment instanceof SearchFragment) {
                bottomNav.setSelectedItemId(R.id.nav_search);
                currentSelectedTab = R.id.nav_search;
            } else if (currentFragment instanceof LibraryFragment) {
                bottomNav.setSelectedItemId(R.id.nav_library);
                currentSelectedTab = R.id.nav_library;
            } else if (currentFragment instanceof UserProfileFragment) {
                bottomNav.setSelectedItemId(R.id.nav_profile);
                currentSelectedTab = R.id.nav_profile;
            } else if (currentFragment instanceof UploadSongFragment) {
                // Don't change navigation for upload fragment
                // Keep the previous selection
                currentSelectedTab = R.id.nav_upload;
            } else {
                // Default to home if unknown fragment
                bottomNav.setSelectedItemId(R.id.nav_home);
                currentSelectedTab = R.id.nav_home;
            }
        }
    }

    /**
     * Navigate to UploadSongFragment for new song upload
     */
    private void navigateToUploadSong() {
        UploadSongFragment uploadFragment = UploadSongFragment.newInstanceForUpload();

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,  // enter
                        R.anim.slide_out_left,  // exit
                        R.anim.slide_in_left,   // popEnter
                        R.anim.slide_out_right  // popExit
                )
                .replace(R.id.fragment_container, uploadFragment)
                .addToBackStack("upload_song")
                .commit();

        android.util.Log.d("MainActivity", "Navigating to UploadSongFragment");
    }





    /**
     * Restore specific tab when returning from UserProfile
     */
    private void restoreTab(int tabId) {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(tabId);
            currentSelectedTab = tabId;
        }
    }

    /**
     * Navigate to UploadSongFragment for editing an existing song
     */
    public void navigateToEditSong(long songId) {
        UploadSongFragment uploadFragment = UploadSongFragment.newInstanceForEdit(songId);

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right,  // enter
                        R.anim.slide_out_left,  // exit
                        R.anim.slide_in_left,   // popEnter
                        R.anim.slide_out_right  // popExit
                )
                .replace(R.id.fragment_container, uploadFragment)
                .addToBackStack("edit_song")
                .commit();

        android.util.Log.d("MainActivity", "Navigating to UploadSongFragment for edit, songId: " + songId);
    }

    @Override
    public void onBackPressed() {
        // Check if there are fragments in the back stack
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            // Pop the fragment from back stack
            getSupportFragmentManager().popBackStack();
        } else {
            // No fragments in back stack, handle normal back press
            super.onBackPressed();
        }
    }

    private void logout() {
        // Clear user session
        authManager.logout();
        
        // Navigate back to LoginActivity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
    }
}
