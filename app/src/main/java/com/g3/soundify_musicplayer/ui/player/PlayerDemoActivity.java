package com.g3.soundify_musicplayer.ui.player;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * PlayerDemoActivity - Compatibility wrapper for FullPlayerActivity
 * This class exists to maintain compatibility with existing references in manifest and other files
 * It simply redirects to FullPlayerActivity with the same intent data
 */
public class PlayerDemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Get intent data from this activity
        Intent currentIntent = getIntent();
        
        // Create new intent for FullPlayerActivity
        Intent fullPlayerIntent = new Intent(this, FullPlayerActivity.class);
        
        // Transfer all extras from current intent to new intent
        if (currentIntent.getExtras() != null) {
            fullPlayerIntent.putExtras(currentIntent.getExtras());
        }
        
        // Start FullPlayerActivity
        startActivity(fullPlayerIntent);
        
        // Finish this activity immediately
        finish();
    }
}
