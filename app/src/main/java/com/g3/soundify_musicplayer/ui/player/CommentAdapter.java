package com.g3.soundify_musicplayer.ui.player;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying comments in the comments list.
 * Handles comment display, like button interactions, and time formatting.
 */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<CommentWithUser> comments;
    private Context context;
    private OnCommentLikeListener likeListener;

    public interface OnCommentLikeListener {
        void onCommentLike(CommentWithUser comment, int position);
    }

    public CommentAdapter(Context context) {
        this.context = context;
        this.comments = new ArrayList<>();
    }

    public void setComments(List<CommentWithUser> comments) {
        this.comments = comments != null ? comments : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnCommentLikeListener(OnCommentLikeListener listener) {
        this.likeListener = listener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        CommentWithUser commentWithUser = comments.get(position);
        holder.bind(commentWithUser, position);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageAvatar;
        private TextView textUsername;
        private TextView textTime;
        private TextView textComment;
        private ImageButton btnLike;
        private TextView textLikeCount;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            imageAvatar = itemView.findViewById(R.id.image_avatar);
            textUsername = itemView.findViewById(R.id.text_username);
            textTime = itemView.findViewById(R.id.text_time);
            textComment = itemView.findViewById(R.id.text_comment);
            btnLike = itemView.findViewById(R.id.btn_like);
            textLikeCount = itemView.findViewById(R.id.text_like_count);
        }

        public void bind(CommentWithUser commentWithUser, int position) {
            // Set user info
            String displayName = commentWithUser.getDisplayName();
            String username = commentWithUser.getUsername();
            textUsername.setText(displayName != null && !displayName.isEmpty() ? displayName : username);

            // Set comment content
            textComment.setText(commentWithUser.getContent());

            // Set time ago
            textTime.setText(formatTimeAgo(commentWithUser.getCreatedAt()));

            // Set like button state
            updateLikeButton(commentWithUser.isLiked());

            // Set like count
            updateLikeCount(commentWithUser.getLikeCount());

            // Set avatar (placeholder for now)
            imageAvatar.setImageResource(R.drawable.placeholder_avatar);

            // Set like button click listener
            btnLike.setOnClickListener(v -> {
                if (likeListener != null) {
                    likeListener.onCommentLike(commentWithUser, position);
                }
            });
        }

        private void updateLikeButton(boolean isLiked) {
            if (isLiked) {
                btnLike.setImageResource(R.drawable.ic_heart_filled);
                btnLike.setColorFilter(context.getColor(R.color.button_like_active));
            } else {
                btnLike.setImageResource(R.drawable.ic_heart);
                btnLike.setColorFilter(context.getColor(R.color.button_like_inactive));
            }
        }

        private void updateLikeCount(int likeCount) {
            if (likeCount > 0) {
                textLikeCount.setVisibility(View.VISIBLE);
                if (likeCount == 1) {
                    textLikeCount.setText(context.getString(R.string.comment_likes_count_single));
                } else {
                    textLikeCount.setText(context.getString(R.string.comment_likes_count, likeCount));
                }
            } else {
                textLikeCount.setVisibility(View.GONE);
            }
        }

        private String formatTimeAgo(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            long minutes = diff / (60 * 1000);
            long hours = diff / (60 * 60 * 1000);
            long days = diff / (24 * 60 * 60 * 1000);

            if (minutes < 1) {
                return context.getString(R.string.just_now);
            } else if (minutes < 60) {
                return context.getString(R.string.minutes_ago, (int) minutes);
            } else if (hours < 24) {
                return context.getString(R.string.hours_ago, (int) hours);
            } else {
                return context.getString(R.string.days_ago, (int) days);
            }
        }
    }
}
