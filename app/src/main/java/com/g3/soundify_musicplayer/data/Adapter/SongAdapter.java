package com.g3.soundify_musicplayer.data.Adapter;

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
import com.g3.soundify_musicplayer.data.entity.Song;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongVH> {

    private final List<Song> data;
    private final OnSongClick listener;

    public interface OnSongClick {
        void onPlay(Song s);
        void onOpenDetail(Song s);
    }

    public SongAdapter(List<Song> data, OnSongClick l) {
        this.data = data;
        this.listener = l;
    }

    @NonNull
    @Override
    public SongVH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        View view = LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_song_card, p, false);
        return new SongVH(view);
    }

    @Override public void onBindViewHolder(@NonNull SongVH h, int i) {
        Song s = data.get(i);
        h.tvTitle.setText(s.getTitle());
        h.tvUploader.setText("Uploader ID: " + s.getUploaderId());

        // load cover: nếu null -> placeholder
        if (s.getCoverArtUrl() != null && !s.getCoverArtUrl().isEmpty()) {
            Glide.with(h.imgCover.getContext())
                    .load(s.getCoverArtUrl())
                    .placeholder(R.drawable.splashi_icon)
                    .into(h.imgCover);
        } else {
            h.imgCover.setImageResource(R.drawable.splashi_icon);
        }

        h.itemView.setOnClickListener(v -> listener.onOpenDetail(s));
        h.btnPlay.setOnClickListener(v -> listener.onPlay(s));
    }

    @Override public int getItemCount() { return data.size(); }

    static class SongVH extends RecyclerView.ViewHolder {
        ImageView imgCover; TextView tvTitle, tvUploader; ImageButton btnPlay;
        SongVH(View v) {
            super(v);
            imgCover = v.findViewById(R.id.imgCover);
            tvTitle  = v.findViewById(R.id.tvTitle);
            tvUploader = v.findViewById(R.id.tvUploader);
            btnPlay  = v.findViewById(R.id.btnPlay);
        }
    }
}
