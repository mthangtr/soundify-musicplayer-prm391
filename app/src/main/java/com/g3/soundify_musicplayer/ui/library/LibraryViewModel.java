package com.g3.soundify_musicplayer.ui.library;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.Song;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ViewModel for Library Screen
 * Provides mock data for My Songs, My Playlists, and Liked Songs tabs
 * UI ONLY - No backend integration, suitable for academic presentation
 */
public class LibraryViewModel extends ViewModel {

    // LiveData for each tab
    private final MutableLiveData<List<Song>> mySongs = new MutableLiveData<>();
    private final MutableLiveData<List<Playlist>> myPlaylists = new MutableLiveData<>();
    private final MutableLiveData<List<Song>> likedSongs = new MutableLiveData<>();

    // Mock current user ID
    private static final long CURRENT_USER_ID = 1L;

    public LibraryViewModel() {
        // Initialize with empty lists
        mySongs.setValue(new ArrayList<>());
        myPlaylists.setValue(new ArrayList<>());
        likedSongs.setValue(new ArrayList<>());
    }

    // Getters for LiveData
    public LiveData<List<Song>> getMySongs() {
        return mySongs;
    }

    public LiveData<List<Playlist>> getMyPlaylists() {
        return myPlaylists;
    }

    public LiveData<List<Song>> getLikedSongs() {
        return likedSongs;
    }

    /**
     * Load mock library data for all tabs
     */
    public void loadLibraryData() {
        loadMySongs();
        loadMyPlaylists();
        loadLikedSongs();
    }

    /**
     * Load mock songs uploaded by the current user
     */
    private void loadMySongs() {
        List<Song> mockMySongs = Arrays.asList(
            createMockSong(101L, "My Original Track", "Original composition with guitar and vocals",
                "file:///android_asset/my_original.mp3", "Electronic", 245000),
            createMockSong(102L, "Acoustic Cover", "Acoustic version of a popular song",
                "file:///android_asset/acoustic_cover.mp3", "Acoustic", 198000),
            createMockSong(103L, "Beat Drop", "High energy electronic dance music",
                "file:///android_asset/beat_drop.mp3", "EDM", 312000),
            createMockSong(104L, "Midnight Jazz", "Smooth jazz for late night listening",
                "file:///android_asset/midnight_jazz.mp3", "Jazz", 267000),
            createMockSong(105L, "Summer Vibes", "Upbeat pop song perfect for summer",
                "file:///android_asset/summer_vibes.mp3", "Pop", 189000),
            createMockSong(106L, "Rainy Day Blues", "Melancholic blues for contemplative moments",
                "file:///android_asset/rainy_blues.mp3", "Blues", 234000),
            createMockSong(107L, "Workout Anthem", "High-energy track to pump you up",
                "file:///android_asset/workout_anthem.mp3", "Hip-Hop", 198000),
            createMockSong(108L, "Sunset Dreams", "Dreamy indie track with soft vocals",
                "file:///android_asset/sunset_dreams.mp3", "Indie", 276000),
            createMockSong(109L, "City Nights", "Urban electronic with deep bass",
                "file:///android_asset/city_nights.mp3", "Electronic", 289000),
            createMockSong(110L, "Folk Story", "Acoustic storytelling with guitar",
                "file:///android_asset/folk_story.mp3", "Folk", 312000),
            createMockSong(111L, "Dance Floor", "Upbeat dance track for parties",
                "file:///android_asset/dance_floor.mp3", "Dance", 201000),
            createMockSong(112L, "Morning Coffee", "Smooth jazz for peaceful mornings",
                "file:///android_asset/morning_coffee_mine.mp3", "Jazz", 223000)
        );

        // Set cover art URLs for uploaded songs
        for (Song song : mockMySongs) {
            song.setCoverArtUrl("mock://cover/my_song_" + song.getId() + ".jpg");
        }

        mySongs.setValue(mockMySongs);
    }

