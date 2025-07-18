package com.g3.soundify_musicplayer.ui.profile;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter for public playlists in user profile
 */
public class PublicPlaylistAdapter extends RecyclerView.Adapter<PublicPlaylistAdapter.PlaylistViewHolder> {
    
    private List<Playlist> playlists = new ArrayList<>();
    private final Context context;
    private OnPlaylistClickListener listener;
    private boolean showMenu = false;
    
    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist, int position);
        void onPlaylistMenuClick(Playlist playlist, int position);
    }
    
    public PublicPlaylistAdapter(Context context) {
        this.context = context;
    }
    
    public void setOnPlaylistClickListener(OnPlaylistClickListener listener) {
        this.listener = listener;
    }
    
    public void setShowMenu(boolean showMenu) {
        this.showMenu = showMenu;
        notifyDataSetChanged();
    }
    
    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists != null ? new ArrayList<>(playlists) : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public List<Playlist> getPlaylists() {
        return new ArrayList<>(playlists);
    }
    
    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_public_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.bind(playlist, position);
    }
    
    @Override
    public int getItemCount() {
        return playlists.size();
    }
    
    class PlaylistViewHolder extends RecyclerView.ViewHolder {
        
        private final ShapeableImageView playlistCover;
        private final TextView playlistName;
        private final TextView playlistDescription;
        private final TextView playlistInfo;
        private final ImageButton menuButton;
        
        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            
            playlistCover = itemView.findViewById(R.id.image_view_playlist_cover);
            playlistName = itemView.findViewById(R.id.text_view_playlist_name);
            playlistDescription = itemView.findViewById(R.id.text_view_playlist_description);
            playlistInfo = itemView.findViewById(R.id.text_view_playlist_info);
            menuButton = itemView.findViewById(R.id.button_playlist_menu);
            
            // Set up click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onPlaylistClick(playlists.get(position), position);
                }
            });
            
            menuButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onPlaylistMenuClick(playlists.get(position), position);
                }
            });
        }
        
        public void bind(Playlist playlist, int position) {
            // Set playlist name
            playlistName.setText(playlist.getName());
            
            // Set playlist description
            if (playlist.getDescription() != null && !playlist.getDescription().trim().isEmpty()) {
                playlistDescription.setText(playlist.getDescription());
                playlistDescription.setVisibility(View.VISIBLE);
            } else {
                playlistDescription.setVisibility(View.GONE);
            }
            
            // Set playlist info (song count + duration)
            // TODO: Get actual song count and duration from database
            // For now, show placeholder
            playlistInfo.setText("Playlist");
            
            // Set cover art (placeholder for now)
            // TODO: Generate playlist cover from first few songs or use custom cover
            playlistCover.setImageResource(R.drawable.placeholder_album_art);
            
            // Show/hide menu button
            menuButton.setVisibility(showMenu ? View.VISIBLE : View.GONE);
        }
    }
}
