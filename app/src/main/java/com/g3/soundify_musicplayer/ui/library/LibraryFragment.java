package com.g3.soundify_musicplayer.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.Adapter.PlaylistAdapter;
import com.g3.soundify_musicplayer.data.Adapter.SongAdapter;
import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.ui.player.MiniPlayerManager;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Library Fragment - Contains 3 sub-tabs: My Songs, My Playlists, Liked Songs
 * 100% MOCK DATA - No backend, no database, no network calls
 * SIMPLE UI TESTING - Hardcoded lists for demo purposes
 * Perfect for academic presentation and UI testing
 */
public class LibraryFragment extends Fragment {

    // UI Components
    private TabLayout tabLayout;
    private RecyclerView mySongsRecyclerView;
    private RecyclerView myPlaylistsRecyclerView;
    private RecyclerView likedSongsRecyclerView;
    private LinearLayout emptyStateLayout;
    private TextView emptyStateTitle;
    private TextView emptyStateSubtitle;

    // Adapters and Data
    private SongAdapter mySongsAdapter;
    private PlaylistAdapter myPlaylistsAdapter;
    private SongAdapter likedSongsAdapter;

    // Mock data
    private List<Song> mySongs;
    private List<Playlist> myPlaylists;
    private List<Song> likedSongs;

    // Current tab state
    private int currentTab = 0; // 0: My Songs, 1: My Playlists, 2: Liked Songs

    // Tab constants
    private static final int TAB_MY_SONGS = 0;
    private static final int TAB_MY_PLAYLISTS = 1;
    private static final int TAB_LIKED_SONGS = 2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupTabs();
        setupRecyclerViews();

