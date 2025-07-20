package com.g3.soundify_musicplayer.data.Fragment;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.Adapter.RecentSongAdapter;
import com.g3.soundify_musicplayer.data.Adapter.SongAdapter;
import com.g3.soundify_musicplayer.data.Adapter.PlaylistAdapter;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.ui.player.MiniPlayerManager;
import com.g3.soundify_musicplayer.viewmodel.HomeViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;

    public HomeFragment() { super(R.layout.fragment_home); }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        // Initialize ViewModel
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Recently Played RecyclerView
        RecyclerView rvRecentlyPlayed = v.findViewById(R.id.rvRecentlyPlayed);
        rvRecentlyPlayed.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        // All Songs RecyclerView
        RecyclerView rv = v.findViewById(R.id.rvSongs);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        // My Playlists RecyclerView
        RecyclerView rvMyPlaylists = v.findViewById(R.id.rvMyPlaylists);
        rvMyPlaylists.setLayoutManager(new LinearLayoutManager(requireContext()));

        // ✨ Hardcoded playlist data
        List<Playlist> demoPlaylists = Arrays.asList(
                createDemoPlaylist(1, "My Favorites", "My personal favorite songs collection"),
                createDemoPlaylist(2, "Chill Vibes", "Perfect for relaxing and studying")
        );

        // My Playlists Adapter
        PlaylistAdapter playlistAdapter = new PlaylistAdapter(demoPlaylists, new PlaylistAdapter.OnPlaylistClickListener() {
            @Override
            public void onPlaylistClick(Playlist playlist) {
                Toast.makeText(requireContext(), "Open playlist: " + playlist.getName(), Toast.LENGTH_SHORT).show();

                // Track playlist access
                homeViewModel.trackPlaylistAccess(playlist.getId());

                // TODO: Navigate to playlist detail
            }

            @Override
            public void onPlayButtonClick(Playlist playlist) {
                Toast.makeText(requireContext(), "Play playlist: " + playlist.getName(), Toast.LENGTH_SHORT).show();

                // Track playlist access
                homeViewModel.trackPlaylistAccess(playlist.getId());

                // TODO: Implement play playlist functionality
            }
        });
        rvMyPlaylists.setAdapter(playlistAdapter);

        // Recently Played Adapter
        RecentSongAdapter recentAdapter = new RecentSongAdapter(new ArrayList<>(), new RecentSongAdapter.OnRecentSongClick() {
            @Override
            public void onPlay(Song s) {
                Toast.makeText(requireContext(), "Playing: " + s.getTitle(), Toast.LENGTH_SHORT).show();

                // Track recently played
                homeViewModel.trackRecentlyPlayed(s.getId());

                // Show mini player with the selected song
                showMiniPlayer(s);
            }
        });
        rvRecentlyPlayed.setAdapter(recentAdapter);

        // Observe recent songs from ViewModel
        homeViewModel.getRecentSongs().observe(getViewLifecycleOwner(), recentSongs -> {
            if (recentSongs != null) {
                recentAdapter.updateSongs(recentSongs);
                android.util.Log.d("HomeFragment", "Recent songs updated: " + recentSongs.size() + " songs");
            }
        });

        // Observe recent playlists from ViewModel
        homeViewModel.getRecentPlaylists().observe(getViewLifecycleOwner(), recentPlaylists -> {
            if (recentPlaylists != null) {
                playlistAdapter.updateData(recentPlaylists);
                android.util.Log.d("HomeFragment", "Recent playlists updated: " + recentPlaylists.size() + " playlists");
            }
        });

        // All Songs (Suggested) Adapter
        SongAdapter adt = new SongAdapter(new ArrayList<>(), new SongAdapter.OnSongClick() {
            @Override public void onPlay(Song s) {
                Toast.makeText(requireContext(), "Playing: " + s.getTitle(), Toast.LENGTH_SHORT).show();

                // Track recently played
                homeViewModel.trackRecentlyPlayed(s.getId());

                // Show mini player with the selected song
                showMiniPlayer(s);
            }
            @Override public void onOpenDetail(Song s) {
                Toast.makeText(requireContext(), "Open detail " + s.getTitle(), Toast.LENGTH_SHORT).show();
            }
        });
        rv.setAdapter(adt);

        // Observe suggested songs from ViewModel
        homeViewModel.getSuggestedSongs().observe(getViewLifecycleOwner(), suggestedSongs -> {
            if (suggestedSongs != null) {
                adt.updateData(suggestedSongs);
                android.util.Log.d("HomeFragment", "Suggested songs updated: " + suggestedSongs.size() + " songs");
            }
        });
    }
    
    // Helper method to create demo playlists
    private Playlist createDemoPlaylist(long id, String name, String description) {
        Playlist playlist = new Playlist(1L, name); // ownerId = 1
        playlist.setId(id);
        playlist.setDescription(description);
        playlist.setPublic(true);
        playlist.setCreatedAt(System.currentTimeMillis() - (id * 86400000L)); // Different creation times
        return playlist;
    }

    // Helper method to show mini player with song
    private void showMiniPlayer(Song song) {
        // Create a mock artist for the song
        User mockArtist = createMockArtist(song.getUploaderId());

        // Show mini player using the global manager
        MiniPlayerManager.getInstance().showMiniPlayer(song, mockArtist);
    }

    // Helper method to create mock artist
    private User createMockArtist(long artistId) {
        User artist = new User();
        artist.setId(artistId);
        artist.setUsername("demo_artist");
        artist.setDisplayName("Demo Artist");
        artist.setAvatarUrl("mock://avatar/demo_artist.jpg");
        artist.setCreatedAt(System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)); // 30 days ago
        return artist;
    }
}
