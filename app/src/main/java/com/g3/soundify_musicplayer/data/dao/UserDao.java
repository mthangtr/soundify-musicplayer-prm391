package com.g3.soundify_musicplayer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.g3.soundify_musicplayer.data.entity.User;

import java.util.List;

@Dao
public interface UserDao {
    
    @Insert
    long insert(User user);
    
    @Update
    void update(User user);
    
    @Delete
    void delete(User user);
    
    @Query("SELECT * FROM users WHERE id = :userId")
    LiveData<User> getUserById(long userId);
    
    @Query("SELECT * FROM users WHERE id = :userId")
    User getUserByIdSync(long userId);
    
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User getUserByUsername(String username);
    
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);
    
    @Query("SELECT * FROM users WHERE username = :username AND password_hash = :passwordHash LIMIT 1")
    User authenticateUser(String username, String passwordHash);
    
    @Query("SELECT * FROM users ORDER BY created_at DESC")
    LiveData<List<User>> getAllUsers();
    
    @Query("SELECT * FROM users WHERE username LIKE '%' || :query || '%' OR display_name LIKE '%' || :query || '%' ORDER BY display_name")
    LiveData<List<User>> searchUsers(String query);
    
    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    int checkUsernameExists(String username);
    
    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    int checkEmailExists(String email);

    @Query("SELECT * FROM users ORDER BY created_at DESC")
    List<User> getAllUsersSync();
}