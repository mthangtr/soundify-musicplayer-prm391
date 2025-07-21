package com.g3.soundify_musicplayer.ui.player.comment;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.repository.CommentRepository;
import com.g3.soundify_musicplayer.utils.AuthManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for the Comments screen.
 * Handles comment CRUD operations and like functionality with real database integration.
 */
public class CommentsViewModel extends AndroidViewModel {

    private final CommentRepository commentRepository;
    private final AuthManager authManager;
    private final ExecutorService executor;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isAddingComment = new MutableLiveData<>(false);

    private long currentSongId;

    public CommentsViewModel(@NonNull Application application) {
        super(application);
        commentRepository = new CommentRepository(application);
        authManager = new AuthManager(application);
        executor = Executors.newFixedThreadPool(2);
    }

    /**
     * Load comments for a song
     */
    public LiveData<List<CommentWithUser>> loadComments(long songId) {
        this.currentSongId = songId;
        isLoading.setValue(true);
        errorMessage.setValue(null);

        MediatorLiveData<List<CommentWithUser>> result = new MediatorLiveData<>();
        LiveData<List<CommentWithUser>> commentsLiveData = commentRepository.getCommentsWithUserData(songId);

        result.addSource(commentsLiveData, comments -> {
            isLoading.setValue(false);
            result.setValue(comments);
        });

        return result;
    }

    /**
     * Add a new comment
     */
    public void addComment(String content) {
        if (content == null || content.trim().isEmpty()) {
            errorMessage.setValue("Comment cannot be empty");
            return;
        }

        if (!authManager.isLoggedIn()) {
            errorMessage.setValue("You must be logged in to comment");
            return;
        }

        if (currentSongId <= 0) {
            errorMessage.setValue("Invalid song selected");
            return;
        }

        isAddingComment.setValue(true);
        errorMessage.setValue(null); // Clear previous errors

        executor.execute(() -> {
            try {
                Long commentId = commentRepository.addComment(currentSongId, content).get();
                if (commentId != null && commentId > 0) {
                    successMessage.postValue("Comment added successfully");
                    android.util.Log.d("CommentsViewModel", "Comment added with ID: " + commentId);
                } else {
                    errorMessage.postValue("Failed to add comment");
                }
            } catch (IllegalStateException e) {
                errorMessage.postValue("Please log in to add comments");
            } catch (IllegalArgumentException e) {
                errorMessage.postValue("Invalid comment content");
            } catch (Exception e) {
                android.util.Log.e("CommentsViewModel", "Error adding comment", e);
                errorMessage.postValue("Unable to add comment. Please try again.");
            } finally {
                isAddingComment.postValue(false);
            }
        });
    }

    /**
     * Delete a comment (only if user owns it)
     */
    public void deleteComment(long commentId) {
        if (!authManager.isLoggedIn()) {
            errorMessage.setValue("You must be logged in to delete comments");
            return;
        }

        executor.execute(() -> {
            try {
                Boolean success = commentRepository.deleteComment(commentId).get();
                if (success) {
                    successMessage.postValue("Comment deleted successfully");
                } else {
                    errorMessage.postValue("You can only delete your own comments");
                }
            } catch (Exception e) {
                errorMessage.postValue("Error deleting comment: " + e.getMessage());
            }
        });
    }

    /**
     * Toggle like status for a comment
     */
    public void toggleCommentLike(long commentId) {
        if (!authManager.isLoggedIn()) {
            errorMessage.setValue("You must be logged in to like comments");
            return;
        }

        executor.execute(() -> {
            try {
                Boolean isNowLiked = commentRepository.toggleCommentLike(commentId).get();
                // Success message is optional for likes
            } catch (Exception e) {
                errorMessage.postValue("Error updating like: " + e.getMessage());
            }
        });
    }

    /**
     * Check if current user can delete a comment
     */
    public void checkCanDeleteComment(long commentId, CommentPermissionCallback callback) {
        executor.execute(() -> {
            try {
                Boolean canDelete = commentRepository.canDeleteComment(commentId).get();
                callback.onResult(canDelete);
            } catch (Exception e) {
                callback.onResult(false);
            }
        });
    }

    public interface CommentPermissionCallback {
        void onResult(boolean canDelete);
    }

    // LiveData getters
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public LiveData<Boolean> getIsAddingComment() {
        return isAddingComment;
    }

    public long getCurrentUserId() {
        return authManager.getCurrentUserId();
    }

    public boolean isLoggedIn() {
        return authManager.isLoggedIn();
    }

    /**
     * Clear error and success messages after they've been displayed
     */
    public void clearMessages() {
        errorMessage.setValue(null);
        successMessage.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (commentRepository != null) {
            commentRepository.shutdown();
        }
        if (executor != null) {
            executor.shutdown();
        }
    }
}
