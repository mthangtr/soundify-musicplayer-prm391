package com.g3.soundify_musicplayer.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.ui.login_register.LoginActivity;

public class SplashActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_splash);

            // Simple delay then navigate - no complex logic
            new Handler().postDelayed(() -> {
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

}
