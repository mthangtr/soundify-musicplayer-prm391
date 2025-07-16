package com.g3.soundify_musicplayer.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "users",
    indices = {
        @Index(value = {"username"}, unique = true),
        @Index(value = {"email"}, unique = true)
    }
)
public class User {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "username")
    private String username;

    @ColumnInfo(name = "display_name")
    private String displayName;

    @ColumnInfo(name = "email")
    private String email;

    @ColumnInfo(name = "password_hash")
    private String passwordHash;

    @ColumnInfo(name = "avatar_url")
    private String avatarUrl;

    @ColumnInfo(name = "bio")
    private String bio;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    // Constructors
    public User() {}

    public User(String username, String displayName, String email, String passwordHash) {
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
} 