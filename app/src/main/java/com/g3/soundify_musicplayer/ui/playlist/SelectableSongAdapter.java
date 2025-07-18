package com.g3.soundify_musicplayer.ui.playlist;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.utils.TimeUtils;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * RecyclerView Adapter for selectable songs with checkbox support
 */
public class SelectableSongAdapter extends RecyclerView.Adapter<SelectableSongAdapter.SongViewHolder> {
    
    private List<Song> songs = new ArrayList<>();
    private Set<Long> selectedSongIds;
    private final Context context;
    private OnSongSelectionListener listener;
    
    public interface OnSongSelectionListener {
        void onSongSelectionChanged(long songId, boolean isSelected);
        void onSongClick(Song song, int position);
    }
    
    public SelectableSongAdapter(Context context) {
        this.context = context;
    }
    
    public void setOnSongSelectionListener(OnSongSelectionListener listener) {
        this.listener = listener;
    }
    
    public void setSongs(List<Song> songs) {
        this.songs = songs != null ? new ArrayList<>(songs) : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void setSelectedSongIds(Set<Long> selectedSongIds) {
        this.selectedSongIds = selectedSongIds;
        notifyDataSetChanged();
    }
    
    public List<Song> getSongs() {
        return new ArrayList<>(songs);
    }
    
    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_selectable_song, parent, false);
        return new SongViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.bind(song, position);
    }
    
    @Override
    public int getItemCount() {
        return songs.size();
    }
    
    class SongViewHolder extends RecyclerView.ViewHolder {
        
        private final MaterialCheckBox checkbox;
        private final ShapeableImageView songCover;
        private final TextView songTitle;
        private final TextView artistName;
        private final TextView additionalInfo;
        private final TextView duration;
        private final View selectionOverlay;
        
        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            
            checkbox = itemView.findViewById(R.id.checkbox_select);
            songCover = itemView.findViewById(R.id.image_view_song_cover);
            songTitle = itemView.findViewById(R.id.text_view_song_title);
            artistName = itemView.findViewById(R.id.text_view_artist_name);
            additionalInfo = itemView.findViewById(R.id.text_view_additional_info);
            duration = itemView.findViewById(R.id.text_view_duration);
            selectionOverlay = itemView.findViewById(R.id.view_selection_overlay);
            
            // Set up click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Song song = songs.get(position);
                    toggleSelection(song.getId());
                    
                    if (listener != null) {
                        listener.onSongClick(song, position);
                    }
                }
            });
            
            checkbox.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Song song = songs.get(position);
                    toggleSelection(song.getId());
                }
            });
        }
        
        public void bind(Song song, int position) {
            // Set song title
            songTitle.setText(song.getTitle());
            
            // Set artist name (for now show "Unknown Artist")
            // In a real implementation, you would load this from the User table
            artistName.setText("Unknown Artist");
            
            // Set additional info (genre, upload date, etc.)
            StringBuilder additionalText = new StringBuilder();
            if (song.getGenre() != null && !song.getGenre().isEmpty()) {
                additionalText.append(song.getGenre());
            }
            
            if (song.isPublic()) {
                if (additionalText.length() > 0) additionalText.append(" • ");
                additionalText.append("Public");
            } else {
                if (additionalText.length() > 0) additionalText.append(" • ");
                additionalText.append("Private");
            }
            
            if (additionalText.length() > 0) {
                additionalInfo.setText(additionalText.toString());
                additionalInfo.setVisibility(View.VISIBLE);
            } else {
                additionalInfo.setVisibility(View.GONE);
            }
            
            // Set duration
            if (song.getDurationMs() != null && song.getDurationMs() > 0) {
                duration.setText(TimeUtils.formatDuration(song.getDurationMs()));
            } else {
                duration.setText("--:--");
            }
            
            // Set cover art
            if (song.getCoverArtUrl() != null && !song.getCoverArtUrl().isEmpty()) {
                try {
                    Uri coverUri = Uri.parse(song.getCoverArtUrl());
                    songCover.setImageURI(coverUri);
                } catch (Exception e) {
                    songCover.setImageResource(R.drawable.placeholder_album_art);
                }
            } else {
                songCover.setImageResource(R.drawable.placeholder_album_art);
            }
            
            // Set selection state
            boolean isSelected = selectedSongIds != null && selectedSongIds.contains(song.getId());
            checkbox.setChecked(isSelected);
            selectionOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            
            // Update item appearance based on selection
            float alpha = isSelected ? 0.7f : 1.0f;
            songTitle.setAlpha(alpha);
            artistName.setAlpha(alpha);
            additionalInfo.setAlpha(alpha);
            duration.setAlpha(alpha);
        }
        
        private void toggleSelection(long songId) {
            if (listener != null) {
                boolean isCurrentlySelected = selectedSongIds != null && selectedSongIds.contains(songId);
                listener.onSongSelectionChanged(songId, !isCurrentlySelected);
            }
        }
    }
}