        // Show initial tab
        switchTab(TAB_MY_SONGS);
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tab_layout);
        mySongsRecyclerView = view.findViewById(R.id.recycler_my_songs);
        myPlaylistsRecyclerView = view.findViewById(R.id.recycler_my_playlists);
        likedSongsRecyclerView = view.findViewById(R.id.recycler_liked_songs);
        emptyStateLayout = view.findViewById(R.id.empty_state_layout);
        emptyStateTitle = view.findViewById(R.id.empty_state_title);
        emptyStateSubtitle = view.findViewById(R.id.empty_state_subtitle);
    }

    private void createMockData() {
        // SIMPLE MOCK DATA - No backend, no database, just hardcoded lists for UI testing

        // My Songs - Songs I uploaded
        mySongs = Arrays.asList(
            createMockSong(101L, "My Original Track", "Electronic"),
            createMockSong(102L, "Acoustic Cover", "Acoustic"),
            createMockSong(103L, "Beat Drop", "EDM"),
            createMockSong(104L, "Midnight Jazz", "Jazz"),
            createMockSong(105L, "Summer Vibes", "Pop"),
            createMockSong(106L, "Guitar Solo", "Rock"),
            createMockSong(107L, "Piano Dreams", "Classical")
        );

        // My Playlists - Playlists I created
        myPlaylists = Arrays.asList(
            createMockPlaylist(201L, "My Workout Mix", "High energy songs for gym sessions"),
            createMockPlaylist(202L, "Study Focus", "Instrumental and ambient music"),
            createMockPlaylist(203L, "Road Trip Classics", "Perfect songs for long drives"),
            createMockPlaylist(204L, "Chill Evening", "Relaxing songs for winding down"),
            createMockPlaylist(205L, "Party Hits", "Upbeat songs for celebrations"),
            createMockPlaylist(206L, "Sleep Sounds", "Peaceful music for bedtime")
        );

        // Liked Songs - Songs I liked from other artists
        likedSongs = Arrays.asList(
            createMockSong(301L, "Starlight Dreams", "Indie Pop"),
            createMockSong(302L, "Thunder Road", "Rock"),
            createMockSong(303L, "Ocean Waves", "Ambient"),
            createMockSong(304L, "City Lights", "Hip-Hop"),
            createMockSong(305L, "Morning Coffee", "Jazz"),
            createMockSong(306L, "Digital Love", "Synthwave"),
            createMockSong(307L, "Forest Path", "Folk"),
            createMockSong(308L, "Neon Nights", "Electronic"),
            createMockSong(309L, "Vintage Soul", "Soul")
        );
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_my_songs));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_my_playlists));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_liked_songs));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                switchTab(currentTab);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerViews() {
        // Create mock data directly
        createMockData();

        // Setup My Songs RecyclerView
        mySongsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mySongsAdapter = new SongAdapter(mySongs, new SongAdapter.OnSongClick() {
            @Override
            public void onPlay(Song song) {
                showToast("Playing: " + song.getTitle());
                showMiniPlayer(song);
            }

            @Override
            public void onOpenDetail(Song song) {
                showToast("Open detail: " + song.getTitle());
            }
        });
        mySongsRecyclerView.setAdapter(mySongsAdapter);

        // Setup My Playlists RecyclerView
        myPlaylistsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        myPlaylistsAdapter = new PlaylistAdapter(myPlaylists, new PlaylistAdapter.OnPlaylistClickListener() {
            @Override
            public void onPlaylistClick(Playlist playlist) {
                showToast("Open playlist: " + playlist.getName());
            }

            @Override
            public void onPlayButtonClick(Playlist playlist) {
                showToast("Play playlist: " + playlist.getName());
            }
        });
        myPlaylistsRecyclerView.setAdapter(myPlaylistsAdapter);

        // Setup Liked Songs RecyclerView
        likedSongsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        likedSongsAdapter = new SongAdapter(likedSongs, new SongAdapter.OnSongClick() {
            @Override
            public void onPlay(Song song) {
                showToast("Playing liked song: " + song.getTitle());
                showMiniPlayer(song);
            }

            @Override
            public void onOpenDetail(Song song) {
                showToast("Open detail: " + song.getTitle());
            }
        });
        likedSongsRecyclerView.setAdapter(likedSongsAdapter);
    }



    private void switchTab(int tabIndex) {
        // Hide all RecyclerViews
        mySongsRecyclerView.setVisibility(View.GONE);
        myPlaylistsRecyclerView.setVisibility(View.GONE);
        likedSongsRecyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);

        // Show the selected tab's RecyclerView
        switch (tabIndex) {
            case TAB_MY_SONGS:
                mySongsRecyclerView.setVisibility(View.VISIBLE);
                break;
            case TAB_MY_PLAYLISTS:
                myPlaylistsRecyclerView.setVisibility(View.VISIBLE);
                break;
            case TAB_LIKED_SONGS:
                likedSongsRecyclerView.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateEmptyState(boolean isEmpty, String title, String subtitle) {
        if (isEmpty) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            emptyStateTitle.setText(title);
            emptyStateSubtitle.setText(subtitle);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    private void showMiniPlayer(Song song) {
        // Create a mock artist for the song
        User mockArtist = createMockArtist(song.getUploaderId());
        
        // Show mini player using the global manager
        MiniPlayerManager.getInstance().showMiniPlayer(song, mockArtist);
    }

    private User createMockArtist(long artistId) {
        User artist = new User();
        artist.setId(artistId);
        artist.setUsername("demo_artist");
        artist.setDisplayName("Demo Artist");
        artist.setAvatarUrl("mock://avatar/demo_artist.jpg");
        artist.setCreatedAt(System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)); // 30 days ago
        return artist;
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private Song createMockSong(long id, String title, String genre) {
        Song song = new Song(1L, title, "file:///android_asset/" + title.toLowerCase().replace(" ", "_") + ".mp3");
        song.setId(id);
        song.setGenre(genre);
        song.setDurationMs(180000 + (int)(Math.random() * 120000)); // 3-5 minutes
        song.setCoverArtUrl(""); // Empty like HomeFragment
        return song;
    }

    private Playlist createMockPlaylist(long id, String name, String description) {
        Playlist playlist = new Playlist(1L, name);
        playlist.setId(id);
        playlist.setDescription(description);
        playlist.setPublic(true);
        playlist.setCreatedAt(System.currentTimeMillis() - (id * 86400000L));
        return playlist;
    }
}
