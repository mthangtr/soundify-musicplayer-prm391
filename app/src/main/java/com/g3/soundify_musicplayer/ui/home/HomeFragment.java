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
import com.g3.soundify_musicplayer.ui.playlist.PlaylistWithSongCountAdapter;
import com.g3.soundify_musicplayer.ui.playlist.PlaylistDetailFragment;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.dto.PlaylistWithSongCount;
import com.g3.soundify_musicplayer.data.dto.SongWithUploaderInfo;

// REMOVED: SimplePlaybackHandler - using Zero Queue Rule
import com.g3.soundify_musicplayer.ui.player.SongDetailViewModel;
import com.g3.soundify_musicplayer.ui.song.SongWithUploaderInfoAdapter;
import com.g3.soundify_musicplayer.ui.home.RecentSongWithUploaderInfoAdapter;
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
        // Initialize ViewModels
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        // Sử dụng SongDetailViewModel THỐNG NHẤT
        songDetailViewModel = new ViewModelProvider(requireActivity()).get(SongDetailViewModel.class);

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
        PlaylistWithSongCountAdapter playlistAdapter = new PlaylistWithSongCountAdapter(new ArrayList<>(), new PlaylistWithSongCountAdapter.OnPlaylistClickListener() {
            @Override
            public void onPlaylistClick(PlaylistWithSongCount playlistWithSongCount) {
                Toast.makeText(requireContext(), "Open playlist: " + playlistWithSongCount.getName(), Toast.LENGTH_SHORT).show();

                // Track playlist access
                homeViewModel.trackPlaylistAccess(playlistWithSongCount.getId());

                // Navigate to playlist detail
                navigateToPlaylistDetail(playlistWithSongCount.getId());
            }

            @Override
            public void onPlayButtonClick(PlaylistWithSongCount playlistWithSongCount) {
                Toast.makeText(requireContext(), "Play playlist: " + playlistWithSongCount.getName(), Toast.LENGTH_SHORT).show();

                // Track playlist access
                homeViewModel.trackPlaylistAccess(playlistWithSongCount.getId());

                // TODO: Implement play playlist functionality
            }

            @Override
            public void onEditPlaylist(PlaylistWithSongCount playlistWithSongCount) {
                // Not implemented for home - users should edit from My Playlists
                Toast.makeText(getContext(), "Edit playlists from My Playlists tab in Library", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeletePlaylist(PlaylistWithSongCount playlistWithSongCount) {
                // Not implemented for home - users should delete from My Playlists
                Toast.makeText(getContext(), "Delete playlists from My Playlists tab in Library", Toast.LENGTH_SHORT).show();
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
            if (userPlaylists != null && !userPlaylists.isEmpty()) {
                playlistAdapter.updateData(userPlaylists);
                // Show playlist section
                v.findViewById(R.id.tvMyPlaylists).setVisibility(View.VISIBLE);
                v.findViewById(R.id.rvMyPlaylists).setVisibility(View.VISIBLE);

                // Reset constraint: Suggested Songs constraint to My Playlists (default behavior)
            } else {
                // Hide playlist section but keep the constraint structure
                v.findViewById(R.id.tvMyPlaylists).setVisibility(View.GONE);
                v.findViewById(R.id.rvMyPlaylists).setVisibility(View.GONE);

                // Keep original constraint: Suggested Songs still constraint to My Playlists section
                // This maintains proper layout flow even when My Playlists is hidden
            }
        });

        // All Songs (Suggested) Adapter with Uploader Info
        suggestedAdapter = new SongWithUploaderInfoAdapter(
                new ArrayList<>(),
                new SongWithUploaderInfoAdapter.OnSongClick() {
                    @Override
                    public void onPlay(SongWithUploaderInfo songInfo) {
                        homeViewModel.trackRecentlyPlayed(songInfo.getId());

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

                    @Override
                    public void onEditSong(SongWithUploaderInfo songInfo) {
                        // Not implemented for suggested songs - users should edit from My Songs
                        Toast.makeText(getContext(), "Edit songs from My Songs tab in Library", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDeleteSong(SongWithUploaderInfo songInfo) {
                        // Not implemented for suggested songs - users should delete from My Songs
                        Toast.makeText(getContext(), "Delete songs from My Songs tab in Library", Toast.LENGTH_SHORT).show();
                    }
                });
        rv.setAdapter(suggestedAdapter);
        homeViewModel.getSuggestedSongs().observe(getViewLifecycleOwner(), suggestedSongsWithUploader -> {
            if (suggestedSongsWithUploader != null) {
                suggestedAdapter.updateData(suggestedSongsWithUploader);

                // Adjust spacing based on item count
                adjustSuggestedSongsSpacing(v, suggestedSongsWithUploader.size());
            } else {
            }
        });
    }

    // ========== QUEUE CONTEXT METHODS (FOLLOWING UserProfileFragment PATTERN) ==========

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

                } else {
                    // Many items: use normal margin
                    layoutParams.topMargin = (int) (8 * getResources().getDisplayMetrics().density); // 8dp

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
    private void navigateToPlaylistDetail(long playlistId) {
        try {
            // Create PlaylistDetailFragment with playlist data
            PlaylistDetailFragment playlistDetailFragment = PlaylistDetailFragment.newInstance(playlistId);

            // Navigate to playlist detail
            getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, playlistDetailFragment)
                .addToBackStack("playlist_detail")
                .commit();


        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error navigating to playlist detail", e);
            Toast.makeText(requireContext(), "Error opening playlist", Toast.LENGTH_SHORT).show();
        }
    }
}
