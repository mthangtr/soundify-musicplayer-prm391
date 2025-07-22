package com.g3.soundify_musicplayer.viewmodel;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.repository.UserRepository;
import com.g3.soundify_musicplayer.utils.AuthManager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * ViewModel for Authentication (Login/Register)
 * Handles login, register, and session management
 */
public class AuthViewModel extends AndroidViewModel {
    
    private UserRepository userRepository;
    private AuthManager authManager;
    
    // LiveData for UI states
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private MutableLiveData<Boolean> registerSuccess = new MutableLiveData<>();
    private MutableLiveData<User> currentUser = new MutableLiveData<>();
    
    public AuthViewModel(@NonNull Application application) {
        super(application);
        this.userRepository = new UserRepository(application);
        this.authManager = new AuthManager(application);
        
        // Check if user is already logged in
        checkCurrentSession();
    }
    
    // Getters for LiveData
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }
    
    public LiveData<Boolean> getRegisterSuccess() {
        return registerSuccess;
    }
    
    public LiveData<User> getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Login user with username and password
     */
    public void login(String username, String password) {
        // Reset previous states
        errorMessage.setValue(null);
        loginSuccess.setValue(false);

        // Validate input
        if (TextUtils.isEmpty(username)) {
            errorMessage.setValue("Vui lòng nhập tên đăng nhập");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            errorMessage.setValue("Vui lòng nhập mật khẩu");
            return;
        }

        // Show loading
        isLoading.setValue(true);

        // Hash password and authenticate
        String passwordHash = AuthManager.hashPassword(password);

        // Perform authentication in background thread
        Future<User> authFuture = userRepository.authenticateUser(username, passwordHash);

        // Handle result in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                // Debug: Check if user exists first
                Future<User> userCheckFuture = userRepository.getUserByUsername(username);
                User existingUser = userCheckFuture.get();
                User user = authFuture.get();

                // Post results to main thread
                if (user != null) {
                    // Login successful
                    authManager.saveUserSession(user.getId(), user.getUsername());
                    currentUser.postValue(user);
                    loginSuccess.postValue(true);
                } else {
                    // Login failed
                    errorMessage.postValue("Tên đăng nhập hoặc mật khẩu không đúng");
                }
            } catch (ExecutionException | InterruptedException e) {
                android.util.Log.e("AuthViewModel", "Login error", e);
                errorMessage.postValue("Lỗi kết nối. Vui lòng thử lại");
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }
    
    /**
     * Register new user
     */
    public void register(String username, String displayName, String email, String password, String confirmPassword) {
        // Reset previous states
        errorMessage.setValue(null);
        registerSuccess.setValue(false);
        
        // Validate input
        String validationError = validateRegisterInput(username, displayName, email, password, confirmPassword);
        if (validationError != null) {
            errorMessage.setValue(validationError);
            return;
        }
        
        // Show loading
        isLoading.setValue(true);
        
        // Check if username or email already exists
        new Thread(() -> {
            try {
                Future<Boolean> usernameExistsFuture = userRepository.isUsernameExists(username);
                Future<Boolean> emailExistsFuture = userRepository.isEmailExists(email);
                
                boolean usernameExists = usernameExistsFuture.get();
                boolean emailExists = emailExistsFuture.get();
                
                if (usernameExists) {
                    errorMessage.postValue("Tên đăng nhập đã tồn tại");
                    isLoading.postValue(false);
                    return;
                }
                
                if (emailExists) {
                    errorMessage.postValue("Email đã được sử dụng");
                    isLoading.postValue(false);
                    return;
                }
                
                // Create new user
                String passwordHash = AuthManager.hashPassword(password);
                User newUser = new User(username, displayName, email, passwordHash);

                Future<Long> insertFuture = userRepository.insert(newUser);
                long userId = insertFuture.get();
                
                if (userId > 0) {
                    // Registration successful
                    newUser.setId(userId);
                    registerSuccess.postValue(true);
                } else {
                    errorMessage.postValue("Đăng ký thất bại. Vui lòng thử lại");
                }
                
            } catch (ExecutionException | InterruptedException e) {
                errorMessage.postValue("Lỗi kết nối. Vui lòng thử lại");
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }
    
    /**
     * Check if user is already logged in
     */
    private void checkCurrentSession() {
        if (authManager.isLoggedIn()) {
            long userId = authManager.getCurrentUserId();
            if (userId != -1) {
                // Get user info from database
                Future<User> userFuture = userRepository.getUserByIdSync(userId);
                new Thread(() -> {
                    try {
                        User user = userFuture.get();
                        if (user != null) {
                            currentUser.postValue(user);
                            loginSuccess.postValue(true);
                        } else {
                            // User not found in database, clear session
                            authManager.logout();
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        // Error getting user, clear session
                        authManager.logout();
                    }
                }).start();
            }
        }
    }
    
    /**
     * Logout current user
     */
    public void logout() {
        authManager.logout();
        currentUser.setValue(null);
        loginSuccess.setValue(false);
    }
    
    /**
     * Validate register input
     */
    private String validateRegisterInput(String username, String displayName, String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(username)) {
            return "Vui lòng nhập tên đăng nhập";
        }
        
        if (TextUtils.isEmpty(displayName)) {
            return "Vui lòng nhập tên hiển thị";
        }
        
        if (TextUtils.isEmpty(email)) {
            return "Vui lòng nhập email";
        }
        
        if (TextUtils.isEmpty(password)) {
            return "Vui lòng nhập mật khẩu";
        }
        
        if (TextUtils.isEmpty(confirmPassword)) {
            return "Vui lòng xác nhận mật khẩu";
        }
        
        if (!AuthManager.isValidUsername(username)) {
            return "Tên đăng nhập phải từ 3-50 ký tự, chỉ chứa chữ, số và dấu gạch dưới";
        }
        
        if (!AuthManager.isValidDisplayName(displayName)) {
            return "Tên hiển thị không hợp lệ";
        }
        
        if (!AuthManager.isValidEmail(email)) {
            return "Email không hợp lệ";
        }
        
        if (!AuthManager.isValidPassword(password)) {
            return "Mật khẩu phải từ 6-100 ký tự";
        }
        
        if (!password.equals(confirmPassword)) {
            return "Mật khẩu xác nhận không khớp";
        }
        
        return null; // No validation errors
    }
    
    /**
     * Clear error message
     */
    public void clearError() {
        errorMessage.setValue(null);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        if (userRepository != null) {
            userRepository.shutdown();
        }
    }
}
