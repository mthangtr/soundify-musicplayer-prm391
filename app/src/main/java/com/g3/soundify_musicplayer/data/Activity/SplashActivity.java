package com.g3.soundify_musicplayer.data.Activity;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.utils.AuthManager;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 giây
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("SplashActivity", "onCreate started");

        try {
            setContentView(R.layout.activity_splash);
            android.util.Log.d("SplashActivity", "Layout set successfully");

            // Simple delay then navigate - no complex logic
            new Handler().postDelayed(() -> {
                android.util.Log.d("SplashActivity", "Navigating to LoginActivity");
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }, 2000); // 2 seconds

        } catch (Exception e) {
            android.util.Log.e("SplashActivity", "onCreate error", e);
            // Immediate fallback navigation
            Intent fallbackIntent = new Intent(this, LoginActivity.class);
            startActivity(fallbackIntent);
            finish();
        }
    }
    
    private void checkLoginStatusAndNavigate(ImageView logo) {
        try {
            Intent intent;

            // For testing: Always go to LoginActivity
            intent = new Intent(SplashActivity.this, LoginActivity.class);

            // Simple navigation without transition for now
            startActivity(intent);
            finish();

        } catch (Exception e) {
            // Log error and fallback
            android.util.Log.e("SplashActivity", "Navigation error", e);

            // Fallback: Simple intent without transition
            Intent fallbackIntent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(fallbackIntent);
            finish();
        }
    }
}
