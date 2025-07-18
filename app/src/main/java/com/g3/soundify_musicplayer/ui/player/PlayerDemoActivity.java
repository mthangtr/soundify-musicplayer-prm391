package com.g3.soundify_musicplayer.ui.player;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.g3.soundify_musicplayer.R;

/**
 * Demo Activity to test the Full Player Screen UI
 * UI ONLY - No backend integration
 */
public class PlayerDemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_demo);
        
        if (savedInstanceState == null) {
            // Load the Full Player Fragment with mock data
            FullPlayerFragment fragment = FullPlayerFragment.newInstance(1L);
            
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
        }
    }
}
