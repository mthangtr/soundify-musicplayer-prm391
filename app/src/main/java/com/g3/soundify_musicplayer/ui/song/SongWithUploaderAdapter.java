package com.g3.soundify_musicplayer.ui.song;

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
import com.g3.soundify_musicplayer.data.dto.SongWithUploader;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying songs with uploader information
 * Used in HomeFragment for suggested songs section
 */
public class SongWithUploaderAdapter extends RecyclerView.Adapter<SongWithUploaderAdapter.SongVH> {

    private final List<SongWithUploader> data;
    private final OnSongClick listener;

    public interface OnSongClick {
        void onPlay(Song song, User uploader);
        void onOpenDetail(Song song, User uploader);
    }

    public SongWithUploaderAdapter(List<SongWithUploader> data, OnSongClick listener) {
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

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOpenDetail(song, uploader);
            }
        });
        
        holder.btnPlay.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlay(song, uploader);
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
    public void updateData(List<SongWithUploader> newData) {
        data.clear();
        if (newData != null) {
            data.addAll(newData);
        }
        notifyDataSetChanged();
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
