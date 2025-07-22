package com.g3.soundify_musicplayer.ui.player.comment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;

import java.util.List;

/**
 * Activity for displaying comments list for a song.
 * Converted from Fragment-based to pure Activity-based architecture
 */
public class CommentActivity extends AppCompatActivity implements CommentAdapter.OnCommentLikeListener, CommentAdapter.OnCommentDeleteListener {

    private static final String EXTRA_SONG_ID = "song_id";
    public static final String RESULT_COMMENT_COUNT_CHANGED = "comment_count_changed";

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

    // Data
    private long songId;
    private boolean commentCountChanged = false;

    public static Intent createIntent(Context context, long songId) {
        Intent intent = new Intent(context, CommentActivity.class);
        intent.putExtra(EXTRA_SONG_ID, songId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        songId = getIntent().getLongExtra(EXTRA_SONG_ID, 1L);

        initViews();
        setupViewModel();
        setupRecyclerView();
        setupClickListeners();
        observeViewModel();

        loadComments();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        recyclerComments = findViewById(R.id.recycler_comments);
        progressLoading = findViewById(R.id.progress_loading);
        layoutEmpty = findViewById(R.id.layout_empty);
        layoutError = findViewById(R.id.layout_error);
        textError = findViewById(R.id.text_error);
        editComment = findViewById(R.id.edit_comment);
        btnSendComment = findViewById(R.id.btn_send_comment);
    }

    private void setupRecyclerView() {
        adapter = new CommentAdapter(this);
        adapter.setOnCommentLikeListener(this);
        adapter.setOnCommentDeleteListener(this);

        // Add null check for viewModel
        if (viewModel != null) {
            adapter.setCurrentUserId(viewModel.getCurrentUserId());
        } else {
            // ViewModel is null - use default
            adapter.setCurrentUserId(-1); // Default to no user
        }

        recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        recyclerComments.setAdapter(adapter);
    }

    private void setupViewModel() {
        try {
            viewModel = new ViewModelProvider(this).get(CommentsViewModel.class);
        } catch (Exception e) {
            // Handle gracefully
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            finishWithResult();
        });

        btnSendComment.setOnClickListener(v -> {
            String commentText = editComment.getText().toString().trim();
            if (!commentText.isEmpty()) {
                if (viewModel != null) {
                    viewModel.addComment(commentText);
                    editComment.setText(""); // Clear input after sending
                    // Hide keyboard
                    hideKeyboard();
                    commentCountChanged = true;
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
            return;
        }

        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                updateUIState(false, isLoading, null);
            }
        });

        // Observe error messages
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                updateUIState(false, false, errorMessage);
                showToast(errorMessage);
            }
        });

        // Observe success messages
        viewModel.getSuccessMessage().observe(this, successMessage -> {
            if (successMessage != null && !successMessage.isEmpty()) {
                showToast(successMessage);
                commentCountChanged = true;
            }
        });

        // Observe comment adding state
        viewModel.getIsAddingComment().observe(this, isAdding -> {
            if (isAdding != null) {
                btnSendComment.setEnabled(!isAdding);
            }
        });
    }

    private void loadComments() {
        if (viewModel == null) {
            return;
        }

        LiveData<List<CommentWithUser>> commentsLiveData = viewModel.loadComments(songId);
        commentsLiveData.observe(this, comments -> {
            if (comments != null) {
                adapter.setComments(comments);
                updateUIState(comments.isEmpty(), false, null);
            }
        });
    }

    private void updateUIState(boolean isEmpty, boolean isLoading, String errorMessage) {
        if (isLoading) {
            progressLoading.setVisibility(View.VISIBLE);
            recyclerComments.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
            layoutError.setVisibility(View.GONE);
        } else if (errorMessage != null && !errorMessage.isEmpty()) {
            progressLoading.setVisibility(View.GONE);
            recyclerComments.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
            layoutError.setVisibility(View.VISIBLE);
            textError.setText(errorMessage);
        } else if (isEmpty) {
            progressLoading.setVisibility(View.GONE);
            recyclerComments.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            layoutError.setVisibility(View.GONE);
        } else {
            progressLoading.setVisibility(View.GONE);
            recyclerComments.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            layoutError.setVisibility(View.GONE);
        }
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    public void onCommentLike(CommentWithUser comment, int position) {
        if (viewModel != null) {
            viewModel.toggleCommentLike(comment.getCommentId());
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
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Comment")
            .setMessage("Are you sure you want to delete this comment?")
            .setPositiveButton("Delete", (dialog, which) -> {
                viewModel.deleteComment(comment.getCommentId());
                commentCountChanged = true;
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishWithResult();
    }

    private void finishWithResult() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(RESULT_COMMENT_COUNT_CHANGED, commentCountChanged);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
