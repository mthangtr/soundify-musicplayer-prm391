package com.g3.soundify_musicplayer.data.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.viewmodel.AuthViewModel;
import com.g3.soundify_musicplayer.utils.DatabaseTestHelper;

public class LoginActivity extends AppCompatActivity {

    private EditText edtUsername, edtPassword;
    private Button btnLogin, btnRegister;
    private ProgressBar progressBar;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Create test users for development
        DatabaseTestHelper.createTestUsersIfNeeded(this);

        initViews();
        initViewModel();
        setupObservers();
        setupClickListeners();
    }

    private void initViews() {
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnGoToRegister);
        progressBar = findViewById(R.id.progressBar);
    }

    private void initViewModel() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }

    private void setupObservers() {
        // Observe loading state
        authViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                progressBar.setVisibility(View.VISIBLE);
                btnLogin.setEnabled(false);
                btnRegister.setEnabled(false);
            } else {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                btnRegister.setEnabled(true);
            }
        });

        // Observe error messages
        authViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (!TextUtils.isEmpty(errorMessage)) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                authViewModel.clearError();
            }
        });

        // Observe login success
        authViewModel.getLoginSuccess().observe(this, loginSuccess -> {
            if (loginSuccess) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void setupClickListeners() {
        // Handle login button click
        btnLogin.setOnClickListener(v -> {
            String username = edtUsername.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            authViewModel.login(username, password);
        });

        // Handle register button click
        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}
