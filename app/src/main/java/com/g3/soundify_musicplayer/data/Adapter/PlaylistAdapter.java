package com.g3.soundify_musicplayer.data.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Playlist;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {
    
    private List<Playlist> playlists;
    private OnPlaylistClickListener listener;
    
    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
        void onPlayButtonClick(Playlist playlist);
    }
    
    public PlaylistAdapter(List<Playlist> playlists, OnPlaylistClickListener listener) {
        this.playlists = playlists;
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
        Playlist playlist = playlists.get(position);
        holder.bind(playlist);
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
        
        public void bind(Playlist playlist) {
            tvPlaylistName.setText(playlist.getName());
            tvSongCount.setText("0 songs"); // Hardcoded for now
            
            // Format created date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateStr = sdf.format(new Date(playlist.getCreatedAt()));
            tvCreatedDate.setText("Created " + dateStr);
            
            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlaylistClick(playlist);
                }
            });
            
            ivPlayButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlayButtonClick(playlist);
                }
            });
        }
    }

    public void updateData(List<Playlist> newData) {
        playlists.clear();
        if (newData != null) {
            playlists.addAll(newData);
        }
        notifyDataSetChanged();
    }
}