    /**
     * Load mock playlists created by the current user
     */
    private void loadMyPlaylists() {
        List<Playlist> mockMyPlaylists = Arrays.asList(
            createMockPlaylist(201L, "My Workout Mix", "High energy songs for gym sessions",
                System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)), // 1 week ago
            createMockPlaylist(202L, "Study Focus", "Instrumental and ambient music for concentration",
                System.currentTimeMillis() - (14L * 24 * 60 * 60 * 1000)), // 2 weeks ago
            createMockPlaylist(203L, "Road Trip Classics", "Perfect songs for long drives",
                System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)), // 1 month ago
            createMockPlaylist(204L, "Chill Evening", "Relaxing songs for winding down",
                System.currentTimeMillis() - (45L * 24 * 60 * 60 * 1000)), // 1.5 months ago
            createMockPlaylist(205L, "Party Hits", "Upbeat songs for celebrations",
                System.currentTimeMillis() - (60L * 24 * 60 * 60 * 1000)), // 2 months ago
            createMockPlaylist(206L, "Late Night Vibes", "Perfect for midnight listening sessions",
                System.currentTimeMillis() - (3L * 24 * 60 * 60 * 1000)), // 3 days ago
            createMockPlaylist(207L, "Morning Motivation", "Energizing tracks to start your day",
                System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000)), // 10 days ago
            createMockPlaylist(208L, "Rainy Day Collection", "Cozy songs for cloudy weather",
                System.currentTimeMillis() - (21L * 24 * 60 * 60 * 1000)), // 3 weeks ago
            createMockPlaylist(209L, "Summer Memories", "Nostalgic tracks from summer adventures",
                System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)), // 3 months ago
            createMockPlaylist(210L, "Coding Soundtrack", "Background music for programming sessions",
                System.currentTimeMillis() - (5L * 24 * 60 * 60 * 1000)), // 5 days ago
            createMockPlaylist(211L, "Weekend Chill", "Relaxed tunes for lazy weekends",
                System.currentTimeMillis() - (2L * 24 * 60 * 60 * 1000)), // 2 days ago
            createMockPlaylist(212L, "Throwback Hits", "Classic songs from the past decades",
                System.currentTimeMillis() - (120L * 24 * 60 * 60 * 1000)) // 4 months ago
        );

        myPlaylists.setValue(mockMyPlaylists);
    }

    /**
     * Load mock songs liked/saved by the current user
     */
    private void loadLikedSongs() {
        List<Song> mockLikedSongs = Arrays.asList(
            createMockLikedSong(301L, "Starlight Dreams", "Dreamy indie pop with ethereal vocals",
                "file:///android_asset/starlight_dreams.mp3", "Indie Pop", 234000, 2L),
            createMockLikedSong(302L, "Thunder Road", "Classic rock anthem with powerful guitar",
                "file:///android_asset/thunder_road.mp3", "Rock", 298000, 3L),
            createMockLikedSong(303L, "Ocean Waves", "Ambient soundscape for relaxation",
                "file:///android_asset/ocean_waves.mp3", "Ambient", 456000, 4L),
            createMockLikedSong(304L, "City Lights", "Modern hip-hop with urban themes",
                "file:///android_asset/city_lights.mp3", "Hip-Hop", 187000, 5L),
            createMockLikedSong(305L, "Morning Coffee", "Smooth jazz perfect for breakfast",
                "file:///android_asset/morning_coffee.mp3", "Jazz", 223000, 6L),
            createMockLikedSong(306L, "Digital Love", "Synthwave with retro 80s vibes",
                "file:///android_asset/digital_love.mp3", "Synthwave", 276000, 7L),
            createMockLikedSong(307L, "Forest Path", "Folk acoustic with nature themes",
                "file:///android_asset/forest_path.mp3", "Folk", 201000, 8L),
            createMockLikedSong(308L, "Neon Nights", "Electronic dance with pulsing beats",
                "file:///android_asset/neon_nights.mp3", "Electronic", 267000, 9L),
            createMockLikedSong(309L, "Vintage Soul", "Classic soul with modern production",
                "file:///android_asset/vintage_soul.mp3", "Soul", 245000, 10L),
            createMockLikedSong(310L, "Mountain High", "Uplifting rock with soaring melodies",
                "file:///android_asset/mountain_high.mp3", "Rock", 312000, 11L),
            createMockLikedSong(311L, "Midnight Train", "Blues-rock journey through the night",
                "file:///android_asset/midnight_train.mp3", "Blues Rock", 289000, 12L),
            createMockLikedSong(312L, "Golden Hour", "Warm indie folk with acoustic guitar",
                "file:///android_asset/golden_hour.mp3", "Indie Folk", 198000, 13L),
            createMockLikedSong(313L, "Electric Storm", "Progressive rock with complex rhythms",
                "file:///android_asset/electric_storm.mp3", "Progressive Rock", 378000, 14L),
            createMockLikedSong(314L, "Velvet Voice", "Smooth R&B with silky vocals",
                "file:///android_asset/velvet_voice.mp3", "R&B", 234000, 15L),
            createMockLikedSong(315L, "Cosmic Journey", "Ambient space music for meditation",
                "file:///android_asset/cosmic_journey.mp3", "Ambient", 423000, 16L),
            createMockLikedSong(316L, "Street Symphony", "Hip-hop with orchestral elements",
                "file:///android_asset/street_symphony.mp3", "Hip-Hop", 201000, 17L),
            createMockLikedSong(317L, "Sunset Boulevard", "Nostalgic pop with 80s influences",
                "file:///android_asset/sunset_boulevard.mp3", "Synthpop", 256000, 18L),
            createMockLikedSong(318L, "Wild Heart", "Country rock with heartfelt lyrics",
                "file:///android_asset/wild_heart.mp3", "Country Rock", 278000, 19L),
            createMockLikedSong(319L, "Dreamscape", "Ethereal electronic with floating melodies",
                "file:///android_asset/dreamscape.mp3", "Chillwave", 345000, 20L),
            createMockLikedSong(320L, "Fire & Ice", "Dramatic orchestral with electronic fusion",
                "file:///android_asset/fire_ice.mp3", "Cinematic", 298000, 21L)
        );

        // Set cover art URLs for liked songs
        for (Song song : mockLikedSongs) {
            song.setCoverArtUrl("mock://cover/liked_song_" + song.getId() + ".jpg");
        }

        likedSongs.setValue(mockLikedSongs);
    }

    /**
     * Create a mock song uploaded by the current user
     */
    private Song createMockSong(long id, String title, String description, String audioUrl, 
                               String genre, int durationMs) {
        Song song = new Song(CURRENT_USER_ID, title, audioUrl);
        song.setId(id);
        song.setDescription(description);
        song.setGenre(genre);
        song.setDurationMs(durationMs);
        song.setPublic(true);
        song.setCreatedAt(System.currentTimeMillis() - (id * 86400000L)); // Different creation times
        return song;
    }

    /**
     * Create a mock playlist owned by the current user
     */
    private Playlist createMockPlaylist(long id, String name, String description, long createdAt) {
        Playlist playlist = new Playlist(CURRENT_USER_ID, name);
        playlist.setId(id);
        playlist.setDescription(description);
        playlist.setPublic(true);
        playlist.setCreatedAt(createdAt);
        return playlist;
    }

    /**
     * Create a mock liked song from another user
     */
    private Song createMockLikedSong(long id, String title, String description, String audioUrl, 
                                   String genre, int durationMs, long uploaderId) {
        Song song = new Song(uploaderId, title, audioUrl);
        song.setId(id);
        song.setDescription(description);
        song.setGenre(genre);
        song.setDurationMs(durationMs);
        song.setPublic(true);
        song.setCreatedAt(System.currentTimeMillis() - (id * 86400000L)); // Different creation times
        return song;
    }

    /**
     * Refresh data for a specific tab
     */
    public void refreshTab(int tabIndex) {
        switch (tabIndex) {
            case 0: // My Songs
                loadMySongs();
                break;
            case 1: // My Playlists
                loadMyPlaylists();
                break;
            case 2: // Liked Songs
                loadLikedSongs();
                break;
        }
    }

    /**
     * Get current user ID
     */
    public long getCurrentUserId() {
        return CURRENT_USER_ID;
    }
}
