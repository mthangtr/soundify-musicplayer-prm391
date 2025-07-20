package com.g3.soundify_musicplayer.ui.player;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;

/**
 * ViewModel for the Mini Player component.
 * Acts as a bridge between the MiniPlayerFragment and the global MiniPlayerManager.
 * UI ONLY - No backend integration, uses mock data for demo purposes.
 */
public class MiniPlayerViewModel extends AndroidViewModel {

    private final MiniPlayerManager miniPlayerManager;

    public MiniPlayerViewModel(@NonNull Application application) {
        super(application);
        miniPlayerManager = MiniPlayerManager.getInstance();
    }

    // Delegate methods to MiniPlayerManager
    public void togglePlayPause() {
        miniPlayerManager.togglePlayPause();
    }

    public void playNext() {
        miniPlayerManager.playNextTrack();
    }

    public void hideMiniPlayer() {
        miniPlayerManager.hideMiniPlayer();
    }

    public void updateProgress(int progress) {
        miniPlayerManager.updateProgress(progress);
    }

    // LiveData getters - delegate to manager
    public LiveData<Song> getCurrentSong() {
        return miniPlayerManager.getCurrentSong();
    }

    public LiveData<User> getCurrentArtist() {
        return miniPlayerManager.getCurrentArtist();
    }

    public LiveData<Boolean> getIsPlaying() {
        return miniPlayerManager.getIsPlaying();
    }

    public LiveData<Integer> getProgress() {
        return miniPlayerManager.getProgress();
    }

    public LiveData<Boolean> getIsVisible() {
        return miniPlayerManager.getIsVisible();
    }
}
