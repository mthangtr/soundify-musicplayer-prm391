package com.g3.soundify_musicplayer.ui.song;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.dto.SongWithUploaderInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying recent songs with uploader information
 * Used in HomeFragment for recently played songs section
 */
public class RecentSongWithUploaderInfoAdapter extends RecyclerView.Adapter<RecentSongWithUploaderInfoAdapter.RecentSongVH> {

    private final List<SongWithUploaderInfo> data;
    private final OnRecentSongClick listener;

    public interface OnRecentSongClick {
        void onPlay(SongWithUploaderInfo song);
    }

    public RecentSongWithUploaderInfoAdapter(List<SongWithUploaderInfo> data, OnRecentSongClick listener) {
        this.data = new ArrayList<>(data != null ? data : new ArrayList<>());
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecentSongVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_song, parent, false);
        return new RecentSongVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentSongVH holder, int position) {
        SongWithUploaderInfo song = data.get(position);

        if (song == null) {
            android.util.Log.e("RecentSongAdapter", "Song at position " + position + " is null!");
            return;
        }

        // Set song title - IMPORTANT: This is the key fix
        String title = song.getTitle();
        if (title != null && !title.trim().isEmpty()) {
            holder.tvTitle.setText(title);
        } else {
            holder.tvTitle.setText("Unknown Title");
        }

        // Set uploader name with fallback
        holder.tvUploader.setText(song.getDisplayUploaderName());

        // Load cover art
        if (song.getCoverArtUrl() != null && !song.getCoverArtUrl().isEmpty()) {
            Glide.with(holder.imgCover.getContext())
                    .load(song.getCoverArtUrl())
                    .placeholder(R.drawable.splashi_icon)
                    .error(R.drawable.splashi_icon)
                    .into(holder.imgCover);
        } else {
            holder.imgCover.setImageResource(R.drawable.splashi_icon);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlay(song);
            }
        });
    }

    @Override
    public int getItemCount() {
        return Math.min(data.size(), 6); // Display maximum 6 recent songs
    }

    /**
     * Update songs list and notify adapter
     */
    public void updateSongs(List<SongWithUploaderInfo> newSongs) {
        data.clear();
        if (newSongs != null) {
            data.addAll(newSongs);
        }
        notifyDataSetChanged();
    }

    /**
     * Get current songs list (for creating NavigationContext)
     */
    public List<SongWithUploaderInfo> getSongs() {
        return new ArrayList<>(data);
    }

    static class RecentSongVH extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvTitle, tvUploader;

        RecentSongVH(View view) {
            super(view);
            imgCover = view.findViewById(R.id.imgRecentCover);
            tvTitle = view.findViewById(R.id.tvRecentTitle);
            tvUploader = view.findViewById(R.id.tvRecentUploader);
        }
    }
}
