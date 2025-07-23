package com.g3.soundify_musicplayer.ui.playlist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.dto.PlaylistWithSongCount;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying playlists with song counts in RecyclerView
 */
public class PlaylistWithSongCountAdapter extends RecyclerView.Adapter<PlaylistWithSongCountAdapter.PlaylistViewHolder> {
    
    private List<PlaylistWithSongCount> playlists;
    private OnPlaylistClickListener listener;
    private boolean showOwnerOptions = false; // Show edit/delete options for owner playlists
    private long currentUserId = -1; // Current user ID to check ownership

    public interface OnPlaylistClickListener {
        void onPlaylistClick(PlaylistWithSongCount playlistWithSongCount);
        void onPlayButtonClick(PlaylistWithSongCount playlistWithSongCount);
        void onEditPlaylist(PlaylistWithSongCount playlistWithSongCount); // New method for edit
        void onDeletePlaylist(PlaylistWithSongCount playlistWithSongCount); // New method for delete
    }
    
    public PlaylistWithSongCountAdapter(List<PlaylistWithSongCount> playlists, OnPlaylistClickListener listener) {
        this.playlists = new ArrayList<>(playlists != null ? playlists : new ArrayList<>());
        this.listener = listener;
    }

    /**
     * Set whether to show owner options (edit/delete) and current user ID
     */
    public void setOwnerOptions(boolean showOwnerOptions, long currentUserId) {
        if (this.showOwnerOptions != showOwnerOptions || this.currentUserId != currentUserId) {
            this.showOwnerOptions = showOwnerOptions;
            this.currentUserId = currentUserId;
            notifyDataSetChanged(); // Refresh to show/hide overflow buttons
        }
    }
    
    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        PlaylistWithSongCount playlistWithSongCount = playlists.get(position);
        holder.bind(playlistWithSongCount);
    }
    
    @Override
    public int getItemCount() {
        return playlists.size();
    }
    
    class PlaylistViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPlaylistName, tvSongCount, tvCreatedDate;
        private ImageView ivPlaylistCover, ivPlayButton;
        private ImageButton btnPlaylistOverflow;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlaylistName = itemView.findViewById(R.id.tvPlaylistName);
            tvSongCount = itemView.findViewById(R.id.tvSongCount);
            tvCreatedDate = itemView.findViewById(R.id.tvCreatedDate);
            ivPlaylistCover = itemView.findViewById(R.id.ivPlaylistCover);
            ivPlayButton = itemView.findViewById(R.id.ivPlayButton);
            btnPlaylistOverflow = itemView.findViewById(R.id.btnPlaylistOverflow);
        }
        
        public void bind(PlaylistWithSongCount playlistWithSongCount) {
            // Set playlist name
            tvPlaylistName.setText(playlistWithSongCount.getName());
            
            // Set song count with proper pluralization
            int songCount = playlistWithSongCount.getSongCount();
            String songCountText = songCount == 1 ? "1 song" : songCount + " songs";
            tvSongCount.setText(songCountText);
            
            // Format created date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateStr = sdf.format(new Date(playlistWithSongCount.getCreatedAt()));
            tvCreatedDate.setText("Created " + dateStr);
            
            // Set playlist cover (placeholder for now)
            ivPlaylistCover.setImageResource(R.drawable.placeholder_album_art);
            
            // Show/hide overflow button based on ownership
            boolean isOwner = showOwnerOptions && currentUserId != -1 && playlistWithSongCount.getOwnerId() == currentUserId;
            btnPlaylistOverflow.setVisibility(isOwner ? View.VISIBLE : View.GONE);

            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaylistClick(playlistWithSongCount);
                }
            });

            ivPlayButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlayButtonClick(playlistWithSongCount);
                }
            });

            // Set up overflow menu
            btnPlaylistOverflow.setOnClickListener(v -> {
                if (listener != null && isOwner) {
                    showOwnerMenu(v, playlistWithSongCount);
                }
            });

            // Disable play button if playlist is empty
            ivPlayButton.setEnabled(songCount > 0);
            ivPlayButton.setAlpha(songCount > 0 ? 1.0f : 0.5f);
        }
    }

    /**
     * Show owner menu with edit and delete options
     */
    private void showOwnerMenu(View anchor, PlaylistWithSongCount playlist) {
        PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.playlist_detail_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit_playlist) {
                if (listener != null) {
                    listener.onEditPlaylist(playlist);
                }
                return true;
            } else if (item.getItemId() == R.id.action_delete_playlist) {
                if (listener != null) {
                    listener.onDeletePlaylist(playlist);
                }
                return true;
            }
            return false;
        });

        popup.show();
    }

    /**
     * Update adapter data
     */
    public void updateData(List<PlaylistWithSongCount> newData) {
        playlists.clear();
        if (newData != null) {
            playlists.addAll(newData);
        }
        notifyDataSetChanged();
    }
    
    /**
     * Get playlist at position
     */
    public PlaylistWithSongCount getPlaylistAt(int position) {
        if (position >= 0 && position < playlists.size()) {
            return playlists.get(position);
        }
        return null;
    }
    
    /**
     * Get all playlists
     */
    public List<PlaylistWithSongCount> getAllPlaylists() {
        return new ArrayList<>(playlists);
    }
}
