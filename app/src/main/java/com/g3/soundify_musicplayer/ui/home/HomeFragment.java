package com.g3.soundify_musicplayer.ui.home;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.ui.playlist.PlaylistAdapter;
import com.g3.soundify_musicplayer.ui.playlist.PlaylistDetailFragment;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.dto.SongWithUploaderInfo;
import com.g3.soundify_musicplayer.data.model.NavigationContext;
import com.g3.soundify_musicplayer.ui.player.SongDetailViewModel;
import com.g3.soundify_musicplayer.ui.song.SongWithUploaderInfoAdapter;
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

                // Navigate to playlist detail
                navigateToPlaylistDetail(playlist);
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
                        android.util.Log.e("DEBUG_HOME", "=== PLAY BUTTON CLICKED ===");
                        android.util.Log.e("DEBUG_HOME", "Song: " + songInfo.getTitle() + " (ID: " + songInfo.getId() + ")");

                        Toast.makeText(requireContext(), "Playing: " + songInfo.getTitle(), Toast.LENGTH_SHORT).show();

                        // Track recently played
                        android.util.Log.e("DEBUG_HOME", "About to track recently played...");
                        homeViewModel.trackRecentlyPlayed(songInfo.getId());

                        // FIXED: Play with NavigationContext for full queue support
                        playRecentSongWithContext(songInfo, recentAdapter);
                    }
                });
        rvRecentlyPlayed.setAdapter(recentAdapter);

        // Observe recent songs with uploader info from ViewModel
        homeViewModel.getRecentSongs().observe(getViewLifecycleOwner(), recentSongsWithUploader -> {
            android.util.Log.e("DEBUG_HOME", "Recent songs observer triggered");
            if (recentSongsWithUploader != null) {
                android.util.Log.e("DEBUG_HOME", "Recent songs count: " + recentSongsWithUploader.size());
                recentAdapter.updateSongs(recentSongsWithUploader);
            } else {
                android.util.Log.e("DEBUG_HOME", "Recent songs is NULL");
            }
        });

        // Observe user's playlists from ViewModel
        homeViewModel.getUserPlaylists().observe(getViewLifecycleOwner(), userPlaylists -> {
            android.util.Log.e("DEBUG_HOME", "User playlists observer triggered");
            if (userPlaylists != null && !userPlaylists.isEmpty()) {
                android.util.Log.e("DEBUG_HOME", "User playlists count: " + userPlaylists.size());
                playlistAdapter.updateData(userPlaylists);
                // Show playlist section
                v.findViewById(R.id.tvMyPlaylists).setVisibility(View.VISIBLE);
                v.findViewById(R.id.rvMyPlaylists).setVisibility(View.VISIBLE);

                // Reset constraint: Suggested Songs constraint to My Playlists (default behavior)
                android.util.Log.e("DEBUG_HOME", "Showing My Playlists - using default constraint");
            } else {
                android.util.Log.e("DEBUG_HOME", "User playlists is empty or NULL - hiding section");
                // Hide playlist section but keep the constraint structure
                v.findViewById(R.id.tvMyPlaylists).setVisibility(View.GONE);
                v.findViewById(R.id.rvMyPlaylists).setVisibility(View.GONE);

                // Keep original constraint: Suggested Songs still constraint to My Playlists section
                // This maintains proper layout flow even when My Playlists is hidden
                android.util.Log.e("DEBUG_HOME", "My Playlists hidden but constraint structure maintained");
            }
        });

        // All Songs (Suggested) Adapter with Uploader Info
        suggestedAdapter = new SongWithUploaderInfoAdapter(
                new ArrayList<>(),
                new SongWithUploaderInfoAdapter.OnSongClick() {
                    @Override
                    public void onPlay(SongWithUploaderInfo songInfo) {
                        Toast.makeText(requireContext(), "Playing: " + songInfo.getTitle(), Toast.LENGTH_SHORT).show();

                        // Track recently played
                        android.util.Log.d("HomeFragment", "Tracking recently played song: " + songInfo.getTitle() + " (ID: " + songInfo.getId() + ")");
                        homeViewModel.trackRecentlyPlayed(songInfo.getId());

                        // FIXED: Play with NavigationContext for full queue support
                        playSuggestedSongWithContext(songInfo, suggestedAdapter);
                    }

                    @Override
                    public void onOpenDetail(SongWithUploaderInfo songInfo) {
                        android.util.Log.e("DEBUG_HOME", "=== SONG ITEM CLICKED (onOpenDetail) ===");
                        android.util.Log.e("DEBUG_HOME", "Song: " + songInfo.getTitle() + " (ID: " + songInfo.getId() + ")");

                        Toast.makeText(requireContext(), "Open detail: " + songInfo.getTitle() +
                                " by " + songInfo.getDisplayUploaderName(),
                                Toast.LENGTH_SHORT).show();

                        // Track recently played when user clicks on song
                        android.util.Log.e("DEBUG_HOME", "About to track recently played from onOpenDetail...");
                        homeViewModel.trackRecentlyPlayed(songInfo.getId());

                        // FIXED: Play with NavigationContext for full queue support
                        playSuggestedSongWithContext(songInfo, suggestedAdapter);
                    }
                });
        rv.setAdapter(suggestedAdapter);

        // Observe suggested songs with uploader info from ViewModel
        homeViewModel.getSuggestedSongs().observe(getViewLifecycleOwner(), suggestedSongsWithUploader -> {
            android.util.Log.e("DEBUG_HOME", "Suggested songs observer triggered");
            if (suggestedSongsWithUploader != null) {
                android.util.Log.e("DEBUG_HOME", "Suggested songs count: " + suggestedSongsWithUploader.size());
                suggestedAdapter.updateData(suggestedSongsWithUploader);

                // Adjust spacing based on item count
                adjustSuggestedSongsSpacing(v, suggestedSongsWithUploader.size());
            } else {
                android.util.Log.e("DEBUG_HOME", "Suggested songs is NULL");
            }
        });
    }
    
    // Demo playlist method removed - now using real user playlists from database

    // Helper method to show mini player with song
    private void showMiniPlayer(Song song) {
        // Create basic user info from song (deprecated approach)
        User basicUser = new User("user_" + song.getUploaderId(), "User " + song.getUploaderId(), "user@example.com", "");
        basicUser.setId(song.getUploaderId());

        // Show mini player using SongDetailViewModel THỐNG NHẤT
        songDetailViewModel.playSong(song, basicUser);

        android.util.Log.w("HomeFragment", "showMiniPlayer with basic user info - prefer SongWithUploaderInfo approach");
    }

    // Helper method to show mini player with song and real uploader
    private void showMiniPlayerWithUploader(Song song, User uploader) {
        // Show mini player using SongDetailViewModel THỐNG NHẤT
        songDetailViewModel.playSong(song, uploader);
    }

    // Helper method to show mini player with SongWithUploaderInfo
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

        // Create User object from uploader info
        User uploader = new User();
        uploader.setId(songInfo.getUploaderId());
        uploader.setUsername(songInfo.getUploaderUsername());
        uploader.setDisplayName(songInfo.getUploaderDisplayName());
        uploader.setAvatarUrl(songInfo.getUploaderAvatarUrl());

        // TẠO NAVIGATION CONTEXT từ Home (General context)
        // Tạo danh sách chỉ chứa bài hát hiện tại (đơn giản cho Home)
        java.util.List<Long> songIds = new java.util.ArrayList<>();
        songIds.add(songInfo.getId());

        NavigationContext context = NavigationContext.fromGeneral(
            "Home - Suggested Songs",
            songIds,
            0
        );

        // Show mini player với context để tạo queue
        songDetailViewModel.playSongWithContext(song, uploader, context);

        android.util.Log.d("HomeFragment", "Playing song with context - Song: " +
            song.getTitle() + ", Context: Home");
    }

    // Mock artist method removed - using real user data from SongWithUploaderInfo

    // ========== QUEUE CONTEXT METHODS (FOLLOWING UserProfileFragment PATTERN) ==========

    /**
     * Play recent song with full NavigationContext for queue support
     * PATTERN: Same as UserProfileFragment.createNavigationContextAndPlay()
     */
    private void playRecentSongWithContext(SongWithUploaderInfo songInfo, RecentSongWithUploaderInfoAdapter adapter) {
        // Null check for adapter
        if (adapter == null) {
            android.util.Log.w("HomeFragment", "RecentAdapter is null, falling back to single song");
            showMiniPlayerWithSongInfo(songInfo);
            return;
        }

        // Get all recent songs from adapter
        List<SongWithUploaderInfo> allRecentSongs = adapter.getSongs();
        if (allRecentSongs == null || allRecentSongs.isEmpty()) {
            android.util.Log.w("HomeFragment", "Recent songs list is empty, falling back to single song");
            showMiniPlayerWithSongInfo(songInfo); // Fallback to single song
            return;
        }

        // Find position of clicked song
        int position = -1;
        for (int i = 0; i < allRecentSongs.size(); i++) {
            if (allRecentSongs.get(i).getId() == songInfo.getId()) {
                position = i;
                break;
            }
        }

        if (position == -1) {
            showMiniPlayerWithSongInfo(songInfo); // Fallback to single song
            return;
        }

        // Create song IDs list
        List<Long> songIds = new ArrayList<>();
        for (SongWithUploaderInfo s : allRecentSongs) {
            songIds.add(s.getId());
        }

        // Create NavigationContext for Recently Played
        NavigationContext context = NavigationContext.fromGeneral(
            "Recently Played",
            songIds,
            position
        );

        // Convert SongWithUploaderInfo to Song and User
        Song song = convertToSong(songInfo);
        User uploader = convertToUser(songInfo);

        // Play with context for full queue support
        songDetailViewModel.playSongWithContext(song, uploader, context);

        android.util.Log.d("HomeFragment", "Playing recent song with context - Queue size: " +
            songIds.size() + ", Position: " + position);
    }

    /**
     * Play suggested song with full NavigationContext for queue support
     * PATTERN: Same as UserProfileFragment.createNavigationContextAndPlay()
     */
    private void playSuggestedSongWithContext(SongWithUploaderInfo songInfo, SongWithUploaderInfoAdapter adapter) {
        // Null check for adapter
        if (adapter == null) {
            android.util.Log.w("HomeFragment", "SuggestedAdapter is null, falling back to single song");
            showMiniPlayerWithSongInfo(songInfo);
            return;
        }

        // Get all suggested songs from adapter
        List<SongWithUploaderInfo> allSuggestedSongs = adapter.getCurrentData();
        if (allSuggestedSongs == null || allSuggestedSongs.isEmpty()) {
            android.util.Log.w("HomeFragment", "Suggested songs list is empty, falling back to single song");
            showMiniPlayerWithSongInfo(songInfo); // Fallback to single song
            return;
        }

        // Find position of clicked song
        int position = -1;
        for (int i = 0; i < allSuggestedSongs.size(); i++) {
            if (allSuggestedSongs.get(i).getId() == songInfo.getId()) {
                position = i;
                break;
            }
        }

        if (position == -1) {
            showMiniPlayerWithSongInfo(songInfo); // Fallback to single song
            return;
        }

        // Create song IDs list
        List<Long> songIds = new ArrayList<>();
        for (SongWithUploaderInfo s : allSuggestedSongs) {
            songIds.add(s.getId());
        }

        // Create NavigationContext for Suggested Songs
        NavigationContext context = NavigationContext.fromGeneral(
            "Suggested For You",
            songIds,
            position
        );

        // Convert SongWithUploaderInfo to Song and User
        Song song = convertToSong(songInfo);
        User uploader = convertToUser(songInfo);

        // Play with context for full queue support
        songDetailViewModel.playSongWithContext(song, uploader, context);

        android.util.Log.d("HomeFragment", "Playing suggested song with context - Queue size: " +
            songIds.size() + ", Position: " + position);
    }

    // ========== CONVERSION HELPER METHODS ==========

    /**
     * Convert SongWithUploaderInfo to Song object
     */
    private Song convertToSong(SongWithUploaderInfo songInfo) {
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
        User user = new User();
        user.setId(songInfo.getUploaderId());
        user.setDisplayName(songInfo.getDisplayUploaderName());
        user.setUsername(songInfo.getUploaderUsername());
        user.setAvatarUrl(songInfo.getUploaderAvatarUrl());
        return user;
    }

    /**
     * Adjust Suggested Songs spacing based on item count
     * For ≤3 items: reduce spacing to make it look better
     * For ≥4 items: use normal spacing
     */
    private void adjustSuggestedSongsSpacing(View rootView, int itemCount) {
        try {
            androidx.recyclerview.widget.RecyclerView rvSongs = rootView.findViewById(R.id.rvSongs);
            if (rvSongs != null) {
                ViewGroup.MarginLayoutParams layoutParams =
                    (ViewGroup.MarginLayoutParams) rvSongs.getLayoutParams();

                if (itemCount <= 3) {
                    // Few items: reduce top margin to bring closer to title
                    layoutParams.topMargin = 0; // No margin
                    android.util.Log.d("HomeFragment", "Adjusted spacing for " + itemCount + " items: 0dp margin");
                } else {
                    // Many items: use normal margin
                    layoutParams.topMargin = (int) (8 * getResources().getDisplayMetrics().density); // 8dp
                    android.util.Log.d("HomeFragment", "Normal spacing for " + itemCount + " items: 8dp margin");
                }

                rvSongs.setLayoutParams(layoutParams);
            }
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error adjusting suggested songs spacing", e);
        }
    }

    /**
     * Navigate to playlist detail
     */
    private void navigateToPlaylistDetail(Playlist playlist) {
        try {
            // Create PlaylistDetailFragment with playlist data
            PlaylistDetailFragment playlistDetailFragment = PlaylistDetailFragment.newInstance(playlist.getId());

            // Navigate to playlist detail
            getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, playlistDetailFragment)
                .addToBackStack("playlist_detail")
                .commit();

            android.util.Log.d("HomeFragment", "Navigating to playlist: " + playlist.getName());
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error navigating to playlist detail", e);
            Toast.makeText(requireContext(), "Error opening playlist", Toast.LENGTH_SHORT).show();
        }
    }
}
