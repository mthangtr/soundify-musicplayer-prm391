package com.g3.soundify_musicplayer.ui.playlist;

import android.content.Context;
import android.net.Uri;
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
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.utils.TimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RecyclerView Adapter for playlist songs with drag-and-drop support
 */
public class PlaylistSongAdapter extends RecyclerView.Adapter<PlaylistSongAdapter.SongViewHolder> {
    
    private List<Song> songs = new ArrayList<>();
    private final Context context;
    private OnSongActionListener listener;
    private boolean isOwner = false;
    
    public interface OnSongActionListener {
        void onSongClick(Song song, int position);
        void onRemoveSong(Song song, int position);
        void onMoveSong(int fromPosition, int toPosition);
    }
    
    public PlaylistSongAdapter(Context context) {
        this.context = context;
    }
    
    public void setOnSongActionListener(OnSongActionListener listener) {
        this.listener = listener;
    }
    
    public void setIsOwner(boolean isOwner) {
        this.isOwner = isOwner;
        notifyDataSetChanged();
    }
    
    public void setSongs(List<Song> songs) {
        this.songs = songs != null ? new ArrayList<>(songs) : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public List<Song> getSongs() {
        return new ArrayList<>(songs);
    }
    
    /**
     * Move item for drag and drop
     */
    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(songs, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(songs, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        
        if (listener != null) {
            listener.onMoveSong(fromPosition, toPosition);
        }
    }
    
    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist_song, parent, false);
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
        
        private final ImageView dragHandle;
        private final com.google.android.material.imageview.ShapeableImageView songCover;
        private final TextView songTitle;
        private final TextView artistName;
        private final TextView duration;
        private final ImageButton menuButton;
        
        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            
            dragHandle = itemView.findViewById(R.id.image_view_drag_handle);
            songCover = itemView.findViewById(R.id.image_view_song_cover);
            songTitle = itemView.findViewById(R.id.text_view_song_title);
            artistName = itemView.findViewById(R.id.text_view_artist_name);
            duration = itemView.findViewById(R.id.text_view_duration);
            menuButton = itemView.findViewById(R.id.button_song_menu);
            
            // Set up click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSongClick(songs.get(position), position);
                }
            });
            
            menuButton.setOnClickListener(v -> showSongMenu(v, getAdapterPosition()));
        }
        
        public void bind(Song song, int position) {
            // Set song title
            songTitle.setText(song.getTitle());
            
            // Set artist name - for now show "Unknown Artist"
            // In a real implementation, you would load this from the User table
            artistName.setText("Unknown Artist");
            
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
            
            // Show/hide drag handle based on ownership
            dragHandle.setVisibility(isOwner ? View.VISIBLE : View.GONE);
            
            // Show/hide menu button based on ownership
            menuButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        }
        
        private void showSongMenu(View anchor, int position) {
            if (position == RecyclerView.NO_POSITION || !isOwner) {
                return;
            }
            
            Song song = songs.get(position);
            
            PopupMenu popup = new PopupMenu(context, anchor);
            popup.getMenuInflater().inflate(R.menu.menu_playlist_song, popup.getMenu());
            
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_remove_song) {
                    if (listener != null) {
                        listener.onRemoveSong(song, position);
                    }
                    return true;
                } else if (itemId == R.id.action_move_up) {
                    if (position > 0) {
                        moveItem(position, position - 1);
                    }
                    return true;
                } else if (itemId == R.id.action_move_down) {
                    if (position < songs.size() - 1) {
                        moveItem(position, position + 1);
                    }
                    return true;
                }
                return false;
            });
            
            // Enable/disable move options based on position
            popup.getMenu().findItem(R.id.action_move_up).setEnabled(position > 0);
            popup.getMenu().findItem(R.id.action_move_down).setEnabled(position < songs.size() - 1);
            
            popup.show();
        }
    }
}
