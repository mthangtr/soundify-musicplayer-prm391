package com.g3.soundify_musicplayer.ui.player.queue;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.repository.MediaPlayerRepository;
import com.g3.soundify_musicplayer.ui.player.SongDetailViewModel;
import com.g3.soundify_musicplayer.utils.RepositoryManager;

import java.util.List;

/**
 * ✅ Queue Fragment - Simple UI for displaying and managing current song list
 * Works with Zero Queue Rule system - shows currentSongList from MediaPlayerRepository
 */
public class QueueFragment extends Fragment {

    private static final String ARG_SONG_ID = "song_id";

    // UI Components
    private ImageButton btnBack;
    private TextView textQueueTitle;
    private RecyclerView recyclerViewQueue;

    // Repository and Adapter (Direct access for simplicity)
    private MediaPlayerRepository mediaPlayerRepository;
    private SongDetailViewModel songDetailViewModel; // Keep for compatibility
    private QueueAdapter adapter;
    private ItemTouchHelper itemTouchHelper;

    public static QueueFragment newInstance(long songId) {
        QueueFragment fragment = new QueueFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SONG_ID, songId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Direct repository access for better performance
        RepositoryManager repositoryManager = RepositoryManager.getInstance(requireActivity().getApplication());
        mediaPlayerRepository = repositoryManager.getMediaPlayerRepository();

        // Keep ViewModel for compatibility (some methods might still use it)
        songDetailViewModel = new ViewModelProvider(requireActivity()).get(SongDetailViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_queue, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupClickListeners();
        setupRecyclerView();
        setupObservers();
    }

    private void initializeViews(View view) {
        btnBack = view.findViewById(R.id.btn_back);
        textQueueTitle = view.findViewById(R.id.text_queue_title);
        recyclerViewQueue = view.findViewById(R.id.recycler_view_queue);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new QueueAdapter(requireContext());

        // ✅ OPTION 1: Direct repository calls (faster)
        adapter.setOnItemClickListener((song, position) -> {
            android.util.Log.d("QueueFragment", "🎵 Clicked song at position " + position + ": " + song.getTitle());
            // Direct call to repository for better performance
            mediaPlayerRepository.jumpToIndex(position);
        });

        adapter.setOnItemMoveListener((fromPosition, toPosition) -> {
            android.util.Log.d("QueueFragment", "🔄 Moving song from " + fromPosition + " to " + toPosition);
            // Direct call to repository for better performance
            mediaPlayerRepository.moveItemInList(fromPosition, toPosition);
        });

        // ✅ OPTION 2: ViewModel calls (for consistency) - commented out
        // adapter.setOnItemClickListener((song, position) -> songDetailViewModel.playSongAtIndex(position));
        // adapter.setOnItemMoveListener((fromPosition, toPosition) -> songDetailViewModel.moveSongInQueue(fromPosition, toPosition));

        recyclerViewQueue.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewQueue.setAdapter(adapter);

        // Setup drag & drop
        ItemTouchHelper.Callback callback = new QueueItemTouchHelperCallback(adapter);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerViewQueue);

        adapter.setItemTouchHelper(itemTouchHelper);
    }

    private void setupObservers() {
        // ✅ FIXED: Get ALL data from queueInfo directly - no race conditions
        mediaPlayerRepository.getQueueInfo().observe(getViewLifecycleOwner(), queueInfo -> {
            if (queueInfo != null) {
                // ✅ Get current songs from repository
                List<Song> songs = mediaPlayerRepository.getCurrentQueue();
                
                // ✅ Get currentIndex from queueInfo directly (no transformation race)
                int currentIndex = queueInfo.getCurrentIndex();
                
                // ✅ Get title from queueInfo directly
                String title = queueInfo.getQueueTitle();
                
                // ✅ Get current artist from playback state
                User currentArtist = null;
                if (mediaPlayerRepository.getCurrentPlaybackState().getValue() != null) {
                    currentArtist = mediaPlayerRepository.getCurrentPlaybackState().getValue().getCurrentArtist();
                }

                // ✅ Update adapter with consistent data
                adapter.updateData(songs, currentIndex, currentArtist);
                updateQueueTitle(queueInfo.getTotalSongs(), title != null ? title : "Queue");

                android.util.Log.d("QueueFragment", "✅ Queue updated: " + queueInfo.getTotalSongs() + " songs, index: " + currentIndex);
            }
        });
        
        // ✅ ADDITIONAL: Observe playback state for artist updates
        mediaPlayerRepository.getCurrentPlaybackState().observe(getViewLifecycleOwner(), playbackState -> {
            if (playbackState != null && playbackState.getCurrentArtist() != null) {
                // Update artist info in adapter
                adapter.updateCurrentArtist(playbackState.getCurrentArtist());
            }
        });
    }

    private void updateQueueTitle(int queueSize, String queueTitle) {
        String title;
        if (queueTitle != null && !queueTitle.isEmpty()) {
            title = queueTitle + " (" + queueSize + " songs)";
        } else {
            title = queueSize == 1 ? "1 song in queue" : queueSize + " songs in queue";
        }
        textQueueTitle.setText(title);
    }
}
