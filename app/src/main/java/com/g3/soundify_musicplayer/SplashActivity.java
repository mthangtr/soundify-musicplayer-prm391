package com.g3.soundify_musicplayer;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.g3.soundify_musicplayer.data.Activity.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 giây

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.logo);

        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);

            // Shared Element Transition: logo bay lên trên
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                    SplashActivity.this, logo, "logo_shared");

            startActivity(intent, options.toBundle());
            finish();
        }, SPLASH_DELAY);
    }
}
