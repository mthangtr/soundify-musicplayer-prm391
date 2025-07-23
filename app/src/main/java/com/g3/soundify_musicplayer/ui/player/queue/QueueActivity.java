package com.g3.soundify_musicplayer.ui.player.queue;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.repository.MediaPlayerRepository;
import com.g3.soundify_musicplayer.data.repository.SongRepository;
import com.g3.soundify_musicplayer.utils.RepositoryManager;

import java.util.List;

/**
 * Activity for displaying and managing the current queue.
 * Converted from Fragment-based to pure Activity-based architecture
 */
public class QueueActivity extends AppCompatActivity {

    private static final String EXTRA_SONG_ID = "song_id";

    // UI Components
    private ImageButton btnBack;
    private TextView textQueueTitle;
    private RecyclerView recyclerViewQueue;

    // Data and Adapter
    private QueueAdapter adapter;
    private ItemTouchHelper itemTouchHelper;
    private MediaPlayerRepository mediaPlayerRepository;
    private SongRepository songRepository;

    // Data
    private long songId;

    public static Intent createIntent(Context context, long songId) {
        Intent intent = new Intent(context, QueueActivity.class);
        intent.putExtra(EXTRA_SONG_ID, songId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);

        // Get song ID from intent
        songId = getIntent().getLongExtra(EXTRA_SONG_ID, -1);

        // Initialize repositories
        mediaPlayerRepository = RepositoryManager.getInstance(getApplication()).getMediaPlayerRepository();
        songRepository = RepositoryManager.getInstance(getApplication()).getSongRepository();

        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        setupObservers();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btn_back);
        textQueueTitle = findViewById(R.id.text_queue_title);
        recyclerViewQueue = findViewById(R.id.recycler_view_queue);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            finish();
        });
    }

    private void setupRecyclerView() {
        adapter = new QueueAdapter(this, songRepository);

        adapter.setOnItemClickListener((song, position) -> {
            mediaPlayerRepository.jumpToIndex(position);
        });

        adapter.setOnItemMoveListener((fromPosition, toPosition) -> {
            mediaPlayerRepository.moveItemInList(fromPosition, toPosition);
        });
        
        recyclerViewQueue.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewQueue.setAdapter(adapter);
        
        ItemTouchHelper.Callback callback = new QueueItemTouchHelperCallback(adapter);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerViewQueue);
        adapter.setItemTouchHelper(itemTouchHelper);
    }

    private void setupObservers() {
        mediaPlayerRepository.getQueueInfo().observe(this, queueInfo -> {
            if (queueInfo != null) {
                List<Song> songs = mediaPlayerRepository.getCurrentQueue();
                int currentIndex = queueInfo.getCurrentIndex();
                String title = queueInfo.getQueueTitle();
                User currentArtist = null;
                if (mediaPlayerRepository.getCurrentPlaybackState().getValue() != null) {
                    currentArtist = mediaPlayerRepository.getCurrentPlaybackState().getValue().getCurrentArtist();
                }
                adapter.updateData(songs, currentIndex, currentArtist);
                updateQueueTitle(queueInfo.getTotalSongs(), title != null ? title : "Queue");
            }
        });

        // Observe playback state changes to update current song highlighting
        mediaPlayerRepository.getCurrentPlaybackState().observe(this, playbackState -> {
            if (playbackState != null && playbackState.getCurrentSong() != null) {
                // Update adapter to highlight current song
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void updateQueueTitle(int totalSongs, String queueTitle) {
        String title = queueTitle + " (" + totalSongs + " songs)";
        textQueueTitle.setText(title);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }


}
