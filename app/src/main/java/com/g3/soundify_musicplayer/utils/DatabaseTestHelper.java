package com.g3.soundify_musicplayer.utils;

import android.content.Context;

import com.g3.soundify_musicplayer.data.database.AppDatabase;
import com.g3.soundify_musicplayer.data.dao.UserDao;
import com.g3.soundify_musicplayer.data.entity.User;

import java.util.concurrent.Executors;

/**
 * Helper class to create test data for development and testing
 */
public class DatabaseTestHelper {
    
    /**
     * Create test users if they don't exist
     */
    public static void createTestUsersIfNeeded(Context context) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase database = AppDatabase.getInstance(context);
            UserDao userDao = database.userDao();
            
            // Check if admin user exists
            User adminUser = userDao.getUserByUsername("admin");
            if (adminUser == null) {
                // Create admin user: admin/123
                String adminPasswordHash = AuthManager.hashPassword("123");
                User newAdminUser = new User("admin", "Administrator", "admin@soundify.com", adminPasswordHash);
                userDao.insert(newAdminUser);
            }
            
            // Check if test user exists
            User testUser = userDao.getUserByUsername("user");
            if (testUser == null) {
                // Create test user: user/password
                String userPasswordHash = AuthManager.hashPassword("password");
                User newTestUser = new User("user", "Test User", "user@soundify.com", userPasswordHash);
                userDao.insert(newTestUser);
            }
        });
    }
    
    /**
     * Clear all data from database (for testing purposes)
     */
    public static void clearAllData(Context context) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase database = AppDatabase.getInstance(context);
            database.clearAllTables();
        });
    }
}
