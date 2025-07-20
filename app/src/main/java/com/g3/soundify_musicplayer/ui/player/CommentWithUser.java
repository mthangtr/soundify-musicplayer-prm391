package com.g3.soundify_musicplayer.ui.player;

import com.g3.soundify_musicplayer.data.entity.Comment;
import com.g3.soundify_musicplayer.data.entity.User;

/**
 * Model class that combines Comment and User data for display in the comments list.
 * This is used for UI purposes to show comment content along with user information.
 */
public class CommentWithUser {
    private Comment comment;
    private User user;
    private boolean isLiked;
    private int likeCount;

    public CommentWithUser() {}

    public CommentWithUser(Comment comment, User user) {
        this.comment = comment;
        this.user = user;
        this.isLiked = false;
        this.likeCount = 0;
    }

    public CommentWithUser(Comment comment, User user, boolean isLiked, int likeCount) {
        this.comment = comment;
        this.user = user;
        this.isLiked = isLiked;
        this.likeCount = likeCount;
    }

    // Getters and Setters
    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    // Convenience methods
    public long getCommentId() {
        return comment != null ? comment.getId() : 0;
    }

    public String getContent() {
        return comment != null ? comment.getContent() : "";
    }

    public long getCreatedAt() {
        return comment != null ? comment.getCreatedAt() : 0;
    }

    public String getUsername() {
        return user != null ? user.getUsername() : "";
    }

    public String getDisplayName() {
        return user != null ? user.getDisplayName() : "";
    }

    public String getAvatarUrl() {
        return user != null ? user.getAvatarUrl() : "";
    }

    public long getUserId() {
        return user != null ? user.getId() : 0;
    }
}
