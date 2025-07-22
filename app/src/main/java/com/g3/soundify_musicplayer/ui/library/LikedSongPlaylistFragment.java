package com.g3.soundify_musicplayer.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.model.NavigationContext;
import com.g3.soundify_musicplayer.ui.home.SongAdapter;
import com.g3.soundify_musicplayer.ui.player.SongDetailViewModel;

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
    private Button buttonPlayAll;
    private Button buttonShuffle;

    // ViewModels và Adapter
    private LikedSongPlaylistViewModel viewModel;
    private SongDetailViewModel songDetailViewModel;
    private SongAdapter songsAdapter;

    // Data
    private List<Song> currentLikedSongs = new ArrayList<>();

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
        
        android.util.Log.d("LikedSongPlaylistFragment", "🔄 onViewCreated called - Initializing LikedSongPlaylistFragment");

        // Initialize ViewModels
        viewModel = new ViewModelProvider(this).get(LikedSongPlaylistViewModel.class);
        songDetailViewModel = new ViewModelProvider(requireActivity()).get(SongDetailViewModel.class);

        android.util.Log.d("LikedSongPlaylistFragment", "✅ ViewModels initialized");

        // Test database connection
        viewModel.testDatabaseConnection();

        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
        
        android.util.Log.d("LikedSongPlaylistFragment", "✅ LikedSongPlaylistFragment setup completed");
    }

    private void initViews(View view) {
        recyclerViewLikedSongs = view.findViewById(R.id.recycler_view_liked_songs);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        layoutLoading = view.findViewById(R.id.layout_loading);
        textSongCount = view.findViewById(R.id.text_song_count);
        buttonPlayAll = view.findViewById(R.id.button_play_all);
        buttonShuffle = view.findViewById(R.id.button_shuffle);
    }

    private void setupRecyclerView() {
        recyclerViewLikedSongs.setLayoutManager(new LinearLayoutManager(getContext()));
        
        songsAdapter = new SongAdapter(new ArrayList<>(), new SongAdapter.OnSongClick() {
            @Override
            public void onPlay(Song song) {
                playSelectedSong(song);
            }

            @Override
            public void onOpenDetail(Song song) {
                playSelectedSong(song);
            }
        });
        
        recyclerViewLikedSongs.setAdapter(songsAdapter);
    }

    private void setupClickListeners() {
        buttonPlayAll.setOnClickListener(v -> playAllSongs(false));
        buttonShuffle.setOnClickListener(v -> playAllSongs(true));
    }

    private void observeViewModel() {
        // Observer liked songs
        android.util.Log.d("LikedSongPlaylistFragment", "🔍 Setting up observers...");
        
        viewModel.getLikedSongs().observe(getViewLifecycleOwner(), songs -> {
            android.util.Log.d("LikedSongPlaylistFragment", "🎵 Liked songs observer triggered - Songs: " + 
                (songs != null ? songs.size() + " items" : "NULL"));
            
            if (songs != null) {
                for (int i = 0; i < Math.min(songs.size(), 3); i++) {
                    Song song = songs.get(i);
                    android.util.Log.d("LikedSongPlaylistFragment", "   Song " + i + ": " + song.getTitle() + " (ID: " + song.getId() + ")");
                }
                
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
                updateActionButtonsState(count > 0);
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
    private void playSelectedSong(Song song) {
        if (currentLikedSongs.isEmpty()) {
            showToast("Không có bài hát nào để phát");
            return;
        }

        // Tạo NavigationContext cho Liked Songs
        List<Long> songIds = new ArrayList<>();
        int clickedPosition = 0;

        for (int i = 0; i < currentLikedSongs.size(); i++) {
            Song s = currentLikedSongs.get(i);
            songIds.add(s.getId());
            
            if (s.getId() == song.getId()) {
                clickedPosition = i;
            }
        }

        NavigationContext context = NavigationContext.fromGeneral(
            "Liked Songs",
            songIds,
            clickedPosition
        );

        // Tạo mock artist
        User mockArtist = createMockArtist(song.getUploaderId());

        // Phát bài hát với context
        songDetailViewModel.playSongWithContext(song, mockArtist, context);

        showToast("Đang phát: " + song.getTitle());
    }

    /**
     * Play all songs (với shuffle nếu cần)
     */
    private void playAllSongs(boolean shuffle) {
        if (currentLikedSongs.isEmpty()) {
            showToast("Không có bài hát nào để phát");
            return;
        }

        // Lấy bài đầu tiên để bắt đầu phát
        Song firstSong = currentLikedSongs.get(0);
        
        // Tạo NavigationContext
        List<Long> songIds = new ArrayList<>();
        for (Song song : currentLikedSongs) {
            songIds.add(song.getId());
        }

        NavigationContext context = NavigationContext.fromGeneral(
            "Liked Songs" + (shuffle ? " (Shuffle)" : ""),
            songIds,
            0
        );

        // Tạo mock artist
        User mockArtist = createMockArtist(firstSong.getUploaderId());

        // Phát với context
        songDetailViewModel.playSongWithContext(firstSong, mockArtist, context);

        // TODO: Implement shuffle logic nếu cần
        if (shuffle) {
            showToast("Phát ngẫu nhiên " + currentLikedSongs.size() + " bài hát yêu thích");
        } else {
            showToast("Phát tất cả " + currentLikedSongs.size() + " bài hát yêu thích");
        }
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
     * Enable/disable action buttons based on songs availability
     */
    private void updateActionButtonsState(boolean hasData) {
        buttonPlayAll.setEnabled(hasData);
        buttonShuffle.setEnabled(hasData);
        
        buttonPlayAll.setAlpha(hasData ? 1.0f : 0.5f);
        buttonShuffle.setAlpha(hasData ? 1.0f : 0.5f);
    }

    /**
     * Create mock artist for song playback
     */
    private User createMockArtist(long artistId) {
        User artist = new User();
        artist.setId(artistId);
        artist.setUsername("artist_" + artistId);
        artist.setDisplayName("Artist " + artistId);
        artist.setAvatarUrl("mock://avatar/artist_" + artistId + ".jpg");
        artist.setCreatedAt(System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)); // 30 days ago
        return artist;
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
        android.util.Log.d("LikedSongPlaylistFragment", "🔄 onResume called - Refreshing liked songs data");
        
        // Refresh data để ensure real-time sync
        if (viewModel != null) {
            viewModel.refreshLikedSongs();
        } else {
            android.util.Log.e("LikedSongPlaylistFragment", "❌ ViewModel is NULL in onResume");
        }
    }
} 