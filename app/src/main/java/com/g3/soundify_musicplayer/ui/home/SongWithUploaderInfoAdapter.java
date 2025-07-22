package com.g3.soundify_musicplayer.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
 * Adapter for displaying songs with uploader information
 * Used in HomeFragment for suggested songs section
 */
public class SongWithUploaderInfoAdapter extends RecyclerView.Adapter<SongWithUploaderInfoAdapter.SongVH> {

    private final List<SongWithUploaderInfo> data;
    private final OnSongClick listener;

    public interface OnSongClick {
        void onPlay(SongWithUploaderInfo song);
        void onOpenDetail(SongWithUploaderInfo song);
    }

    public SongWithUploaderInfoAdapter(List<SongWithUploaderInfo> data, OnSongClick listener) {
        this.data = new ArrayList<>(data != null ? data : new ArrayList<>());
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song_card, parent, false);
        return new SongVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongVH holder, int position) {
        SongWithUploaderInfo song = data.get(position);

        // Log for debugging - check if data is null
        android.util.Log.d("SongAdapter", "Binding position " + position +
                ", song: " + (song != null ? song.getTitle() : "NULL") +
                " by " + (song != null ? song.getDisplayUploaderName() : "NULL"));

        if (song == null) {
            android.util.Log.e("SongAdapter", "Song at position " + position + " is null!");
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

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOpenDetail(song);
            }
        });
        
        holder.btnPlay.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlay(song);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    /**
     * Update the data and notify adapter
     */
    public void updateData(List<SongWithUploaderInfo> newData) {
        data.clear();
        if (newData != null) {
            data.addAll(newData);
        }
        notifyDataSetChanged();
    }

    /**
     * Get current data list (for creating NavigationContext)
     */
    public List<SongWithUploaderInfo> getCurrentData() {
        return new java.util.ArrayList<>(data);
    }

    static class SongVH extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvTitle, tvUploader;
        ImageButton btnPlay;

        SongVH(View view) {
            super(view);
            imgCover = view.findViewById(R.id.imgCover);
            tvTitle = view.findViewById(R.id.tvTitle);
            tvUploader = view.findViewById(R.id.tvUploader);
            btnPlay = view.findViewById(R.id.btnPlay);
        }
    }
}
