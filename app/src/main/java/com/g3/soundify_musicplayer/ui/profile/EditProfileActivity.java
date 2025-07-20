package com.g3.soundify_musicplayer.ui.profile;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.User;

import java.io.File;

public class EditProfileActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private ShapeableImageView imageProfileAvatar;
    private MaterialCardView cardProfilePicture;
    private TextInputLayout textInputDisplayName;
    private TextInputLayout textInputUsername;
    private TextInputLayout textInputEmail;
    private TextInputLayout textInputBio;
    private TextInputLayout textInputCurrentPassword;
    private TextInputLayout textInputNewPassword;
    private TextInputLayout textInputConfirmPassword;
    private TextInputEditText editTextDisplayName;
    private TextInputEditText editTextUsername;
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextBio;
    private TextInputEditText editTextCurrentPassword;
    private TextInputEditText editTextNewPassword;
    private TextInputEditText editTextConfirmPassword;
    private MaterialButton buttonSave;
    private MaterialButton buttonCancel;
    private View loadingOverlay;

    // ViewModel
    private EditProfileViewModel viewModel;

    // Image selection launchers
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    // Current user data
    private User currentUser;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initializeViews();
        setupViewModel();
        setupToolbar();
        setupImageSelection();
        setupTextWatchers();
        setupClickListeners();
        setupObservers();

        // Load current user data
        viewModel.loadCurrentUser();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        imageProfileAvatar = findViewById(R.id.image_profile_avatar);
        cardProfilePicture = findViewById(R.id.card_profile_picture);
        textInputDisplayName = findViewById(R.id.text_input_display_name);
        textInputUsername = findViewById(R.id.text_input_username);
        textInputEmail = findViewById(R.id.text_input_email);
        textInputBio = findViewById(R.id.text_input_bio);
        textInputCurrentPassword = findViewById(R.id.text_input_current_password);
        textInputNewPassword = findViewById(R.id.text_input_new_password);
        textInputConfirmPassword = findViewById(R.id.text_input_confirm_password);
        editTextDisplayName = findViewById(R.id.edit_text_display_name);
        editTextUsername = findViewById(R.id.edit_text_username);
        editTextEmail = findViewById(R.id.edit_text_email);
        editTextBio = findViewById(R.id.edit_text_bio);
        editTextCurrentPassword = findViewById(R.id.edit_text_current_password);
        editTextNewPassword = findViewById(R.id.edit_text_new_password);
        editTextConfirmPassword = findViewById(R.id.edit_text_confirm_password);
        buttonSave = findViewById(R.id.button_save);
        buttonCancel = findViewById(R.id.button_cancel);
        loadingOverlay = findViewById(R.id.loading_overlay);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(EditProfileViewModel.class);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupImageSelection() {
        // Camera launcher
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        handleImageSelection(imageUri);
                    }
                }
            }
        );

        // Gallery launcher
        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        handleImageSelection(imageUri);
                    }
                }
            }
        );

        // Permission launcher
        permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    showImageSelectionDialog();
                } else {
                    Toast.makeText(this, R.string.permission_required_for_image, Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    private void setupTextWatchers() {
        // Real-time validation for display name
        editTextDisplayName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.validateDisplayName(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Real-time validation for username
        editTextUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.validateUsername(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Real-time validation for email
        editTextEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.validateEmail(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Password validation
        editTextNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.validateNewPassword(s.toString());
                // Also validate confirm password if it's not empty
                String confirmPassword = editTextConfirmPassword.getText().toString();
                if (!confirmPassword.isEmpty()) {
                    viewModel.validateConfirmPassword(s.toString(), confirmPassword);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        editTextConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String newPassword = editTextNewPassword.getText().toString();
                viewModel.validateConfirmPassword(newPassword, s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        cardProfilePicture.setOnClickListener(v -> checkPermissionAndSelectImage());
        
        buttonSave.setOnClickListener(v -> saveProfile());
        
        buttonCancel.setOnClickListener(v -> onBackPressed());
    }

    private void checkPermissionAndSelectImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            showImageSelectionDialog();
        }
    }

    private void showImageSelectionDialog() {
        String[] options = {getString(R.string.select_from_camera), getString(R.string.select_from_gallery)};
        
        new AlertDialog.Builder(this)
            .setTitle(R.string.select_profile_photo)
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    openCamera();
                } else {
                    openGallery();
                }
            })
            .show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(intent);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void handleImageSelection(Uri imageUri) {
        selectedImageUri = imageUri;
        
        // Display selected image
        Glide.with(this)
            .load(imageUri)
            .centerCrop()
            .into(imageProfileAvatar);
            
        // Notify ViewModel about image selection
        viewModel.setSelectedImageUri(imageUri);
    }

    private void saveProfile() {
        // Get all input values
        String displayName = editTextDisplayName.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String bio = editTextBio.getText().toString().trim();
        String currentPassword = editTextCurrentPassword.getText().toString();
        String newPassword = editTextNewPassword.getText().toString();
        String confirmPassword = editTextConfirmPassword.getText().toString();

        // Save profile through ViewModel
        viewModel.saveProfile(displayName, username, email, bio,
                            currentPassword, newPassword, confirmPassword, selectedImageUri);
    }

    private void setupObservers() {
        // Observe current user data
        viewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                currentUser = user;
                populateUserData(user);
            }
        });

        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            buttonSave.setEnabled(!isLoading);
            buttonCancel.setEnabled(!isLoading);
        });

        // Observe validation errors
        viewModel.getDisplayNameError().observe(this, error -> {
            textInputDisplayName.setError(error);
        });

        viewModel.getUsernameError().observe(this, error -> {
            textInputUsername.setError(error);
        });

        viewModel.getEmailError().observe(this, error -> {
            textInputEmail.setError(error);
        });

        viewModel.getPasswordError().observe(this, error -> {
            textInputNewPassword.setError(error);
        });

        viewModel.getConfirmPasswordError().observe(this, error -> {
            textInputConfirmPassword.setError(error);
        });

        // Observe success message
        viewModel.getSuccessMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });

        // Observe error message
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                viewModel.clearErrorMessage();
            }
        });
    }

    private void populateUserData(User user) {
        editTextDisplayName.setText(user.getDisplayName());
        editTextUsername.setText(user.getUsername());
        editTextEmail.setText(user.getEmail());
        editTextBio.setText(user.getBio());

        // Load avatar if available
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            File avatarFile = new File(user.getAvatarUrl());
            if (avatarFile.exists()) {
                Glide.with(this)
                    .load(avatarFile)
                    .centerCrop()
                    .into(imageProfileAvatar);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges()) {
            showUnsavedChangesDialog();
        } else {
            super.onBackPressed();
        }
    }

    private boolean hasUnsavedChanges() {
        if (currentUser == null) return false;

        String currentDisplayName = editTextDisplayName.getText().toString().trim();
        String currentUsername = editTextUsername.getText().toString().trim();
        String currentEmail = editTextEmail.getText().toString().trim();
        String currentBio = editTextBio.getText().toString().trim();
        String newPassword = editTextNewPassword.getText().toString();

        return !currentDisplayName.equals(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "") ||
               !currentUsername.equals(currentUser.getUsername() != null ? currentUser.getUsername() : "") ||
               !currentEmail.equals(currentUser.getEmail() != null ? currentUser.getEmail() : "") ||
               !currentBio.equals(currentUser.getBio() != null ? currentUser.getBio() : "") ||
               !newPassword.isEmpty() ||
               selectedImageUri != null;
    }

    private void showUnsavedChangesDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.unsaved_changes_title)
            .setMessage(R.string.unsaved_changes_message)
            .setPositiveButton(R.string.discard_changes, (dialog, which) -> {
                super.onBackPressed();
            })
            .setNegativeButton(R.string.keep_editing, null)
            .show();
    }
}
