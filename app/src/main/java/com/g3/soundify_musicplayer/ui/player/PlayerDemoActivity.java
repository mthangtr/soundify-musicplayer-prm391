package com.g3.soundify_musicplayer.ui.player;

import android.os.Bundle;

import androidx.fragment.app.FragmentTransaction;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.ui.base.BaseActivity;

/**
 * Demo Activity to test the Full Player Screen UI
 * UI ONLY - No backend integration
 */
public class PlayerDemoActivity extends BaseActivity {

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_player_demo;
    }

    @Override
    protected boolean shouldShowMiniPlayer() {
        // Hide mini player on full player screen to avoid duplication
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // Get song ID from intent or use default
            long songId = getIntent().getLongExtra("song_id", 1L);

            // Load the Full Player Fragment with song data
            FullPlayerFragment fragment = FullPlayerFragment.newInstance(songId);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
        }
    }
}
