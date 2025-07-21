package com.g3.soundify_musicplayer.ui.player.comment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import com.g3.soundify_musicplayer.R;

/**
 * Fragment for displaying comments list for a song.
 * UI ONLY - No backend integration, uses mock data for demo purposes.
 */
public class CommentsFragment extends Fragment implements CommentAdapter.OnCommentLikeListener, CommentAdapter.OnCommentDeleteListener {

    private static final String ARG_SONG_ID = "song_id";

    // UI Components
    private ImageButton btnBack;
    private RecyclerView recyclerComments;
    private ProgressBar progressLoading;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutError;
    private TextView textError;
    private EditText editComment;
    private ImageButton btnSendComment;

    // ViewModel and Adapter
    private CommentsViewModel viewModel;
    private CommentAdapter adapter;

    // Callback interface for notifying parent about comment changes
    public interface CommentChangeListener {
        void onCommentCountChanged();
    }

    private CommentChangeListener commentChangeListener;

    // Data
    private long songId;

    public void setCommentChangeListener(CommentChangeListener listener) {
        this.commentChangeListener = listener;
    }

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
        setupViewModel();  // Initialize ViewModel first
        setupRecyclerView();  // Then setup RecyclerView (needs ViewModel)
        setupClickListeners();
        observeViewModel();

        // Load comments for the song
        loadComments();
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btn_back);
        recyclerComments = view.findViewById(R.id.recycler_comments);
        progressLoading = view.findViewById(R.id.progress_loading);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        layoutError = view.findViewById(R.id.layout_error);
        textError = view.findViewById(R.id.text_error);
        editComment = view.findViewById(R.id.edit_comment);
        btnSendComment = view.findViewById(R.id.btn_send_comment);
    }

    private void setupRecyclerView() {
        adapter = new CommentAdapter(requireContext());
        adapter.setOnCommentLikeListener(this);
        adapter.setOnCommentDeleteListener(this);

        // Add null check for viewModel
        if (viewModel != null) {
            adapter.setCurrentUserId(viewModel.getCurrentUserId());
        } else {
            // ViewModel is null - use default
            adapter.setCurrentUserId(-1); // Default to no user
        }

        recyclerComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerComments.setAdapter(adapter);
    }

    private void setupViewModel() {
        try {
            viewModel = new ViewModelProvider(this).get(CommentsViewModel.class);
            // Xóa log error thừa
        } catch (Exception e) {
            // Xóa log error thừa - handle gracefully
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        btnSendComment.setOnClickListener(v -> {
            String commentText = editComment.getText().toString().trim();
            if (!commentText.isEmpty()) {
                if (viewModel != null) {
                    viewModel.addComment(commentText);
                    editComment.setText(""); // Clear input after sending
                    // Hide keyboard
                    hideKeyboard();
                } else {
                    showToast("Unable to add comment. Please try again.");
                }
            } else {
                showToast("Please enter a comment");
            }
        });
    }

    private void observeViewModel() {
        if (viewModel == null) {
            // ViewModel is null - return early
            return;
        }

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

        // Observe error messages
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                updateUIState(false, true, error);
                showToast(error);
                // Clear the message after showing it
                viewModel.clearMessages();
            }
        });

        // Observe success messages
        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                showToast(message);
                // Notify parent about comment changes
                if (commentChangeListener != null) {
                    commentChangeListener.onCommentCountChanged();
                }
                // Clear the message after showing it
                viewModel.clearMessages();
            }
        });

        // Observe comment adding state
        viewModel.getIsAddingComment().observe(getViewLifecycleOwner(), isAdding -> {
            // Can be used to show/hide progress on add comment button
        });
    }

    private void loadComments() {
        if (viewModel == null) {
            // ViewModel is null - return early
            return;
        }

        LiveData<List<CommentWithUser>> commentsLiveData = viewModel.loadComments(songId);
        commentsLiveData.observe(getViewLifecycleOwner(), comments -> {
            if (comments != null) {
                adapter.setComments(comments);
                updateUIState(comments.isEmpty(), false, null);
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
        if (viewModel != null) {
            // Handle comment like/unlike in background
            // UI has already been updated immediately in the adapter
            viewModel.toggleCommentLike(comment.getCommentId());

            // No toast needed since UI feedback is immediate
        } else {
            showToast("Unable to like comment. Please try again.");
        }
    }

    @Override
    public void onCommentDelete(CommentWithUser comment, int position) {
        if (viewModel == null) {
            showToast("Unable to delete comment. Please try again.");
            return;
        }

        // Show confirmation dialog
        if (getContext() != null) {
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteComment(comment.getCommentId());
                })
                .setNegativeButton("Cancel", null)
                .show();
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void hideKeyboard() {
        if (getActivity() != null && editComment != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(editComment.getWindowToken(), 0);
            }
        }
    }
}
