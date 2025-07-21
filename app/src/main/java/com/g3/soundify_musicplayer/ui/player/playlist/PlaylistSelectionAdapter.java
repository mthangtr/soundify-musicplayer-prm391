package com.g3.soundify_musicplayer.ui.player.playlist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Playlist;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying playlists in selection mode
 * Clean, Apple-like design with generous whitespace
 */
public class PlaylistSelectionAdapter extends RecyclerView.Adapter<PlaylistSelectionAdapter.PlaylistViewHolder> {

    private final Context context;
    private final List<Playlist> playlists;
    private OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public PlaylistSelectionAdapter(Context context) {
        this.context = context;
        this.playlists = new ArrayList<>();
    }

    public void setOnPlaylistClickListener(OnPlaylistClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist_selection, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.bind(playlist);
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public void updateData(List<Playlist> newData) {
        playlists.clear();
        if (newData != null) {
            playlists.addAll(newData);
        }
        notifyDataSetChanged();
    }

    class PlaylistViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivPlaylistCover;
        private final TextView tvPlaylistName;
        private final TextView tvPlaylistDescription;
        private final TextView tvSongCount;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPlaylistCover = itemView.findViewById(R.id.iv_playlist_cover);
            tvPlaylistName = itemView.findViewById(R.id.tv_playlist_name);
            tvPlaylistDescription = itemView.findViewById(R.id.tv_playlist_description);
            tvSongCount = itemView.findViewById(R.id.tv_song_count);
            ImageView ivArrow = itemView.findViewById(R.id.iv_arrow);
        }

        public void bind(Playlist playlist) {
            tvPlaylistName.setText(playlist.getName());
            tvPlaylistDescription.setText(playlist.getDescription());
            
            // Mock song count (in real app this would come from repository)
            int mockSongCount = (int) (Math.random() * 50) + 1;
            String songCountText = mockSongCount == 1 ? "1 song" : mockSongCount + " songs";
            tvSongCount.setText(songCountText);
            
            // Set placeholder cover art
            ivPlaylistCover.setImageResource(R.drawable.placeholder_album_art);
            
            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaylistClick(playlist);
                }
            });
            
            // Add subtle press effect
            itemView.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        v.setAlpha(0.7f);
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        v.setAlpha(1.0f);
                        break;
                }
                return false;
            });
        }
    }
}
