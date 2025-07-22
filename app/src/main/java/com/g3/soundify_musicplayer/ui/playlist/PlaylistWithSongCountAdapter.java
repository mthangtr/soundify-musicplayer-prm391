package com.g3.soundify_musicplayer.ui.playlist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
    
    public interface OnPlaylistClickListener {
        void onPlaylistClick(PlaylistWithSongCount playlistWithSongCount);
        void onPlayButtonClick(PlaylistWithSongCount playlistWithSongCount);
    }
    
    public PlaylistWithSongCountAdapter(List<PlaylistWithSongCount> playlists, OnPlaylistClickListener listener) {
        this.playlists = new ArrayList<>(playlists != null ? playlists : new ArrayList<>());
        this.listener = listener;
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
        
        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlaylistName = itemView.findViewById(R.id.tvPlaylistName);
            tvSongCount = itemView.findViewById(R.id.tvSongCount);
            tvCreatedDate = itemView.findViewById(R.id.tvCreatedDate);
            ivPlaylistCover = itemView.findViewById(R.id.ivPlaylistCover);
            ivPlayButton = itemView.findViewById(R.id.ivPlayButton);
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
            
            // Disable play button if playlist is empty
            ivPlayButton.setEnabled(songCount > 0);
            ivPlayButton.setAlpha(songCount > 0 ? 1.0f : 0.5f);
        }
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
