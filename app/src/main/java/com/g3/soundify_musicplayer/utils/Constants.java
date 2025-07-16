package com.g3.soundify_musicplayer.utils;

public class Constants {
    
    // Database
    public static final String DATABASE_NAME = "soundpify_database";
    public static final int DATABASE_VERSION = 1;
    
    // SharedPreferences
    public static final String PREFS_NAME = "soundpify_prefs";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_IS_LOGGED_IN = "is_logged_in";
    public static final String KEY_CURRENT_SONG_ID = "current_song_id";
    public static final String KEY_PLAYBACK_POSITION = "playback_position";
    
    // Validation Constants
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_PASSWORD_LENGTH = 100;
    public static final int MAX_DISPLAY_NAME_LENGTH = 100;
    public static final int MAX_EMAIL_LENGTH = 255;
    public static final int MAX_SONG_TITLE_LENGTH = 255;
    public static final int MAX_PLAYLIST_NAME_LENGTH = 255;
    public static final int MAX_COMMENT_LENGTH = 1000;
    public static final int MAX_BIO_LENGTH = 500;
    
    // File and Media Constants
    public static final String[] SUPPORTED_AUDIO_FORMATS = {".mp3", ".wav", ".m4a", ".aac"};
    public static final String[] SUPPORTED_IMAGE_FORMATS = {".jpg", ".jpeg", ".png", ".webp"};
    public static final int MAX_FILE_SIZE_MB = 50; // 50MB for audio files
    public static final int MAX_IMAGE_SIZE_MB = 5; // 5MB for images
    
    // UI Constants
    public static final int ITEMS_PER_PAGE = 20;
    public static final int SEARCH_DELAY_MS = 500;
    public static final int SPLASH_DELAY_MS = 2000;
    public static final int ANIMATION_DURATION_MS = 300;
    
    // Music Player Constants
    public static final int SEEK_FORWARD_TIME = 10000; // 10 seconds
    public static final int SEEK_BACKWARD_TIME = 10000; // 10 seconds
    public static final int PROGRESS_UPDATE_INTERVAL = 1000; // 1 second
    
    // Request Codes
    public static final int REQUEST_CODE_PICK_AUDIO = 1001;
    public static final int REQUEST_CODE_PICK_IMAGE = 1002;
    public static final int REQUEST_CODE_PERMISSIONS = 1003;
    
    // Intent Keys
    public static final String EXTRA_SONG_ID = "extra_song_id";
    public static final String EXTRA_PLAYLIST_ID = "extra_playlist_id";
    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_COMMENT_ID = "extra_comment_id";
    public static final String EXTRA_SEARCH_QUERY = "extra_search_query";
    
    // Notification Constants
    public static final String NOTIFICATION_CHANNEL_ID = "soundpify_player";
    public static final String NOTIFICATION_CHANNEL_NAME = "Music Player";
    public static final int NOTIFICATION_ID = 1;
    
    // Error Messages
    public static final String ERROR_NETWORK = "Network error occurred";
    public static final String ERROR_FILE_NOT_FOUND = "File not found";
    public static final String ERROR_PERMISSION_DENIED = "Permission denied";
    public static final String ERROR_INVALID_INPUT = "Invalid input";
    public static final String ERROR_USER_NOT_FOUND = "User not found";
    public static final String ERROR_SONG_NOT_FOUND = "Song not found";
    public static final String ERROR_PLAYLIST_NOT_FOUND = "Playlist not found";
    public static final String ERROR_AUTHENTICATION_FAILED = "Authentication failed";
    public static final String ERROR_USERNAME_EXISTS = "Username already exists";
    public static final String ERROR_EMAIL_EXISTS = "Email already exists";
    
    // Success Messages
    public static final String SUCCESS_REGISTRATION = "Registration successful";
    public static final String SUCCESS_LOGIN = "Login successful";
    public static final String SUCCESS_LOGOUT = "Logout successful";
    public static final String SUCCESS_SONG_UPLOADED = "Song uploaded successfully";
    public static final String SUCCESS_PLAYLIST_CREATED = "Playlist created successfully";
    public static final String SUCCESS_COMMENT_POSTED = "Comment posted successfully";
    public static final String SUCCESS_FOLLOW = "User followed successfully";
    public static final String SUCCESS_UNFOLLOW = "User unfollowed successfully";
    
    // Default Values
    public static final String DEFAULT_GENRE = "Unknown";
    public static final String DEFAULT_AVATAR_URL = "";
    public static final String DEFAULT_COVER_ART_URL = "";
    public static final boolean DEFAULT_IS_PUBLIC = true;
    public static final int DEFAULT_DURATION_MS = 0;
    
    // Genres
    public static final String[] MUSIC_GENRES = {
        "Pop", "Rock", "Hip Hop", "Jazz", "Classical", "Electronic", 
        "Country", "R&B", "Blues", "Folk", "Reggae", "Alternative", 
        "Indie", "Metal", "Punk", "Funk", "Soul", "Gospel", "Latin", "World"
    };
    
    // Private constructor to prevent instantiation
    private Constants() {}
} 