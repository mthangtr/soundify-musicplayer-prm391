package com.g3.soundify_musicplayer.ui.player;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.fragment.app.FragmentTransaction;

import com.g3.soundify_musicplayer.MainActivity;
import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
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

        // Setup test button for mini player
        setupTestButton();
    }

    private void setupTestButton() {
        Button btnTestMiniPlayer = findViewById(R.id.btn_test_mini_player);
        btnTestMiniPlayer.setOnClickListener(v -> {
            // Create mock song and artist
            Song mockSong = createMockSong();
            User mockArtist = createMockArtist();

            // Show mini player
            MiniPlayerManager.getInstance().showMiniPlayer(mockSong, mockArtist);

            // Navigate to MainActivity to see mini player
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }

    private Song createMockSong() {
        Song song = new Song();
        song.setId(1L);
        song.setTitle("Beautiful Sunset");
        song.setDescription("A relaxing instrumental track");
        song.setUploaderId(1L);
        song.setGenre("Ambient");
        song.setDurationMs(225000); // 3:45
        song.setPublic(true);
        song.setCreatedAt(System.currentTimeMillis());
        return song;
    }

    private User createMockArtist() {
        User artist = new User();
        artist.setId(1L);
        artist.setUsername("demo_artist");
        artist.setDisplayName("Demo Artist");
        artist.setAvatarUrl("mock://avatar/demo_artist.jpg");
        artist.setCreatedAt(System.currentTimeMillis());
        return artist;
    }
}
