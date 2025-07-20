package com.g3.soundify_musicplayer.ui.profile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying users in followers/following lists
 */
public class UserFollowAdapter extends RecyclerView.Adapter<UserFollowAdapter.UserViewHolder> {

    private Context context;
    private List<User> users;
    private List<User> filteredUsers;
    private List<Long> followingIds; // IDs of users that current user is following
    private OnUserActionListener listener;
    private long currentUserId;

    public interface OnUserActionListener {
        void onUserClick(User user, int position);
        void onFollowClick(User user, int position, boolean isCurrentlyFollowing);
    }

    public UserFollowAdapter(Context context, long currentUserId) {
        this.context = context;
        this.currentUserId = currentUserId;
        this.users = new ArrayList<>();
        this.filteredUsers = new ArrayList<>();
        this.followingIds = new ArrayList<>();
    }

    public void setOnUserActionListener(OnUserActionListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<User> users) {
        this.users = users != null ? new ArrayList<>(users) : new ArrayList<>();
        this.filteredUsers = new ArrayList<>(this.users);
        notifyDataSetChanged();
    }

    public void setFollowingIds(List<Long> followingIds) {
        this.followingIds = followingIds != null ? new ArrayList<>(followingIds) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void filterUsers(String query) {
        filteredUsers.clear();
        
        if (query == null || query.trim().isEmpty()) {
            filteredUsers.addAll(users);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (User user : users) {
                if (matchesQuery(user, lowerCaseQuery)) {
                    filteredUsers.add(user);
                }
            }
        }
        
        notifyDataSetChanged();
    }

    private boolean matchesQuery(User user, String query) {
        if (user.getDisplayName() != null && 
            user.getDisplayName().toLowerCase().contains(query)) {
            return true;
        }
        
        if (user.getUsername() != null && 
            user.getUsername().toLowerCase().contains(query)) {
            return true;
        }
        
        if (user.getBio() != null && 
            user.getBio().toLowerCase().contains(query)) {
            return true;
        }
        
        return false;
    }

    public void updateFollowStatus(long userId, boolean isFollowing) {
        if (isFollowing) {
            if (!followingIds.contains(userId)) {
                followingIds.add(userId);
            }
        } else {
            followingIds.remove(userId);
        }
        
        // Find and update the specific item
        for (int i = 0; i < filteredUsers.size(); i++) {
            if (filteredUsers.get(i).getId() == userId) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_follow, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = filteredUsers.get(position);
        holder.bind(user, position);
    }

    @Override
    public int getItemCount() {
        return filteredUsers.size();
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {
        
        private ShapeableImageView imageUserAvatar;
        private TextView textDisplayName;
        private TextView textUsername;
        private TextView textBio;
        private MaterialButton buttonFollow;
        private MaterialButton buttonFollowing;
        private TextView textMutualFollow;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            
            imageUserAvatar = itemView.findViewById(R.id.image_user_avatar);
            textDisplayName = itemView.findViewById(R.id.text_display_name);
            textUsername = itemView.findViewById(R.id.text_username);
            textBio = itemView.findViewById(R.id.text_bio);
            buttonFollow = itemView.findViewById(R.id.button_follow);
            buttonFollowing = itemView.findViewById(R.id.button_following);
            textMutualFollow = itemView.findViewById(R.id.text_mutual_follow);
        }

        public void bind(User user, int position) {
            // Set user info
            textDisplayName.setText(user.getDisplayName() != null ? 
                user.getDisplayName() : user.getUsername());
            textUsername.setText("@" + user.getUsername());
            
            // Set bio if available
            if (user.getBio() != null && !user.getBio().trim().isEmpty()) {
                textBio.setText(user.getBio());
                textBio.setVisibility(View.VISIBLE);
            } else {
                textBio.setVisibility(View.GONE);
            }
            
            // Load avatar
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                File avatarFile = new File(user.getAvatarUrl());
                if (avatarFile.exists()) {
                    Glide.with(context)
                        .load(avatarFile)
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_avatar)
                        .into(imageUserAvatar);
                } else {
                    imageUserAvatar.setImageResource(R.drawable.placeholder_avatar);
                }
            } else {
                imageUserAvatar.setImageResource(R.drawable.placeholder_avatar);
            }
            
            // Handle follow button state
            boolean isCurrentlyFollowing = followingIds.contains(user.getId());
            boolean isCurrentUser = user.getId() == currentUserId;
            
            if (isCurrentUser) {
                // Hide follow buttons for current user
                buttonFollow.setVisibility(View.GONE);
                buttonFollowing.setVisibility(View.GONE);
            } else {
                if (isCurrentlyFollowing) {
                    buttonFollow.setVisibility(View.GONE);
                    buttonFollowing.setVisibility(View.VISIBLE);
                } else {
                    buttonFollow.setVisibility(View.VISIBLE);
                    buttonFollowing.setVisibility(View.GONE);
                }
            }
            
            // TODO: Implement mutual follow detection
            textMutualFollow.setVisibility(View.GONE);
            
            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user, position);
                }
            });
            
            buttonFollow.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFollowClick(user, position, false);
                }
            });
            
            buttonFollowing.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFollowClick(user, position, true);
                }
            });
        }
    }
}
