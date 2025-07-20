package com.g3.soundify_musicplayer.utils;

import android.content.Context;

import com.g3.soundify_musicplayer.data.database.AppDatabase;
import com.g3.soundify_musicplayer.data.dao.UserDao;
import com.g3.soundify_musicplayer.data.dao.SongDao;
import com.g3.soundify_musicplayer.data.dao.PlaylistDao;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.Playlist;

import java.util.concurrent.Executors;

/**
 * Helper class to create test data for development and testing
 */
public class DatabaseTestHelper {
    
    /**
     * Create test users and songs if they don't exist
     */
    public static void createTestUsersIfNeeded(Context context) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase database = AppDatabase.getInstance(context);
            UserDao userDao = database.userDao();
            SongDao songDao = database.songDao();
            PlaylistDao playlistDao = database.playlistDao();

            // Check if admin user exists
            User adminUser = userDao.getUserByUsername("admin");
            if (adminUser == null) {
                // Create admin user: admin/123
                String adminPasswordHash = AuthManager.hashPassword("123");
                User newAdminUser = new User("admin", "Administrator", "admin@soundify.com", adminPasswordHash);
                long adminId = userDao.insert(newAdminUser);

                // Create demo songs and playlists for admin user
                createDemoSongs(songDao, adminId);
                createDemoPlaylists(playlistDao, adminId);
            }

            // Check if test user exists
            User testUser = userDao.getUserByUsername("user");
            if (testUser == null) {
                // Create test user: user/password
                String userPasswordHash = AuthManager.hashPassword("password");
                User newTestUser = new User("user", "Test User", "user@soundify.com", userPasswordHash);
                long userId = userDao.insert(newTestUser);

                // Create demo songs and playlists for test user
                createDemoSongs(songDao, userId);
                createDemoPlaylists(playlistDao, userId);
            }
        });
    }

    /**
     * Create demo songs for testing
     */
    private static void createDemoSongs(SongDao songDao, long uploaderId) {
        // Check if songs already exist
        if (songDao.getAllSongsSync().size() > 0) {
            return; // Songs already exist
        }

        // Create demo songs
        Song[] demoSongs = {
            new Song(uploaderId, "Lofi Chill", "file:///android_asset/lofi.mp3"),
            new Song(uploaderId, "Future Bass", "file:///android_asset/future.mp3"),
            new Song(uploaderId, "Guitar Solo", "file:///android_asset/guitar.mp3"),
            new Song(uploaderId, "Jazz Night", "file:///android_asset/jazz.mp3"),
            new Song(uploaderId, "Electronic Beat", "file:///android_asset/electronic.mp3"),
            new Song(uploaderId, "Acoustic Melody", "file:///android_asset/acoustic.mp3"),
            new Song(uploaderId, "Synthwave Dreams", "file:///android_asset/synthwave.mp3"),
            new Song(uploaderId, "Piano Ballad", "file:///android_asset/piano.mp3"),
            new Song(uploaderId, "Rock Anthem", "file:///android_asset/rock.mp3"),
            new Song(uploaderId, "Ambient Space", "file:///android_asset/ambient.mp3"),
            new Song(uploaderId, "Hip Hop Beat", "file:///android_asset/hiphop.mp3"),
            new Song(uploaderId, "Classical Suite", "file:///android_asset/classical.mp3")
        };

        // Set properties and insert songs
        for (Song song : demoSongs) {
            song.setCoverArtUrl(""); // Empty cover for now
            song.setPublic(true); // Make songs public
            song.setCreatedAt(System.currentTimeMillis());
            songDao.insert(song);
        }
    }

    /**
     * Create demo playlists for testing
     */
    private static void createDemoPlaylists(PlaylistDao playlistDao, long ownerId) {
        // Check if playlists already exist
        if (playlistDao.getAllPlaylistsSync().size() > 0) {
            return; // Playlists already exist
        }

        // Create demo playlists
        Playlist[] demoPlaylists = {
            new Playlist(ownerId, "My Favorites"),
            new Playlist(ownerId, "Chill Vibes"),
            new Playlist(ownerId, "Workout Mix"),
            new Playlist(ownerId, "Late Night"),
            new Playlist(ownerId, "Road Trip"),
            new Playlist(ownerId, "Focus Mode")
        };

        // Set properties and insert playlists
        long currentTime = System.currentTimeMillis();
        String[] descriptions = {
            "Collection of my favorite songs",
            "Relaxing music for study and work",
            "High energy songs for exercise",
            "Perfect for late night listening",
            "Songs for long drives",
            "Instrumental music for concentration"
        };
        boolean[] publicFlags = {true, true, true, false, true, true};

        for (int i = 0; i < demoPlaylists.length; i++) {
            Playlist playlist = demoPlaylists[i];
            playlist.setDescription(descriptions[i]);
            playlist.setPublic(publicFlags[i]);
            playlist.setCreatedAt(currentTime - (i * 86400000L)); // Each playlist created 1 day apart
            playlistDao.insert(playlist);
        }
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
