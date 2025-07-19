package com.g3.soundify_musicplayer.ui.player;

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
import com.g3.soundify_musicplayer.utils.TimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapter for Queue with drag & drop functionality
 */
public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> 
        implements QueueItemTouchHelperCallback.ItemTouchHelperAdapter {

    private final Context context;
    private final List<QueueViewModel.QueueItem> queueItems;
    private OnItemClickListener listener;
    private ItemTouchHelper itemTouchHelper;
    private int currentPosition = 0;

    public interface OnItemClickListener {
        void onItemClick(Song song, int position);
    }

    public QueueAdapter(Context context) {
        this.context = context;
        this.queueItems = new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItemTouchHelper(ItemTouchHelper itemTouchHelper) {
        this.itemTouchHelper = itemTouchHelper;
    }

    public void setCurrentPosition(int position) {
        int oldPosition = this.currentPosition;
        this.currentPosition = position;
        
        // Update the playing state
        if (oldPosition < queueItems.size()) {
            queueItems.get(oldPosition).setCurrentlyPlaying(false);
            notifyItemChanged(oldPosition);
        }
        
        if (position < queueItems.size()) {
            queueItems.get(position).setCurrentlyPlaying(true);
            notifyItemChanged(position);
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
        QueueViewModel.QueueItem item = queueItems.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return queueItems.size();
    }

    public void updateData(List<QueueViewModel.QueueItem> newData) {
        queueItems.clear();
        if (newData != null) {
            queueItems.addAll(newData);
        }
        notifyDataSetChanged();
    }

    // ItemTouchHelperAdapter implementation
    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        // Don't allow moving the currently playing song
        if (fromPosition == currentPosition) {
            return false;
        }
        
        Collections.swap(queueItems, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        
        // Update current position if needed
        if (toPosition == currentPosition) {
            currentPosition = fromPosition;
        } else if (fromPosition < currentPosition && toPosition >= currentPosition) {
            currentPosition--;
        } else if (fromPosition > currentPosition && toPosition <= currentPosition) {
            currentPosition++;
        }
        
        return true;
    }

    @Override
    public void onItemDismiss(int position) {
        // Don't allow dismissing the currently playing song
        if (position == currentPosition) {
            return;
        }
        
        queueItems.remove(position);
        notifyItemRemoved(position);
        
        // Update current position if needed
        if (position < currentPosition) {
            currentPosition--;
        }
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

        public void bind(QueueViewModel.QueueItem item, int position) {
            Song song = item.getSong();
            
            tvSongTitle.setText(song.getTitle());
            tvArtistName.setText("Demo Artist"); // Mock artist name
            tvDuration.setText(TimeUtils.formatDuration(song.getDurationMs()));
            
            // Set album art placeholder
            ivAlbumArt.setImageResource(R.drawable.placeholder_album_art);
            
            // Show/hide playing indicator
            if (item.isCurrentlyPlaying()) {
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
