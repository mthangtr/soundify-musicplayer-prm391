package com.g3.soundify_musicplayer.ui.profile;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.utils.TimeUtils;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for public songs in user profile
 */
public class PublicSongAdapter extends RecyclerView.Adapter<PublicSongAdapter.SongViewHolder> {
    
    private List<Song> songs = new ArrayList<>();
    private final Context context;
    private OnSongClickListener listener;
    private boolean showMenu = false;
    
    public interface OnSongClickListener {
        void onSongClick(Song song, int position);
        void onSongMenuClick(Song song, int position);
    }
    
    public PublicSongAdapter(Context context) {
        this.context = context;
    }
    
    public void setOnSongClickListener(OnSongClickListener listener) {
        this.listener = listener;
    }
    
    public void setShowMenu(boolean showMenu) {
        this.showMenu = showMenu;
        notifyDataSetChanged();
    }
    
    public void setSongs(List<Song> songs) {
        this.songs = songs != null ? new ArrayList<>(songs) : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public List<Song> getSongs() {
        return new ArrayList<>(songs);
    }
    
    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_public_song, parent, false);
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
        
        private final ShapeableImageView songCover;
        private final TextView songTitle;
        private final TextView songInfo;
        private final TextView playCount;
        private final TextView duration;
        private final ImageButton menuButton;
        
        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            
            songCover = itemView.findViewById(R.id.image_view_song_cover);
            songTitle = itemView.findViewById(R.id.text_view_song_title);
            songInfo = itemView.findViewById(R.id.text_view_song_info);
            playCount = itemView.findViewById(R.id.text_view_play_count);
            duration = itemView.findViewById(R.id.text_view_duration);
            menuButton = itemView.findViewById(R.id.button_song_menu);
            
            // Set up click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSongClick(songs.get(position), position);
                }
            });
            
            menuButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSongMenuClick(songs.get(position), position);
                }
            });
        }
        
        public void bind(Song song, int position) {
            // Set song title
            songTitle.setText(song.getTitle());
            
            // Set song info (genre + upload date)
            StringBuilder infoText = new StringBuilder();
            if (song.getGenre() != null && !song.getGenre().isEmpty()) {
                infoText.append(song.getGenre());
            }
            
            // Add upload date
            if (song.getCreatedAt() > 0) {
                if (infoText.length() > 0) infoText.append(" • ");
                String dateStr = formatUploadDate(song.getCreatedAt());
                infoText.append(dateStr);
            }
            
            if (infoText.length() > 0) {
                songInfo.setText(infoText.toString());
                songInfo.setVisibility(View.VISIBLE);
            } else {
                songInfo.setVisibility(View.GONE);
            }
            
            // Set play count (placeholder for now)
            // TODO: Implement play count tracking
            playCount.setVisibility(View.GONE);
            
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
            
            // Show/hide menu button
            menuButton.setVisibility(showMenu ? View.VISIBLE : View.GONE);
        }
        
        private String formatUploadDate(long timestamp) {
            try {
                Date uploadDate = new Date(timestamp);
                Date now = new Date();
                long diffMs = now.getTime() - uploadDate.getTime();
                long diffDays = diffMs / (24 * 60 * 60 * 1000);
                
                if (diffDays == 0) {
                    return "Today";
                } else if (diffDays == 1) {
                    return "Yesterday";
                } else if (diffDays < 7) {
                    return diffDays + " days ago";
                } else if (diffDays < 30) {
                    long weeks = diffDays / 7;
                    return weeks == 1 ? "1 week ago" : weeks + " weeks ago";
                } else if (diffDays < 365) {
                    long months = diffDays / 30;
                    return months == 1 ? "1 month ago" : months + " months ago";
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                    return sdf.format(uploadDate);
                }
            } catch (Exception e) {
                return "Recently";
            }
        }
    }
}
