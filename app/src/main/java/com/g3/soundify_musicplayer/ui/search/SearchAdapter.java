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
import java.util.List;

/**
 * RecyclerView adapter for displaying search results.
 * Handles different types of search results: Songs, Artists, and Playlists.
 */
public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.SearchViewHolder> {

    private List<SearchResult> searchResults;
    private Context context;
    private OnSearchResultClickListener clickListener;

    public interface OnSearchResultClickListener {
        void onSongClick(SearchResult result);
        void onArtistClick(SearchResult result);
        void onPlaylistClick(SearchResult result);
        void onActionClick(SearchResult result);
    }

    public SearchAdapter(Context context) {
        this.context = context;
        this.searchResults = new ArrayList<>();
    }

    public void setSearchResults(List<SearchResult> results) {
        this.searchResults = results != null ? results : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnSearchResultClickListener(OnSearchResultClickListener listener) {
        this.clickListener = listener;
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
                    clickListener.onActionClick(result);
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
            imageTypeIcon.setImageResource(R.drawable.ic_person_add);
            
            // Set follow button (or person icon)
            btnAction.setImageResource(R.drawable.ic_person_add);
            btnAction.setContentDescription("Follow artist");
            btnAction.setColorFilter(context.getColor(R.color.accent_blue));
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
