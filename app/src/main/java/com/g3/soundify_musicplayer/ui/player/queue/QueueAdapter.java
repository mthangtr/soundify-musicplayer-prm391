package com.g3.soundify_musicplayer.ui.player.queue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for Queue with drag & drop functionality
 */
public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder>
        implements QueueItemTouchHelperCallback.ItemTouchHelperAdapter {

    private final Context context;
    private final List<Song> queueItems;
    private OnItemClickListener listener;
    private OnItemMoveListener moveListener;
    private ItemTouchHelper itemTouchHelper;
    private int currentPlayingIndex = -1;
    private User currentArtist;

    public interface OnItemClickListener {
        void onItemClick(Song song, int position);
    }

    public interface OnItemMoveListener {
        void onItemMoveRequested(int fromPosition, int toPosition);
    }

    public QueueAdapter(Context context) {
        this.context = context;
        this.queueItems = new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemMoveListener(OnItemMoveListener moveListener) {
        this.moveListener = moveListener;
    }

    public void setItemTouchHelper(ItemTouchHelper itemTouchHelper) {
        this.itemTouchHelper = itemTouchHelper;
    }

    public void setCurrentPlayingIndex(int index) {
        int oldIndex = this.currentPlayingIndex;
        this.currentPlayingIndex = index;

        // Update the old and new positions
        if (oldIndex != -1) {
            notifyItemChanged(oldIndex);
        }
        if (index != -1) {
            notifyItemChanged(index);
        }
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_queue_song, parent, false);
        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        Song song = queueItems.get(position);
        boolean isCurrentlyPlaying = (position == currentPlayingIndex);
        holder.bind(song, position, isCurrentlyPlaying);
    }

    @Override
    public int getItemCount() {
        return queueItems.size();
    }

    public void updateData(List<Song> newData, int currentPlayingIndex, User artist) {
        queueItems.clear();
        if (newData != null) {
            queueItems.addAll(newData);
        }
        this.currentPlayingIndex = currentPlayingIndex;
        this.currentArtist = artist;
        notifyDataSetChanged();
    }

    /**
     * Update only the current playing index without reloading the entire list
     * More efficient for when only the playing indicator needs to change
     */
    public void updateCurrentPlayingIndex(int newIndex) {
        int oldIndex = this.currentPlayingIndex;
        this.currentPlayingIndex = newIndex;

        // Only update the affected items
        if (oldIndex != -1 && oldIndex < queueItems.size()) {
            notifyItemChanged(oldIndex);
        }
        if (newIndex != -1 && newIndex < queueItems.size()) {
            notifyItemChanged(newIndex);
        }
    }
    
    /**
     * ✅ Update current artist efficiently
     */
    public void updateCurrentArtist(User artist) {
        this.currentArtist = artist;
        // Only update currently playing item to show correct artist
        if (currentPlayingIndex >= 0 && currentPlayingIndex < queueItems.size()) {
            notifyItemChanged(currentPlayingIndex);
        }
    }

    // ItemTouchHelperAdapter implementation
    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        // Don't allow moving the currently playing song
        if (fromPosition == currentPlayingIndex) {
            return false;
        }

        // Report the move request to Fragment/ViewModel instead of handling it here
        if (moveListener != null) {
            moveListener.onItemMoveRequested(fromPosition, toPosition);
        }

        return true;
    }

    @Override
    public void onItemDismiss(int position) {
        // Don't allow dismissing the currently playing song
        if (position == currentPlayingIndex) {
            return;
        }

        // For now, we don't support removing songs from queue
        // This can be implemented later if needed
    }

    class QueueViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivAlbumArt;
        private final TextView tvSongTitle;
        private final TextView tvArtistName;
        private final TextView tvDuration;
        private final ImageView ivDragHandle;
        private final ImageView ivPlayingIndicator;
        private final View itemContainer;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAlbumArt = itemView.findViewById(R.id.iv_album_art);
            tvSongTitle = itemView.findViewById(R.id.tv_song_title);
            tvArtistName = itemView.findViewById(R.id.tv_artist_name);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            ivDragHandle = itemView.findViewById(R.id.iv_drag_handle);
            ivPlayingIndicator = itemView.findViewById(R.id.iv_playing_indicator);
            itemContainer = itemView.findViewById(R.id.item_container);
        }

        public void bind(Song song, int position, boolean isCurrentlyPlaying) {
            tvSongTitle.setText(song.getTitle());

            // Use real artist name from current artist info
            String artistName = "Unknown Artist";
            if (currentArtist != null && currentArtist.getUsername() != null) {
                artistName = currentArtist.getUsername();
            }
            tvArtistName.setText(artistName);

            // ✅ NULL SAFE: Handle null duration
            Integer duration = song.getDurationMs();
            if (duration != null) {
                tvDuration.setText(TimeUtils.formatDuration(duration));
            } else {
                tvDuration.setText("0:00");
            }

            // Set album art placeholder
            ivAlbumArt.setImageResource(R.drawable.placeholder_album_art);

            // Show/hide playing indicator
            if (isCurrentlyPlaying) {
                ivPlayingIndicator.setVisibility(View.VISIBLE);
                ivPlayingIndicator.setImageResource(R.drawable.ic_play);
                itemContainer.setAlpha(1.0f);
                tvSongTitle.setTextColor(context.getColor(R.color.accent_blue));
            } else {
                ivPlayingIndicator.setVisibility(View.GONE);
                itemContainer.setAlpha(0.8f);
                tvSongTitle.setTextColor(context.getColor(R.color.text_primary));
            }

            // Setup drag handle
            ivDragHandle.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN && itemTouchHelper != null) {
                    itemTouchHelper.startDrag(this);
                }
                return false;
            });

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(song, position);
                }
            });
        }
    }
}
