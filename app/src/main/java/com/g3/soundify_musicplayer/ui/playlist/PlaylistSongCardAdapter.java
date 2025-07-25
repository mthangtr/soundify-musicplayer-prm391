package com.g3.soundify_musicplayer.ui.playlist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Song;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for playlist songs using item_song_card.xml layout
 * This adapter displays songs in a playlist with the standard song card layout
 */
public class PlaylistSongCardAdapter extends RecyclerView.Adapter<PlaylistSongCardAdapter.SongCardViewHolder> {
    
    private List<Song> songs = new ArrayList<>();
    private final Context context;
    private OnSongActionListener listener;
    private boolean showRemoveOption = false; // Only show for playlist owners

    public interface OnSongActionListener {
        void onSongClick(Song song, int position);
        void onPlaySong(Song song, int position);
        void onRemoveFromPlaylist(Song song, int position); // New method for remove action
    }
    
    public PlaylistSongCardAdapter(Context context) {
        this.context = context;
    }
    
    public void setOnSongActionListener(OnSongActionListener listener) {
        this.listener = listener;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs != null ? new ArrayList<>(songs) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<Song> getSongs() {
        return new ArrayList<>(songs);
    }

    /**
     * Set whether to show remove option (only for playlist owners)
     */
    public void setShowRemoveOption(boolean showRemoveOption) {
        if (this.showRemoveOption != showRemoveOption) {
            this.showRemoveOption = showRemoveOption;
            notifyDataSetChanged(); // Refresh to show/hide overflow buttons
        }
    }
    
    @NonNull
    @Override
    public SongCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song_card, parent, false);
        return new SongCardViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SongCardViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.bind(song, position);
    }
    
    @Override
    public int getItemCount() {
        return songs.size();
    }
    
    class SongCardViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imgCover;
        private final TextView tvTitle;
        private final TextView tvUploader;
        private final ImageButton btnPlay;
        private final ImageButton btnOverflow;

        public SongCardViewHolder(@NonNull View itemView) {
            super(itemView);

            imgCover = itemView.findViewById(R.id.imgCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvUploader = itemView.findViewById(R.id.tvUploader);
            btnPlay = itemView.findViewById(R.id.btnPlay);
            btnOverflow = itemView.findViewById(R.id.btnOverflow);

            // Set up click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSongClick(songs.get(position), position);
                }
            });

            btnPlay.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onPlaySong(songs.get(position), position);
                }
            });

            // Set up overflow menu
            btnOverflow.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    showOverflowMenu(v, songs.get(position), position);
                }
            });
        }

        /**
         * Show overflow menu with remove option
         */
        private void showOverflowMenu(View anchor, Song song, int position) {
            PopupMenu popup = new PopupMenu(context, anchor);
            popup.getMenuInflater().inflate(R.menu.playlist_song_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_remove_from_playlist) {
                    if (listener != null) {
                        listener.onRemoveFromPlaylist(song, position);
                    }
                    return true;
                }
                return false;
            });

            popup.show();
        }
        
        public void bind(Song song, int position) {
            // Set song title
            tvTitle.setText(song.getTitle() != null ? song.getTitle() : "Unknown Title");

            // Set uploader name with fallback handling
            String uploaderName = song.getUploaderName();
            if (uploaderName != null && !uploaderName.trim().isEmpty()) {
                tvUploader.setText(uploaderName);
            } else {
                // Fallback: Try to get uploader info manually
                loadUploaderNameManually(song, tvUploader);
            }

            // Set cover art using Glide with better error handling
            if (song.getCoverArtUrl() != null && !song.getCoverArtUrl().trim().isEmpty()) {
                Glide.with(context)
                        .load(song.getCoverArtUrl())
                        .placeholder(R.drawable.splashi_icon)
                        .error(R.drawable.splashi_icon)
                        .into(imgCover);
            } else {
                imgCover.setImageResource(R.drawable.splashi_icon);
            }

            // Show/hide overflow button based on owner status
            btnOverflow.setVisibility(showRemoveOption ? View.VISIBLE : View.GONE);
        }

        /**
         * Manual fallback to load uploader name when query fails
         */
        private void loadUploaderNameManually(Song song, TextView tvUploader) {
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    com.g3.soundify_musicplayer.data.database.AppDatabase database =
                        com.g3.soundify_musicplayer.data.database.AppDatabase.getInstance(context);

                    com.g3.soundify_musicplayer.data.entity.User user =
                        database.userDao().getUserByIdSync(song.getUploaderId());

                    String uploaderName = "Unknown Artist";
                    if (user != null) {
                        if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                            uploaderName = user.getDisplayName();
                        } else if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
                            uploaderName = user.getUsername();
                        }
                    }

                    final String finalName = uploaderName;

                    // Update UI on main thread
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        tvUploader.setText(finalName);
                    });

                } catch (Exception e) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        tvUploader.setText("Unknown Artist");
                    });
                }
            });
        }
    }
}
