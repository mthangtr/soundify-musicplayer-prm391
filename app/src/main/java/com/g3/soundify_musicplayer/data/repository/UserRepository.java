package com.g3.soundify_musicplayer.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.g3.soundify_musicplayer.data.database.AppDatabase;
import com.g3.soundify_musicplayer.data.dao.UserDao;
import com.g3.soundify_musicplayer.data.entity.User;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class UserRepository {
    
    private UserDao userDao;
    private ExecutorService executor;
    
    public UserRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        userDao = database.userDao();
        executor = Executors.newFixedThreadPool(4);
    }
    
    // Create
    public Future<Long> insert(User user) {
        return executor.submit(() -> userDao.insert(user));
    }
    
    // Read
    public LiveData<User> getUserById(long userId) {
        return userDao.getUserById(userId);
    }
    
    public Future<User> getUserByIdSync(long userId) {
        return executor.submit(() -> userDao.getUserByIdSync(userId));
    }
    
    public Future<User> getUserByUsername(String username) {
        return executor.submit(() -> userDao.getUserByUsername(username));
    }
    
    public Future<User> getUserByEmail(String email) {
        return executor.submit(() -> userDao.getUserByEmail(email));
    }
    
    public Future<User> authenticateUser(String username, String passwordHash) {
        return executor.submit(() -> userDao.authenticateUser(username, passwordHash));
    }
    
    public LiveData<List<User>> getAllUsers() {
        return userDao.getAllUsers();
    }
    
    public LiveData<List<User>> searchUsers(String query) {
        return userDao.searchUsers(query);
    }
    
    // Update
    public Future<Void> update(User user) {
        return executor.submit(() -> {
            userDao.update(user);
            return null;
        });
    }
    
    // Delete
    public Future<Void> delete(User user) {
        return executor.submit(() -> {
            userDao.delete(user);
            return null;
        });
    }
    
    // Validation
    public Future<Boolean> isUsernameExists(String username) {
        return executor.submit(() -> userDao.checkUsernameExists(username) > 0);
    }
    
    public Future<Boolean> isEmailExists(String email) {
        return executor.submit(() -> userDao.checkEmailExists(email) > 0);
    }
    
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
} 