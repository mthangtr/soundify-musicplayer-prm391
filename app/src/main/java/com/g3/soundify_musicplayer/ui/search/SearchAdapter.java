package com.g3.soundify_musicplayer.ui.search;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RecyclerView adapter for displaying search results.
 * Handles different types of search results: Songs, Artists, and Playlists.
 */
public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {

    private List<SearchResult> searchResults;
    private Context context;
    private OnSearchResultClickListener clickListener;
    private Set<Long> followingUserIds;
    private Set<Long> loadingUserIds;
    private long currentUserId;

    public interface OnSearchResultClickListener {
        void onSongClick(SearchResult result);
        void onArtistClick(SearchResult result);
        void onPlaylistClick(SearchResult result);
        void onActionClick(SearchResult result);
        void onFollowClick(SearchResult result, boolean isCurrentlyFollowing);
    }

    public SearchAdapter(Context context) {
        this.context = context;
        this.searchResults = new ArrayList<>();
        this.followingUserIds = new HashSet<>();
        this.loadingUserIds = new HashSet<>();
        this.currentUserId = -1;
    }

    public void setSearchResults(List<SearchResult> results) {
        this.searchResults = results != null ? results : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnSearchResultClickListener(OnSearchResultClickListener listener) {
        this.clickListener = listener;
    }

    public void setFollowingUserIds(Set<Long> followingIds) {
        this.followingUserIds = followingIds != null ? followingIds : new HashSet<>();
        notifyDataSetChanged();
    }

    public void setCurrentUserId(long userId) {
        this.currentUserId = userId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_result, parent, false);
        return new SearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        SearchResult result = searchResults.get(position);
        holder.bind(result);
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    class SearchViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageItem;
        private ImageView imageTypeIcon;
        private TextView textPrimary;
        private TextView textSecondary;
        private TextView textTertiary;
        private ImageButton btnAction;

        public SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            imageItem = itemView.findViewById(R.id.image_item);
            imageTypeIcon = itemView.findViewById(R.id.image_type_icon);
            textPrimary = itemView.findViewById(R.id.text_primary);
            textSecondary = itemView.findViewById(R.id.text_secondary);
            textTertiary = itemView.findViewById(R.id.text_tertiary);
            btnAction = itemView.findViewById(R.id.btn_action);
        }

        public void bind(SearchResult result) {
            // Set text content
            textPrimary.setText(result.getPrimaryText());
            textSecondary.setText(result.getSecondaryText());
            
            // Set tertiary text visibility and content
            if (result.getTertiaryText() != null && !result.getTertiaryText().isEmpty()) {
                textTertiary.setVisibility(View.VISIBLE);
                textTertiary.setText(result.getTertiaryText());
            } else {
                textTertiary.setVisibility(View.GONE);
            }

            // Configure UI based on result type
            switch (result.getType()) {
                case SONG:
                    setupSongResult(result);
                    break;
                case ARTIST:
                    setupArtistResult(result);
                    break;
                case PLAYLIST:
                    setupPlaylistResult(result);
                    break;
            }

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    switch (result.getType()) {
                        case SONG:
                            clickListener.onSongClick(result);
                            break;
                        case ARTIST:
                            clickListener.onArtistClick(result);
                            break;
                        case PLAYLIST:
                            clickListener.onPlaylistClick(result);
                            break;
                    }
                }
            });

            btnAction.setOnClickListener(v -> {
                if (clickListener != null) {
                    if (result.getType() == SearchResult.Type.ARTIST && result.getUser() != null) {
                        // Handle follow/unfollow for artists
                        long userId = result.getUser().getId();
                        boolean isCurrentlyFollowing = followingUserIds.contains(userId);
                        boolean isLoading = loadingUserIds.contains(userId);

                        if (!isLoading && userId != currentUserId) {
                            clickListener.onFollowClick(result, isCurrentlyFollowing);
                        }
                    } else {
                        // Handle other actions (play, etc.)
                        clickListener.onActionClick(result);
                    }
                }
            });
        }

        private void setupSongResult(SearchResult result) {
            // Set song cover art or placeholder
            imageItem.setImageResource(R.drawable.placeholder_album_art);
            
            // Hide type icon for songs (default type)
            imageTypeIcon.setVisibility(View.GONE);
            
            // Set play button
            btnAction.setImageResource(R.drawable.ic_play);
            btnAction.setContentDescription(context.getString(R.string.cd_mini_player_play_pause));
            btnAction.setColorFilter(context.getColor(R.color.player_control_active));
        }

        private void setupArtistResult(SearchResult result) {
            // Set user avatar or placeholder
            imageItem.setImageResource(R.drawable.placeholder_avatar);

            // Show person icon to indicate this is an artist
            imageTypeIcon.setVisibility(View.VISIBLE);
            imageTypeIcon.setImageResource(R.drawable.ic_person);

            // Configure follow button based on current state
            if (result.getUser() != null) {
                long userId = result.getUser().getId();
                boolean isCurrentUser = userId == currentUserId;
                boolean isFollowing = followingUserIds.contains(userId);
                boolean isLoading = loadingUserIds.contains(userId);

                if (isCurrentUser) {
                    // Hide action button for current user
                    btnAction.setVisibility(View.GONE);
                } else {
                    btnAction.setVisibility(View.VISIBLE);

                    if (isLoading) {
                        // Show loading state
                        btnAction.setImageResource(R.drawable.ic_loading);
                        btnAction.setContentDescription("Loading...");
                        btnAction.setColorFilter(context.getColor(R.color.text_secondary));
                        btnAction.setEnabled(false);
                    } else if (isFollowing) {
                        // Show following state
                        btnAction.setImageResource(R.drawable.ic_person_check);
                        btnAction.setContentDescription("Unfollow artist");
                        btnAction.setColorFilter(context.getColor(R.color.accent_blue));
                        btnAction.setEnabled(true);
                    } else {
                        // Show follow state
                        btnAction.setImageResource(R.drawable.ic_person_add);
                        btnAction.setContentDescription("Follow artist");
                        btnAction.setColorFilter(context.getColor(R.color.accent_blue));
                        btnAction.setEnabled(true);
                    }
                }
            } else {
                btnAction.setVisibility(View.GONE);
            }
        }

        private void setupPlaylistResult(SearchResult result) {
            // Set playlist icon (no cover art for playlists in this implementation)
            imageItem.setImageResource(R.drawable.ic_library);
            imageItem.setScaleType(ImageView.ScaleType.CENTER);
            imageItem.setColorFilter(context.getColor(R.color.text_secondary));
            
            // Show playlist icon to indicate this is a playlist
            imageTypeIcon.setVisibility(View.VISIBLE);
            imageTypeIcon.setImageResource(R.drawable.ic_library);
            
            // Set play button for playlist
            btnAction.setImageResource(R.drawable.ic_play);
            btnAction.setContentDescription("Play playlist");
            btnAction.setColorFilter(context.getColor(R.color.player_control_active));
        }
    }
}
