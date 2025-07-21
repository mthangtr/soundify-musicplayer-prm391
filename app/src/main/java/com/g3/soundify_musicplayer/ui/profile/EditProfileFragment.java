package com.g3.soundify_musicplayer.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.utils.AuthManager;

/**
 * Fragment for editing user profile
 */
public class EditProfileFragment extends Fragment {
    
    // ViewModels and Managers
    private EditProfileViewModel viewModel;
    private AuthManager authManager;
    
    // UI Components - Profile Info
    private ImageView profileImageView;
    private Button changeImageButton;
    private TextInputLayout textInputDisplayName;
    private TextInputLayout textInputEmail;
    private TextInputLayout textInputBio;
    
    // UI Components - Password
    private TextInputLayout textInputCurrentPassword;
    private TextInputLayout textInputNewPassword;
    private TextInputLayout textInputConfirmPassword;
    
    // UI Components - Actions
    private Button saveButton;
    private Button cancelButton;
    
    // UI Components - Input Fields
    private TextInputEditText editTextDisplayName;
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextBio;
    private TextInputEditText editTextCurrentPassword;
    private TextInputEditText editTextNewPassword;
    private TextInputEditText editTextConfirmPassword;
    
    // Data
    private User currentUser;
    private Uri selectedImageUri;
    
    // Image picker launcher
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    
    public static EditProfileFragment newInstance() {
        return new EditProfileFragment();
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize AuthManager
        authManager = new AuthManager(requireContext());
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(EditProfileViewModel.class);
        
        // Setup image picker launcher
        setupImagePickerLauncher();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupTextWatchers();
        setupClickListeners();
        setupObservers();
        
        // Load current user data
        loadCurrentUser();
    }
    
    /**
     * Initialize all UI components
     */
    private void initViews(View view) {
        // Profile image
        profileImageView = view.findViewById(R.id.profile_image_view);
        changeImageButton = view.findViewById(R.id.change_image_button);
        
        // Text input layouts
        textInputDisplayName = view.findViewById(R.id.text_input_display_name);
        textInputEmail = view.findViewById(R.id.text_input_email);
        textInputBio = view.findViewById(R.id.text_input_bio);
        textInputCurrentPassword = view.findViewById(R.id.text_input_current_password);
        textInputNewPassword = view.findViewById(R.id.text_input_new_password);
        textInputConfirmPassword = view.findViewById(R.id.text_input_confirm_password);
        
        // Edit text fields
        editTextDisplayName = view.findViewById(R.id.edit_text_display_name);
        editTextEmail = view.findViewById(R.id.edit_text_email);
        editTextBio = view.findViewById(R.id.edit_text_bio);
        editTextCurrentPassword = view.findViewById(R.id.edit_text_current_password);
        editTextNewPassword = view.findViewById(R.id.edit_text_new_password);
        editTextConfirmPassword = view.findViewById(R.id.edit_text_confirm_password);
        
        // Action buttons
        saveButton = view.findViewById(R.id.save_button);
        cancelButton = view.findViewById(R.id.cancel_button);
    }
    
    /**
     * Setup text watchers for real-time validation
     */
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

        // TODO: Add password validation when validatePasswords method is available
    }
    
    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        changeImageButton.setOnClickListener(v -> openImagePicker());
        saveButton.setOnClickListener(v -> saveProfile());
        cancelButton.setOnClickListener(v -> cancelEdit());
    }
    
    /**
     * Setup ViewModel observers
     */
    private void setupObservers() {
        // Observe current user
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), this::populateUserData);
        
        // Observe validation errors
        viewModel.getDisplayNameError().observe(getViewLifecycleOwner(), error -> {
            textInputDisplayName.setError(error);
        });

        viewModel.getEmailError().observe(getViewLifecycleOwner(), error -> {
            textInputEmail.setError(error);
        });

        viewModel.getPasswordError().observe(getViewLifecycleOwner(), error -> {
            textInputNewPassword.setError(error);
            textInputConfirmPassword.setError(error);
        });
        
        // TODO: Add save result observer when getSaveResult method is available
        
        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            saveButton.setEnabled(!isLoading);
            saveButton.setText(isLoading ? "Saving..." : "Save Changes");
        });
    }
    
    /**
     * Setup image picker launcher
     */
    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        profileImageView.setImageURI(selectedImageUri);
                    }
                }
            }
        );
    }
    
    /**
     * Load current user data
     */
    private void loadCurrentUser() {
        viewModel.loadCurrentUser();
    }
    
    /**
     * Populate UI with user data
     */
    private void populateUserData(User user) {
        if (user == null) return;
        
        currentUser = user;
        editTextDisplayName.setText(user.getDisplayName());
        editTextEmail.setText(user.getEmail());
        editTextBio.setText(user.getBio());
        
        // Load profile image if available
        // TODO: Implement image loading with Glide/Picasso
    }
    
    /**
     * Open image picker
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Profile Image"));
    }
    
    /**
     * Save profile changes
     */
    private void saveProfile() {
        if (currentUser == null) return;
        
        // Get all input values
        String displayName = editTextDisplayName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String bio = editTextBio.getText().toString().trim();
        String currentPassword = editTextCurrentPassword.getText().toString();
        String newPassword = editTextNewPassword.getText().toString();
        String confirmPassword = editTextConfirmPassword.getText().toString();

        // Save profile through ViewModel (username stays the same)
        viewModel.saveProfile(displayName, currentUser.getUsername(), email, bio,
                            currentPassword, newPassword, confirmPassword, selectedImageUri);
    }
    
    /**
     * Cancel editing and go back
     */
    private void cancelEdit() {
        if (hasUnsavedChanges()) {
            // Show confirmation dialog
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Discard Changes?")
                .setMessage("You have unsaved changes. Are you sure you want to discard them?")
                .setPositiveButton("Discard", (dialog, which) -> navigateBack())
                .setNegativeButton("Cancel", null)
                .show();
        } else {
            navigateBack();
        }
    }
    
    /**
     * Navigate back to UserProfileFragment
     */
    private void navigateBack() {
        getParentFragmentManager().popBackStack();
    }
    
    /**
     * Check if there are unsaved changes
     */
    private boolean hasUnsavedChanges() {
        if (currentUser == null) return false;
        
        String currentDisplayName = editTextDisplayName.getText().toString().trim();
        String currentEmail = editTextEmail.getText().toString().trim();
        String currentBio = editTextBio.getText().toString().trim();
        String newPassword = editTextNewPassword.getText().toString();

        return !currentDisplayName.equals(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "") ||
               !currentEmail.equals(currentUser.getEmail() != null ? currentUser.getEmail() : "") ||
               !currentBio.equals(currentUser.getBio() != null ? currentUser.getBio() : "") ||
               !newPassword.isEmpty() ||
               selectedImageUri != null;
    }
    
    /**
     * Show toast message
     */
    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up any resources if needed
    }
}
