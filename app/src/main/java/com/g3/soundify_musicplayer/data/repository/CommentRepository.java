package com.g3.soundify_musicplayer.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.g3.soundify_musicplayer.data.database.AppDatabase;
import com.g3.soundify_musicplayer.data.dao.CommentDao;
import com.g3.soundify_musicplayer.data.dao.CommentLikeDao;
import com.g3.soundify_musicplayer.data.dao.UserDao;
import com.g3.soundify_musicplayer.data.entity.Comment;
import com.g3.soundify_musicplayer.data.entity.CommentLike;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.ui.player.comment.CommentWithUser;
import com.g3.soundify_musicplayer.utils.AuthManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Repository for comment-related operations
 * Handles CRUD operations for comments and comment likes
 */
public class CommentRepository {
    
    private CommentDao commentDao;
    private CommentLikeDao commentLikeDao;
    private UserDao userDao;
    private AuthManager authManager;
    private ExecutorService executor;
    
    public CommentRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        commentDao = database.commentDao();
        commentLikeDao = database.commentLikeDao();
        userDao = database.userDao();
        authManager = new AuthManager(application);
        executor = Executors.newFixedThreadPool(4);
    }
    
    /**
     * Get comments for a song with user data and like status
     */
    public LiveData<List<CommentWithUser>> getCommentsWithUserData(long songId) {
        MediatorLiveData<List<CommentWithUser>> result = new MediatorLiveData<>();
        
        LiveData<List<Comment>> commentsLiveData = commentDao.getCommentsBySong(songId);
        result.addSource(commentsLiveData, comments -> {
            if (comments != null) {
                // Process comments in background thread
                executor.execute(() -> {
                    List<CommentWithUser> commentsWithUser = new ArrayList<>();
                    long currentUserId = authManager.getCurrentUserId();
                    
                    for (Comment comment : comments) {
                        try {
                            // Get user data
                            User user = userDao.getUserByIdSync(comment.getUserId());
                            if (user != null) {
                                // Get like status and count
                                boolean isLiked = currentUserId != -1 && 
                                    commentLikeDao.isCommentLikedByUser(comment.getId(), currentUserId) > 0;
                                int likeCount = commentLikeDao.getLikeCountForComment(comment.getId());
                                
                                CommentWithUser commentWithUser = new CommentWithUser(comment, user, isLiked, likeCount);
                                commentsWithUser.add(commentWithUser);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("CommentRepository", "Error processing comment: " + comment.getId(), e);
                        }
                    }
                    
                    result.postValue(commentsWithUser);
                });
            }
        });
        
        return result;
    }
    
    /**
     * Add a new comment
     */
    public Future<Long> addComment(long songId, String content) {
        return executor.submit(() -> {
            long currentUserId = authManager.getCurrentUserId();
            if (currentUserId == -1) {
                throw new IllegalStateException("User not logged in");
            }
            
            if (content == null || content.trim().isEmpty()) {
                throw new IllegalArgumentException("Comment content cannot be empty");
            }
            
            Comment comment = new Comment(songId, currentUserId, content.trim());
            return commentDao.insert(comment);
        });
    }
    
    /**
     * Delete a comment (only if user owns it)
     */
    public Future<Boolean> deleteComment(long commentId) {
        return executor.submit(() -> {
            try {
                long currentUserId = authManager.getCurrentUserId();
                if (currentUserId == -1) {
                    android.util.Log.w("CommentRepository", "User not logged in, cannot delete comment");
                    return false;
                }

                Comment comment = commentDao.getCommentByIdSync(commentId);
                if (comment == null) {
                    android.util.Log.w("CommentRepository", "Comment not found: " + commentId);
                    return false;
                }

                if (comment.getUserId() != currentUserId) {
                    android.util.Log.w("CommentRepository", "User " + currentUserId + " cannot delete comment owned by " + comment.getUserId());
                    return false;
                }

                // Delete associated likes first to maintain referential integrity
                commentLikeDao.deleteAllLikesForComment(commentId);

                // Then delete the comment
                commentDao.delete(comment);
                android.util.Log.d("CommentRepository", "Successfully deleted comment: " + commentId);
                return true;

            } catch (Exception e) {
                android.util.Log.e("CommentRepository", "Error deleting comment: " + commentId, e);
                return false;
            }
        });
    }
    
    /**
     * Toggle like status for a comment
     */
    public Future<Boolean> toggleCommentLike(long commentId) {
        return executor.submit(() -> {
            try {
                long currentUserId = authManager.getCurrentUserId();
                if (currentUserId == -1) {
                    android.util.Log.w("CommentRepository", "User not logged in, cannot toggle like");
                    return false;
                }

                // Verify comment exists
                Comment comment = commentDao.getCommentByIdSync(commentId);
                if (comment == null) {
                    android.util.Log.w("CommentRepository", "Comment not found: " + commentId);
                    return false;
                }

                boolean isCurrentlyLiked = commentLikeDao.isCommentLikedByUser(commentId, currentUserId) > 0;
                if (isCurrentlyLiked) {
                    commentLikeDao.unlikeComment(commentId, currentUserId);
                    android.util.Log.d("CommentRepository", "Unliked comment: " + commentId);
                    return false;
                } else {
                    CommentLike commentLike = new CommentLike(commentId, currentUserId);
                    commentLikeDao.insert(commentLike);
                    android.util.Log.d("CommentRepository", "Liked comment: " + commentId);
                    return true;
                }

            } catch (Exception e) {
                android.util.Log.e("CommentRepository", "Error toggling comment like: " + commentId, e);
                return false;
            }
        });
    }
    
    /**
     * Check if current user can delete a comment
     */
    public Future<Boolean> canDeleteComment(long commentId) {
        return executor.submit(() -> {
            long currentUserId = authManager.getCurrentUserId();
            if (currentUserId == -1) {
                return false;
            }
            
            Comment comment = commentDao.getCommentByIdSync(commentId);
            return comment != null && comment.getUserId() == currentUserId;
        });
    }
    
    /**
     * Get comment count for a song
     */
    public Future<Integer> getCommentCount(long songId) {
        return executor.submit(() -> commentDao.getCommentCountBySong(songId));
    }
    
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}
