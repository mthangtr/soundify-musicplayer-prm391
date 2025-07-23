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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.dto.SongWithUploaderInfo;

import com.g3.soundify_musicplayer.ui.player.SongDetailViewModel;
import com.g3.soundify_musicplayer.ui.song.SongAdapter;
import com.g3.soundify_musicplayer.ui.song.SongWithUploaderInfoAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment hiển thị danh sách bài hát đã thích của user
 * Lấy data trực tiếp từ bảng song_likes, không liên quan đến playlist backend
 */
public class LikedSongPlaylistFragment extends Fragment {

    // UI Components
    private RecyclerView recyclerViewLikedSongs;
    private LinearLayout layoutEmptyState;
    private LinearLayout layoutLoading;
    private TextView textSongCount;

    // ViewModels và Adapter
    private LikedSongPlaylistViewModel viewModel;
    private SongDetailViewModel songDetailViewModel;
    private SongWithUploaderInfoAdapter songsAdapter;

    // Data
    private List<SongWithUploaderInfo> currentLikedSongs = new ArrayList<>();

    public static LikedSongPlaylistFragment newInstance() {
        return new LikedSongPlaylistFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_liked_song_playlist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModels
        viewModel = new ViewModelProvider(this).get(LikedSongPlaylistViewModel.class);
        songDetailViewModel = new ViewModelProvider(requireActivity()).get(SongDetailViewModel.class);

        // Test database connection
        viewModel.testDatabaseConnection();

        initViews(view);
        setupRecyclerView();
        observeViewModel();
    }

    private void initViews(View view) {
        recyclerViewLikedSongs = view.findViewById(R.id.recycler_view_liked_songs);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        layoutLoading = view.findViewById(R.id.layout_loading);
        textSongCount = view.findViewById(R.id.text_song_count);
    }

    private void setupRecyclerView() {
        recyclerViewLikedSongs.setLayoutManager(new LinearLayoutManager(getContext()));

        songsAdapter = new SongWithUploaderInfoAdapter(new ArrayList<>(), new SongWithUploaderInfoAdapter.OnSongClick() {
            @Override
            public void onPlay(SongWithUploaderInfo song) {
                playSelectedSong(song);
            }

            @Override
            public void onOpenDetail(SongWithUploaderInfo song) {
                playSelectedSong(song);
            }

            @Override
            public void onEditSong(SongWithUploaderInfo song) {
                showToast("Không thể chỉnh sửa bài hát này");
            }

            @Override
            public void onDeleteSong(SongWithUploaderInfo song) {
                showToast("Sử dụng nút unlike để bỏ thích bài hát");
            }
        });

        recyclerViewLikedSongs.setAdapter(songsAdapter);
    }



    private void observeViewModel() {
        // Observer liked songs
        viewModel.getLikedSongs().observe(getViewLifecycleOwner(), songs -> {
            if (songs != null) {

                currentLikedSongs = songs;
                songsAdapter.updateData(songs);
                viewModel.updateSongCount(songs.size());
                updateUIState(songs.isEmpty());
            } else {
                android.util.Log.w("LikedSongPlaylistFragment", "⚠️ Liked songs list is NULL");
            }
        });

        // Observer song count
        viewModel.getSongCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                updateSongCountText(count);
            }
        });

        // Observer loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                layoutLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        // Observer error messages
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showToast(errorMessage);
                viewModel.clearErrorMessage();
            }
        });
    }

    /**
     * Play một bài hát cụ thể với context của Liked Songs playlist
     */
    private void playSelectedSong(SongWithUploaderInfo songWithUploader) {
        if (currentLikedSongs.isEmpty()) {
            showToast("Không có bài hát nào để phát");
            return;
        }

        // Convert SongWithUploaderInfo to Song list for playback
        List<Song> songList = new ArrayList<>();
        int clickedPosition = 0;

        for (int i = 0; i < currentLikedSongs.size(); i++) {
            SongWithUploaderInfo s = currentLikedSongs.get(i);

            // Convert to Song object
            Song song = convertToSong(s);
            songList.add(song);

            if (s.getId() == songWithUploader.getId()) {
                clickedPosition = i;
            }
        }

        // ✅ CONSISTENT: Use playFromView with full liked songs for navigation
        songDetailViewModel.playFromView(songList, "Liked Songs", clickedPosition);

        showToast("Đang phát: " + songWithUploader.getTitle());
    }

    /**
     * Convert SongWithUploaderInfo to Song
     */
    private Song convertToSong(SongWithUploaderInfo songWithUploader) {
        Song song = new Song();
        song.setId(songWithUploader.getId());
        song.setUploaderId(songWithUploader.getUploaderId());
        song.setTitle(songWithUploader.getTitle());
        song.setDescription(songWithUploader.getDescription());
        song.setAudioUrl(songWithUploader.getAudioUrl());
        song.setCoverArtUrl(songWithUploader.getCoverArtUrl());
        song.setGenre(songWithUploader.getGenre());
        song.setDurationMs(songWithUploader.getDurationMs());
        song.setPublic(songWithUploader.isPublic());
        song.setCreatedAt(songWithUploader.getCreatedAt());

        // Set uploader name from the joined data
        song.setUploaderName(songWithUploader.getDisplayUploaderName());

        return song;
    }



    /**
     * Update UI state based on data availability
     */
    private void updateUIState(boolean isEmpty) {
        if (isEmpty) {
            recyclerViewLikedSongs.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerViewLikedSongs.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    /**
     * Update song count text
     */
    private void updateSongCountText(int count) {
        if (count == 0) {
            textSongCount.setText("Chưa có bài hát nào");
        } else if (count == 1) {
            textSongCount.setText("1 bài hát");
        } else {
            textSongCount.setText(count + " bài hát");
        }
    }



    /**
     * Show toast message
     */
    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Refresh data when fragment becomes visible
     */
    @Override
    public void onResume() {
        super.onResume();

        // Refresh data để ensure real-time sync
        if (viewModel != null) {
            viewModel.refreshLikedSongs();
        } else {
            android.util.Log.e("LikedSongPlaylistFragment", "❌ ViewModel is NULL in onResume");
        }
    }
} 