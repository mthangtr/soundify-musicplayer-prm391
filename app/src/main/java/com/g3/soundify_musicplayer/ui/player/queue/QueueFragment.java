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
import com.g3.soundify_musicplayer.ui.player.SongDetailViewModel;

import java.util.List;

/**
 * Queue Fragment - Shows current song and upcoming queue
 * UI ONLY - Uses mock data for demonstration
 */
public class QueueFragment extends Fragment {

    private static final String ARG_SONG_ID = "song_id";

    // UI Components
    private ImageButton btnBack;
    private TextView textQueueTitle;
    private RecyclerView recyclerViewQueue;

    // ViewModel and Adapter
    private SongDetailViewModel songDetailViewModel;
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
        // Use shared ViewModel from Activity to access real queue data
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
        adapter.setOnItemClickListener((song, position) -> {
            // Play the selected song
            songDetailViewModel.playSongAtIndex(position);
        });

        adapter.setOnItemMoveListener((fromPosition, toPosition) -> {
            // Move song in queue
            songDetailViewModel.moveSongInQueue(fromPosition, toPosition);
        });

        recyclerViewQueue.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewQueue.setAdapter(adapter);

        // Setup drag & drop
        ItemTouchHelper.Callback callback = new QueueItemTouchHelperCallback(adapter);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerViewQueue);

        adapter.setItemTouchHelper(itemTouchHelper);
    }

    private void setupObservers() {
        // Observer 1: Queue list changes (only when queue actually changes - drag/drop, add/remove)
        songDetailViewModel.getCurrentQueueLiveData().observe(getViewLifecycleOwner(), queueList -> {
            if (queueList != null && !queueList.isEmpty()) {
                // Get current index and artist info
                Integer currentIndex = songDetailViewModel.getCurrentQueueIndex().getValue();
                User currentArtist = songDetailViewModel.getCurrentArtistDirect();

                // Update entire adapter data
                adapter.updateData(queueList, currentIndex != null ? currentIndex : -1, currentArtist);

                android.util.Log.d("QueueFragment", "Queue list updated - " + queueList.size() + " songs");
            }
        });

        // Observer 2: Current playing index changes (only updates playing indicator)
        songDetailViewModel.getCurrentQueueIndex().observe(getViewLifecycleOwner(), currentIndex -> {
            if (currentIndex != null) {
                // Only update the playing indicator, don't reload entire list
                adapter.updateCurrentPlayingIndex(currentIndex);

                android.util.Log.d("QueueFragment", "Current index updated - " + currentIndex);
            }
        });

        // Observer 3: Queue info for title updates
        songDetailViewModel.getQueueInfo().observe(getViewLifecycleOwner(), queueInfo -> {
            if (queueInfo != null) {
                updateQueueTitle(queueInfo.getTotalSongs(), queueInfo.getQueueTitle());
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
