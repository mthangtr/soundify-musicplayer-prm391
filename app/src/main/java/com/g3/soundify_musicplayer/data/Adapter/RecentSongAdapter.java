package com.g3.soundify_musicplayer.data.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Song;

import java.util.ArrayList;
import java.util.List;

public class RecentSongAdapter extends RecyclerView.Adapter<RecentSongAdapter.RecentSongVH> {

    private final List<Song> data;
    private final OnRecentSongClick listener;

    public interface OnRecentSongClick {
        void onPlay(Song song);
    }

    public RecentSongAdapter(List<Song> data, OnRecentSongClick listener) {
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
        Song song = data.get(position);
        holder.tvTitle.setText(song.getTitle());
        holder.tvUploader.setText("Uploader ID: " + song.getUploaderId());

        // Load cover art
        if (song.getCoverArtUrl() != null && !song.getCoverArtUrl().isEmpty()) {
            Glide.with(holder.imgCover.getContext())
                    .load(song.getCoverArtUrl())
                    .placeholder(R.drawable.splashi_icon)
                    .into(holder.imgCover);
        } else {
            holder.imgCover.setImageResource(R.drawable.splashi_icon);
        }

        holder.itemView.setOnClickListener(v -> listener.onPlay(song));
    }

    @Override
    public int getItemCount() {
        return Math.min(data.size(), 6); // Hiển thị tối đa 6 bài (theo requirement mới)
    }

    /**
     * Update songs list and notify adapter
     */
    public void updateSongs(List<Song> newSongs) {
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