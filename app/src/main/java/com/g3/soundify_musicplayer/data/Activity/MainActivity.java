package com.g3.soundify_musicplayer.data.Activity;

import android.content.Intent;
import android.os.Bundle;
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
import com.g3.soundify_musicplayer.utils.AuthManager;

public class MainActivity extends BaseActivity {
    
    private AuthManager authManager;

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
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
                return true;
            } else if (itemId == R.id.nav_search) {
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SearchFragment())
                    .commit();
                return true;
            } else if (itemId == R.id.nav_library) {
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new LibraryFragment())
                    .commit();
                return true;
            } else if (itemId == R.id.nav_upload) {
                Toast.makeText(this, "Upload clicked", Toast.LENGTH_SHORT).show();
                // TODO: Navigate to UploadFragment
                return true;
            } else if (itemId == R.id.nav_logout) {
                logout();
                return true;
            }
            return false;
        });
        
        // Set Home as selected by default
        bottomNav.setSelectedItemId(R.id.nav_home);
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
