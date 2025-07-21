package com.g3.soundify_musicplayer.ui.player.comment;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Comment;
import com.g3.soundify_musicplayer.data.entity.User;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for the Comments screen.
 * Provides mock data and handles like state management for demo purposes.
 * UI ONLY - No backend integration.
 */
public class CommentsViewModel extends AndroidViewModel {

    private final MutableLiveData<List<CommentWithUser>> comments = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private List<CommentWithUser> commentsList;
    private long currentSongId;

    public CommentsViewModel(@NonNull Application application) {
        super(application);
        commentsList = new ArrayList<>();
    }

    // Public methods for Fragment to call
    public void loadComments(long songId) {
        this.currentSongId = songId;
        isLoading.setValue(true);
        
        // Simulate loading delay
        new Thread(() -> {
            try {
                Thread.sleep(500); // Simulate network delay
                
                // Create mock comments
                List<CommentWithUser> mockComments = createMockComments(songId);
                commentsList = mockComments;
                
                // Update UI on main thread
                comments.postValue(mockComments);
                isLoading.postValue(false);
                
            } catch (InterruptedException e) {
                error.postValue("Error loading comments");
                isLoading.postValue(false);
            }
        }).start();
    }

    public void toggleCommentLike(CommentWithUser commentWithUser, int position) {
        if (commentsList != null && position >= 0 && position < commentsList.size()) {
            CommentWithUser comment = commentsList.get(position);
            
            // Toggle like state
            boolean newLikeState = !comment.isLiked();
            comment.setLiked(newLikeState);
            
            // Update like count
            int currentCount = comment.getLikeCount();
            if (newLikeState) {
                comment.setLikeCount(currentCount + 1);
            } else {
                comment.setLikeCount(Math.max(0, currentCount - 1));
            }
            
            // Notify observers
            comments.setValue(commentsList);
        }
    }

    // LiveData getters
    public LiveData<List<CommentWithUser>> getComments() {
        return comments;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    // Mock data creation methods
    private List<CommentWithUser> createMockComments(long songId) {
        List<CommentWithUser> mockComments = new ArrayList<>();
        
        // Create mock users
        User user1 = createMockUser(1L, "sarah_music", "Sarah Johnson");
        User user2 = createMockUser(2L, "mike_beats", "Mike Chen");
        User user3 = createMockUser(3L, "emma_sound", "Emma Davis");
        User user4 = createMockUser(4L, "alex_tunes", "Alex Rodriguez");
        User user5 = createMockUser(5L, "lisa_melody", "Lisa Kim");

        // Create mock comments with different timestamps
        long now = System.currentTimeMillis();
        
        Comment comment1 = createMockComment(1L, songId, 1L, 
            "This song is absolutely beautiful! The melody gives me chills every time I listen to it. 🎵", 
            now - (2 * 60 * 60 * 1000)); // 2 hours ago
        mockComments.add(new CommentWithUser(comment1, user1, true, 12));

        Comment comment2 = createMockComment(2L, songId, 2L, 
            "Amazing production quality! You can really hear every instrument clearly. Great work! 👏", 
            now - (45 * 60 * 1000)); // 45 minutes ago
        mockComments.add(new CommentWithUser(comment2, user2, false, 8));

        Comment comment3 = createMockComment(3L, songId, 3L, 
            "This is my new favorite song! Been playing it on repeat all day. Can't get enough! ❤️", 
            now - (30 * 60 * 1000)); // 30 minutes ago
        mockComments.add(new CommentWithUser(comment3, user3, true, 15));

        Comment comment4 = createMockComment(4L, songId, 4L, 
            "The lyrics are so meaningful and relatable. This really speaks to my soul.", 
            now - (15 * 60 * 1000)); // 15 minutes ago
        mockComments.add(new CommentWithUser(comment4, user4, false, 3));

        Comment comment5 = createMockComment(5L, songId, 5L, 
            "Perfect for my evening playlist! Such a relaxing and peaceful vibe. 🌅", 
            now - (5 * 60 * 1000)); // 5 minutes ago
        mockComments.add(new CommentWithUser(comment5, user5, false, 1));

        return mockComments;
    }

    private User createMockUser(long id, String username, String displayName) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setAvatarUrl("mock://avatar/" + username + ".jpg");
        user.setCreatedAt(System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)); // 30 days ago
        return user;
    }

    private Comment createMockComment(long id, long songId, long userId, String content, long createdAt) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setSongId(songId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setCreatedAt(createdAt);
        return comment;
    }
}
