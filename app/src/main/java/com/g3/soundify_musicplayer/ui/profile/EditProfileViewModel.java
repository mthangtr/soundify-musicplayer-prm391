package com.g3.soundify_musicplayer.ui.profile;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.repository.UserRepository;
import com.g3.soundify_musicplayer.utils.AuthManager;
import com.g3.soundify_musicplayer.utils.FileUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class EditProfileViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final AuthManager authManager;
    private final ExecutorService executor;

    // LiveData for UI state
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Validation error LiveData
    private final MutableLiveData<String> displayNameError = new MutableLiveData<>();
    private final MutableLiveData<String> usernameError = new MutableLiveData<>();
    private final MutableLiveData<String> emailError = new MutableLiveData<>();
    private final MutableLiveData<String> passwordError = new MutableLiveData<>();
    private final MutableLiveData<String> confirmPasswordError = new MutableLiveData<>();

    // Selected image URI
    private Uri selectedImageUri;

    public EditProfileViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
        authManager = new AuthManager(application);
        executor = Executors.newFixedThreadPool(4);
    }

    // Getters for LiveData
    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getDisplayNameError() { return displayNameError; }
    public LiveData<String> getUsernameError() { return usernameError; }
    public LiveData<String> getEmailError() { return emailError; }
    public LiveData<String> getPasswordError() { return passwordError; }
    public LiveData<String> getConfirmPasswordError() { return confirmPasswordError; }

    public void loadCurrentUser() {
        long currentUserId = authManager.getCurrentUserId();
        if (currentUserId != -1) {
            userRepository.getUserById(currentUserId).observeForever(user -> {
                currentUser.setValue(user);
            });
        }
    }

    public void setSelectedImageUri(Uri uri) {
        this.selectedImageUri = uri;
    }

    // Validation methods
    public void validateDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            displayNameError.setValue(getApplication().getString(R.string.error_display_name_required));
        } else if (displayName.length() > 100) {
            displayNameError.setValue(getApplication().getString(R.string.error_display_name_too_long));
        } else {
            displayNameError.setValue(null);
        }
    }

    public void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            usernameError.setValue(getApplication().getString(R.string.error_username_required));
        } else if (!AuthManager.isValidUsername(username)) {
            usernameError.setValue(getApplication().getString(R.string.error_username_invalid));
        } else {
            // Check if username is already taken (excluding current user)
            checkUsernameAvailability(username);
        }
    }

    public void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            emailError.setValue(getApplication().getString(R.string.error_email_required));
        } else if (!AuthManager.isValidEmail(email)) {
            emailError.setValue(getApplication().getString(R.string.error_email_invalid));
        } else {
            // Check if email is already taken (excluding current user)
            checkEmailAvailability(email);
        }
    }

    public void validateNewPassword(String password) {
        if (password != null && !password.isEmpty()) {
            if (!AuthManager.isValidPassword(password)) {
                passwordError.setValue(getApplication().getString(R.string.error_password_invalid));
            } else {
                passwordError.setValue(null);
            }
        } else {
            passwordError.setValue(null);
        }
    }

    public void validateConfirmPassword(String newPassword, String confirmPassword) {
        if (newPassword != null && !newPassword.isEmpty()) {
            if (confirmPassword == null || confirmPassword.isEmpty()) {
                confirmPasswordError.setValue(getApplication().getString(R.string.error_confirm_password_required));
            } else if (!newPassword.equals(confirmPassword)) {
                confirmPasswordError.setValue(getApplication().getString(R.string.error_passwords_not_match));
            } else {
                confirmPasswordError.setValue(null);
            }
        } else {
            confirmPasswordError.setValue(null);
        }
    }

    private void checkUsernameAvailability(String username) {
        executor.execute(() -> {
            try {
                User currentUserValue = currentUser.getValue();
                if (currentUserValue != null && username.equals(currentUserValue.getUsername())) {
                    // Same as current username, no need to check
                    usernameError.postValue(null);
                    return;
                }

                Future<User> future = userRepository.getUserByUsername(username);
                User existingUser = future.get();
                
                if (existingUser != null) {
                    usernameError.postValue(getApplication().getString(R.string.error_username_taken));
                } else {
                    usernameError.postValue(null);
                }
            } catch (Exception e) {
                // If error occurs, don't show username taken error
                usernameError.postValue(null);
            }
        });
    }

    private void checkEmailAvailability(String email) {
        executor.execute(() -> {
            try {
                User currentUserValue = currentUser.getValue();
                if (currentUserValue != null && email.equals(currentUserValue.getEmail())) {
                    // Same as current email, no need to check
                    emailError.postValue(null);
                    return;
                }

                Future<User> future = userRepository.getUserByEmail(email);
                User existingUser = future.get();
                
                if (existingUser != null) {
                    emailError.postValue(getApplication().getString(R.string.error_email_taken));
                } else {
                    emailError.postValue(null);
                }
            } catch (Exception e) {
                // If error occurs, don't show email taken error
                emailError.postValue(null);
            }
        });
    }

    public void saveProfile(String displayName, String username, String email, String bio,
                           String currentPassword, String newPassword, String confirmPassword,
                           Uri imageUri) {
        
        // Validate all fields first
        validateDisplayName(displayName);
        validateUsername(username);
        validateEmail(email);
        
        boolean hasPasswordChange = newPassword != null && !newPassword.isEmpty();
        if (hasPasswordChange) {
            validateNewPassword(newPassword);
            validateConfirmPassword(newPassword, confirmPassword);
        }

        // Check if there are any validation errors
        if (hasValidationErrors()) {
            errorMessage.setValue(getApplication().getString(R.string.error_fix_validation_errors));
            return;
        }

        isLoading.setValue(true);

        executor.execute(() -> {
            try {
                User user = currentUser.getValue();
                if (user == null) {
                    errorMessage.postValue(getApplication().getString(R.string.error_user_not_found));
                    isLoading.postValue(false);
                    return;
                }

                // Verify current password if changing password
                if (hasPasswordChange) {
                    if (currentPassword == null || currentPassword.isEmpty()) {
                        errorMessage.postValue(getApplication().getString(R.string.error_current_password_required));
                        isLoading.postValue(false);
                        return;
                    }

                    String hashedCurrentPassword = AuthManager.hashPassword(currentPassword);
                    if (!hashedCurrentPassword.equals(user.getPasswordHash())) {
                        errorMessage.postValue(getApplication().getString(R.string.error_current_password_incorrect));
                        isLoading.postValue(false);
                        return;
                    }
                }

                // Handle avatar image if selected
                if (imageUri != null) {
                    String avatarFileName = "avatar_" + user.getId() + "_" + System.currentTimeMillis() + ".jpg";
                    String avatarPath = FileUtils.copyFileToInternalStorage(
                        getApplication(), imageUri, avatarFileName);
                    
                    if (avatarPath != null) {
                        // Delete old avatar if exists
                        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                            FileUtils.deleteFile(user.getAvatarUrl());
                        }
                        user.setAvatarUrl(avatarPath);
                    }
                }

                // Update user data
                user.setDisplayName(displayName);
                user.setUsername(username);
                user.setEmail(email);
                user.setBio(bio);

                // Update password if changed
                if (hasPasswordChange) {
                    user.setPasswordHash(AuthManager.hashPassword(newPassword));
                }

                // Save to database
                Future<Void> future = userRepository.update(user);
                future.get();

                // Update auth manager session if username changed
                if (!username.equals(authManager.getCurrentUsername())) {
                    authManager.saveUserSession(user.getId(), username);
                }

                successMessage.postValue(getApplication().getString(R.string.profile_updated_successfully));
                
            } catch (Exception e) {
                errorMessage.postValue(getApplication().getString(R.string.error_updating_profile));
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    private boolean hasValidationErrors() {
        return (displayNameError.getValue() != null && !displayNameError.getValue().isEmpty()) ||
               (usernameError.getValue() != null && !usernameError.getValue().isEmpty()) ||
               (emailError.getValue() != null && !emailError.getValue().isEmpty()) ||
               (passwordError.getValue() != null && !passwordError.getValue().isEmpty()) ||
               (confirmPasswordError.getValue() != null && !confirmPasswordError.getValue().isEmpty());
    }

    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (executor != null) {
            executor.shutdown();
        }
    }
}
