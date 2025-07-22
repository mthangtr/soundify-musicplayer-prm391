package com.g3.soundify_musicplayer.ui.home;

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
import com.g3.soundify_musicplayer.ui.playlist.PlaylistAdapter;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.dto.SongWithUploaderInfo;

// REMOVED: SimplePlaybackHandler - using Zero Queue Rule
import com.g3.soundify_musicplayer.ui.player.SongDetailViewModel;
import com.g3.soundify_musicplayer.viewmodel.HomeViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private SongDetailViewModel songDetailViewModel;

    // Adapter fields for queue context access
    private RecentSongWithUploaderInfoAdapter recentAdapter;
    private SongWithUploaderInfoAdapter suggestedAdapter;

    public HomeFragment() { super(R.layout.fragment_home); }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        android.util.Log.e("DEBUG_HOME", "=== HomeFragment onViewCreated START ===");

        // Initialize ViewModels
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        // Sử dụng SongDetailViewModel THỐNG NHẤT
        songDetailViewModel = new ViewModelProvider(requireActivity()).get(SongDetailViewModel.class);

        android.util.Log.e("DEBUG_HOME", "ViewModels initialized");

        android.util.Log.d("HomeFragment", "SongDetailViewModel initialized: " + songDetailViewModel.hashCode());

        // Recently Played RecyclerView
        RecyclerView rvRecentlyPlayed = v.findViewById(R.id.rvRecentlyPlayed);
        rvRecentlyPlayed.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        // All Songs RecyclerView
        RecyclerView rv = v.findViewById(R.id.rvSongs);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        // My Playlists RecyclerView
        RecyclerView rvMyPlaylists = v.findViewById(R.id.rvMyPlaylists);
        rvMyPlaylists.setLayoutManager(new LinearLayoutManager(requireContext()));

        // My Playlists Adapter - Start with empty list, will be populated by ViewModel
        PlaylistAdapter playlistAdapter = new PlaylistAdapter(new ArrayList<>(), new PlaylistAdapter.OnPlaylistClickListener() {
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

        // Recently Played Adapter with Uploader Info
        recentAdapter = new RecentSongWithUploaderInfoAdapter(
                new ArrayList<>(),
                new RecentSongWithUploaderInfoAdapter.OnRecentSongClick() {
                    @Override
                    public void onPlay(SongWithUploaderInfo songInfo) {
                        homeViewModel.trackRecentlyPlayed(songInfo.getId());

                        // ✅ CONSISTENT: Use playFromView for all fragments
                        List<Song> allSongs = convertToSongs(recentAdapter.getSongs());
                        int position = findSongPosition(songInfo, recentAdapter.getSongs());
                        songDetailViewModel.playFromView(allSongs, "Recently Played", position);
                    }
                });
        rvRecentlyPlayed.setAdapter(recentAdapter);
        homeViewModel.getRecentSongs().observe(getViewLifecycleOwner(), recentSongsWithUploader -> {
            if (recentSongsWithUploader != null) {
                recentAdapter.updateSongs(recentSongsWithUploader);
            } else {
            }
        });

        // Observe user's playlists from ViewModel
        homeViewModel.getUserPlaylists().observe(getViewLifecycleOwner(), userPlaylists -> {
            if (userPlaylists != null) {
                playlistAdapter.updateData(userPlaylists);
            }
        });

        // All Songs (Suggested) Adapter with Uploader Info
        suggestedAdapter = new SongWithUploaderInfoAdapter(
                new ArrayList<>(),
                new SongWithUploaderInfoAdapter.OnSongClick() {
                    @Override
                    public void onPlay(SongWithUploaderInfo songInfo) {
                        homeViewModel.trackRecentlyPlayed(songInfo.getId());

                        // ✅ CONSISTENT: Use playFromView for all fragments
                        List<Song> allSongs = convertToSongs(suggestedAdapter.getCurrentData());
                        int position = findSongPosition(songInfo, suggestedAdapter.getCurrentData());
                        songDetailViewModel.playFromView(allSongs, "Suggested For You", position);
                    }

                    @Override
                    public void onOpenDetail(SongWithUploaderInfo songInfo) {
                        homeViewModel.trackRecentlyPlayed(songInfo.getId());

                        // ✅ CONSISTENT: Use playFromView for all fragments
                        List<Song> allSongs = convertToSongs(suggestedAdapter.getCurrentData());
                        int position = findSongPosition(songInfo, suggestedAdapter.getCurrentData());
                        songDetailViewModel.playFromView(allSongs, "Suggested For You", position);
                    }
                });
        rv.setAdapter(suggestedAdapter);
        homeViewModel.getSuggestedSongs().observe(getViewLifecycleOwner(), suggestedSongsWithUploader -> {
            if (suggestedSongsWithUploader != null) {
                suggestedAdapter.updateData(suggestedSongsWithUploader);
            } else {
            }
        });
    }
    private void showMiniPlayerWithSongInfo(SongWithUploaderInfo songInfo) {
        // Create Song object from SongWithUploaderInfo
        Song song = new Song(songInfo.getUploaderId(), songInfo.getTitle(), songInfo.getAudioUrl());
        song.setId(songInfo.getId());
        song.setDescription(songInfo.getDescription());
        song.setCoverArtUrl(songInfo.getCoverArtUrl());
        song.setGenre(songInfo.getGenre());
        song.setDurationMs(songInfo.getDurationMs());
        song.setPublic(songInfo.isPublic());
        song.setCreatedAt(songInfo.getCreatedAt());

        // Play song directly
        // Find position in suggested songs and play from current view
        List<SongWithUploaderInfo> suggestedSongs = suggestedAdapter != null ? suggestedAdapter.getCurrentData() : new ArrayList<>();
        int position = findSongPosition(songInfo, suggestedSongs);

        android.util.Log.d("HomeFragment", "Playing song with context - Song: " +
            song.getTitle() + ", Context: Home");
    }

    /**
     * ✅ Convert list of SongWithUploaderInfo to Song objects
     */
    private List<Song> convertToSongs(List<SongWithUploaderInfo> songInfoList) {
        List<Song> songs = new ArrayList<>();
        if (songInfoList != null) {
            for (SongWithUploaderInfo songInfo : songInfoList) {
                songs.add(convertToSong(songInfo));
            }
        }
        return songs;
    }

    /**
     * Helper method để check section nào đang active
     */
    private boolean isRecentSectionActive() {
        // TODO: Implement logic để check tab nào đang active
        // Tạm thời return false (suggested section)
        return false;
    }

    /**
     * Helper method để tìm position của song trong list
     */
    private int findSongPosition(SongWithUploaderInfo targetSong, List<SongWithUploaderInfo> songList) {
        if (targetSong == null || songList == null) return 0;

        for (int i = 0; i < songList.size(); i++) {
            SongWithUploaderInfo song = songList.get(i);
            if (song != null && song.getId() == targetSong.getId()) {
                return i;
            }
        }
        return 0; // Default to first position if not found
    }

    /**
     * Convert SongWithUploaderInfo to Song object (from SimplePlaybackHandler)
     * Made public to match interface requirement
     */
    public Song convertToSong(SongWithUploaderInfo songInfo) {
        if (songInfo == null) return null;

        Song song = new Song();
        song.setId(songInfo.getId());
        song.setTitle(songInfo.getTitle());
        song.setUploaderId(songInfo.getUploaderId());
        song.setAudioUrl(songInfo.getAudioUrl());
        song.setCoverArtUrl(songInfo.getCoverArtUrl());
        song.setDescription(songInfo.getDescription());
        song.setGenre(songInfo.getGenre());
        song.setDurationMs(songInfo.getDurationMs());
        song.setPublic(songInfo.isPublic());
        song.setCreatedAt(songInfo.getCreatedAt());
        return song;
    }

    /**
     * Convert SongWithUploaderInfo to User object
     */
    private User convertToUser(SongWithUploaderInfo songInfo) {
        if (songInfo == null) return null;

        User user = new User();
        user.setId(songInfo.getUploaderId());
        user.setDisplayName(songInfo.getDisplayUploaderName());
        user.setUsername(songInfo.getUploaderUsername());
        user.setAvatarUrl(songInfo.getUploaderAvatarUrl());
        return user;
    }
}
