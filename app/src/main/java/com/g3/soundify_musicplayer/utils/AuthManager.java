package com.g3.soundify_musicplayer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthManager {
    
    private static final String PREFS_NAME = "soundpify_prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    
    public AuthManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }
    
    // Session Management
    public void saveUserSession(long userId, String username) {
        editor.putLong(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }
    
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    public long getCurrentUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }
    
    public String getCurrentUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }
    
    public void logout() {
        editor.clear();
        editor.apply();
    }
    
    // Password Hashing
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    // Validation
    public static boolean isValidUsername(String username) {
        return username != null && 
               username.length() >= 3 && 
               username.length() <= 50 && 
               username.matches("^[a-zA-Z0-9_]+$");
    }
    
    public static boolean isValidEmail(String email) {
        return email != null && 
               email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }
    
    public static boolean isValidPassword(String password) {
        return password != null && 
               password.length() >= 6 && 
               password.length() <= 100;
    }
    
    public static boolean isValidDisplayName(String displayName) {
        return displayName != null && 
               displayName.trim().length() >= 1 && 
               displayName.length() <= 100;
    }
} 