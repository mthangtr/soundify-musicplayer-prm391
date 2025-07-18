package com.g3.soundify_musicplayer.data.Fragment;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.Adapter.RecentSongAdapter;
import com.g3.soundify_musicplayer.data.Adapter.SongAdapter;
import com.g3.soundify_musicplayer.data.Adapter.PlaylistAdapter;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.Playlist;

import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {

    public HomeFragment() { super(R.layout.fragment_home); }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
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
                // TODO: Navigate to playlist detail
            }
            
            @Override
            public void onPlayButtonClick(Playlist playlist) {
                Toast.makeText(requireContext(), "Play playlist: " + playlist.getName(), Toast.LENGTH_SHORT).show();
                // TODO: Implement play playlist functionality
            }
        });
        rvMyPlaylists.setAdapter(playlistAdapter);

        // ✨ Dữ liệu cứng tạm thời
        List<Song> demo = Arrays.asList(
                new Song(1, "Lofi Chill", "file:///android_asset/lofi.mp3"),
                new Song(2, "Future Bass", "file:///android_asset/future.mp3"),
                new Song(3, "Guitar Solo", "file:///android_asset/guitar.mp3"),
                new Song(4, "Jazz Night", "file:///android_asset/jazz.mp3")
        );

        // gán cover tạm
        for (Song s : demo) s.setCoverArtUrl("");

        // Recently Played Adapter
        RecentSongAdapter recentAdapter = new RecentSongAdapter(demo, new RecentSongAdapter.OnRecentSongClick() {
            @Override
            public void onPlay(Song s) {
                Toast.makeText(requireContext(), "Play recent: " + s.getTitle(), Toast.LENGTH_SHORT).show();
                // TODO: Implement play functionality
            }
        });
        rvRecentlyPlayed.setAdapter(recentAdapter);

        // All Songs Adapter
        SongAdapter adt = new SongAdapter(demo, new SongAdapter.OnSongClick() {
            @Override public void onPlay(Song s) {
                Toast.makeText(requireContext(), "Play " + s.getTitle(), Toast.LENGTH_SHORT).show();
            }
            @Override public void onOpenDetail(Song s) {
                Toast.makeText(requireContext(), "Open detail " + s.getTitle(), Toast.LENGTH_SHORT).show();
            }
        });
        rv.setAdapter(adt);
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
}
