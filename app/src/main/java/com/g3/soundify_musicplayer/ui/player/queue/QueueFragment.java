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
    private QueueViewModel viewModel;
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
        viewModel = new ViewModelProvider(this).get(QueueViewModel.class);
        
        if (getArguments() != null) {
            long songId = getArguments().getLong(ARG_SONG_ID);
            viewModel.loadQueue(songId);
        }
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
            // Handle song click - could play the selected song
            viewModel.moveToPosition(position);
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
        viewModel.getQueueItems().observe(getViewLifecycleOwner(), queueItems -> {
            if (queueItems != null) {
                adapter.updateData(queueItems);
                updateQueueTitle(queueItems.size());
            }
        });

        viewModel.getCurrentPosition().observe(getViewLifecycleOwner(), position -> {
            if (position != null) {
                adapter.setCurrentPosition(position);
            }
        });
    }

    private void updateQueueTitle(int queueSize) {
        String title = queueSize == 1 ? "1 song in queue" : queueSize + " songs in queue";
        textQueueTitle.setText(title);
    }
}
