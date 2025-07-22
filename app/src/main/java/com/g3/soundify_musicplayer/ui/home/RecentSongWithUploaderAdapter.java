package com.g3.soundify_musicplayer.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.dto.SongWithUploader;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying recent songs with uploader information
 * Used in HomeFragment for recently played songs section
 */
public class RecentSongWithUploaderAdapter extends RecyclerView.Adapter<RecentSongWithUploaderAdapter.RecentSongVH> {

    private final List<SongWithUploader> data;
    private final OnRecentSongClick listener;

    public interface OnRecentSongClick {
        void onPlay(Song song, User uploader);
    }

    public RecentSongWithUploaderAdapter(List<SongWithUploader> data, OnRecentSongClick listener) {
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
        SongWithUploader songWithUploader = data.get(position);
        Song song = songWithUploader.getSong();
        User uploader = songWithUploader.getUploader();

        // Set song title
        holder.tvTitle.setText(song.getTitle());

        // Set uploader name with fallback
        if (uploader != null) {
            String uploaderName = uploader.getDisplayName();
            if (uploaderName == null || uploaderName.trim().isEmpty()) {
                uploaderName = uploader.getUsername();
            }
            if (uploaderName == null || uploaderName.trim().isEmpty()) {
                uploaderName = "Unknown Artist";
            }
            holder.tvUploader.setText(uploaderName);
        } else {
            holder.tvUploader.setText("Unknown Artist");
        }

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
                listener.onPlay(song, uploader);
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
    public void updateSongs(List<SongWithUploader> newSongs) {
        data.clear();
        if (newSongs != null) {
            data.addAll(newSongs);
        }
        notifyDataSetChanged();
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
