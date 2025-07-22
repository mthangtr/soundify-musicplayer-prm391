package com.g3.soundify_musicplayer.ui.login_register;

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

public class RegisterActivity extends AppCompatActivity {

    private EditText edtUsername, edtDisplayName, edtPassword, edtConfirmPassword;
    private Button btnRegister, btnBackToLogin;
    private ProgressBar progressBar;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Create test users for development
        DatabaseTestHelper.createTestUsersIfNeeded(this);

        initViews();
        initViewModel();
        setupObservers();
        setupClickListeners();
    }

    private void initViews() {
        edtUsername = findViewById(R.id.edtUsername);
        edtDisplayName = findViewById(R.id.edtDisplayName);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);
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
                btnRegister.setEnabled(false);
                btnBackToLogin.setEnabled(false);
            } else {
                progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);
                btnBackToLogin.setEnabled(true);
            }
        });

        // Observe error messages
        authViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (!TextUtils.isEmpty(errorMessage)) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                authViewModel.clearError();
            }
        });

        // Observe register success
        authViewModel.getRegisterSuccess().observe(this, registerSuccess -> {
            if (registerSuccess) {
                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                // Navigate to LoginActivity
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void setupClickListeners() {
        // Handle register button click
        btnRegister.setOnClickListener(v -> {
            String username = edtUsername.getText().toString().trim();
            String displayName = edtDisplayName.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            String confirmPassword = edtConfirmPassword.getText().toString().trim();

            // Use a default email for now (will be updated in user profile later)
            String defaultEmail = username + "@soundify.local";

            authViewModel.register(username, displayName, defaultEmail, password, confirmPassword);
        });

        // Handle back to login button click
        btnBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
