package com.g3.soundify_musicplayer;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.g3.soundify_musicplayer.data.database.AppDatabase;
import com.g3.soundify_musicplayer.utils.RepositoryManager;

/**
 * Custom Application class for Soundify Music Player
 * Handles global initialization and configuration
 */
public class SoundifyApplication extends Application {
    
    private static final String TAG = "SoundifyApplication";
    
    // Notification channels
    public static final String MEDIA_PLAYBACK_CHANNEL_ID = "MediaPlaybackChannel";
    public static final String GENERAL_NOTIFICATIONS_CHANNEL_ID = "GeneralNotifications";

    // Singleton Repository Manager
    private RepositoryManager repositoryManager;
    
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize notification channels
        createNotificationChannels();

        // Initialize database (this will create the database if it doesn't exist)
        initializeDatabase();

        // Initialize Repository Manager singleton
        repositoryManager = RepositoryManager.getInstance(this);
    }
    
    /**
     * Create notification channels for Android 8.0+
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            
            // Media playback channel
            NotificationChannel mediaChannel = new NotificationChannel(
                MEDIA_PLAYBACK_CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            );
            mediaChannel.setDescription("Controls for music playback");
            mediaChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(mediaChannel);
            
            // General notifications channel
            NotificationChannel generalChannel = new NotificationChannel(
                GENERAL_NOTIFICATIONS_CHANNEL_ID,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            generalChannel.setDescription("General app notifications");
            notificationManager.createNotificationChannel(generalChannel);
        }
    }
    
    /**
     * Initialize the Room database
     */
    private void initializeDatabase() {
        try {
            // This will trigger database creation if it doesn't exist
            AppDatabase database = AppDatabase.getInstance(this);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing database", e);
        }
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();

        // Cleanup repositories
        if (repositoryManager != null) {
            repositoryManager.cleanup();
        }
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "Low memory warning received");
    }
    
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.w(TAG, "Memory trim requested, level: " + level);
    }

    /**
     * Get Repository Manager instance
     * Useful for debugging or manual access
     */
    public RepositoryManager getRepositoryManager() {
        if (repositoryManager == null) {
            repositoryManager = RepositoryManager.getInstance(this);
        }
        return repositoryManager;
    }
}
