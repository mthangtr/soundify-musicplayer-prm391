package com.g3.soundify_musicplayer.ui.player;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;

/**
 * Fragment for displaying comments list for a song.
 * UI ONLY - No backend integration, uses mock data for demo purposes.
 */
public class CommentsFragment extends Fragment implements CommentAdapter.OnCommentLikeListener {

    private static final String ARG_SONG_ID = "song_id";

    // UI Components
    private ImageButton btnBack;
    private RecyclerView recyclerComments;
    private ProgressBar progressLoading;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutError;
    private TextView textError;

    // ViewModel and Adapter
    private CommentsViewModel viewModel;
    private CommentAdapter adapter;

    // Data
    private long songId;

    public static CommentsFragment newInstance(long songId) {
        CommentsFragment fragment = new CommentsFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SONG_ID, songId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            songId = getArguments().getLong(ARG_SONG_ID, 1L);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_comments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupRecyclerView();
        setupViewModel();
        setupClickListeners();
        observeViewModel();
        
        // Load comments for the song
        viewModel.loadComments(songId);
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btn_back);
        recyclerComments = view.findViewById(R.id.recycler_comments);
        progressLoading = view.findViewById(R.id.progress_loading);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        layoutError = view.findViewById(R.id.layout_error);
        textError = view.findViewById(R.id.text_error);
    }

    private void setupRecyclerView() {
        adapter = new CommentAdapter(requireContext());
        adapter.setOnCommentLikeListener(this);
        
        recyclerComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerComments.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(CommentsViewModel.class);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    private void observeViewModel() {
        // Observe comments data
        viewModel.getComments().observe(getViewLifecycleOwner(), comments -> {
            if (comments != null) {
                adapter.setComments(comments);
                updateUIState(comments.isEmpty(), false, null);
            }
        });

        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                if (isLoading) {
                    // Hide other states while loading
                    layoutEmpty.setVisibility(View.GONE);
                    layoutError.setVisibility(View.GONE);
                    recyclerComments.setVisibility(View.GONE);
                }
            }
        });

        // Observe error state
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                updateUIState(false, true, error);
                showToast("Error loading comments");
            }
        });
    }

    private void updateUIState(boolean isEmpty, boolean hasError, String errorMessage) {
        if (hasError) {
            // Show error state
            layoutError.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            recyclerComments.setVisibility(View.GONE);
            if (errorMessage != null) {
                textError.setText(errorMessage);
            }
        } else if (isEmpty) {
            // Show empty state
            layoutEmpty.setVisibility(View.VISIBLE);
            layoutError.setVisibility(View.GONE);
            recyclerComments.setVisibility(View.GONE);
        } else {
            // Show comments list
            recyclerComments.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            layoutError.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCommentLike(CommentWithUser comment, int position) {
        // Handle comment like/unlike
        viewModel.toggleCommentLike(comment, position);
        
        // Show feedback toast
        String message = comment.isLiked() ? "Liked comment" : "Unliked comment";
        showToast(message);
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
