package com.g3.soundify_musicplayer;

import android.os.Bundle;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.g3.soundify_musicplayer.data.Fragment.HomeFragment;
import com.g3.soundify_musicplayer.ui.base.BaseActivity;
import com.g3.soundify_musicplayer.ui.search.SearchFragment;

public class MainActivity extends BaseActivity {

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_main;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
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
                Toast.makeText(this, "Library clicked", Toast.LENGTH_SHORT).show();
                // TODO: Navigate to LibraryFragment
                return true;
            } else if (itemId == R.id.nav_upload) {
                Toast.makeText(this, "Upload clicked", Toast.LENGTH_SHORT).show();
                // TODO: Navigate to UploadFragment
                return true;
            }
            return false;
        });
        
        // Set Home as selected by default
        bottomNav.setSelectedItemId(R.id.nav_home);
    }
}