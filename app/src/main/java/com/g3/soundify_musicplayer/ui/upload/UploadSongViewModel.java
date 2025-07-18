package com.g3.soundify_musicplayer.ui.upload;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.repository.SongRepository;
import com.g3.soundify_musicplayer.utils.AuthManager;
import com.g3.soundify_musicplayer.utils.FileUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * ViewModel for Upload/Edit Song Screen
 * Handles song creation, update, and deletion operations
 */
public class UploadSongViewModel extends AndroidViewModel {
    
    private final SongRepository songRepository;
    private final AuthManager authManager;
    
    // LiveData for UI state
    private final MutableLiveData<Song> currentSong = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isEditMode = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    
    // Form data
    private final MutableLiveData<Uri> selectedAudioUri = new MutableLiveData<>();
    private final MutableLiveData<Uri> selectedCoverArtUri = new MutableLiveData<>();
    private final MutableLiveData<String> audioFileName = new MutableLiveData<>();
    
    public UploadSongViewModel(@NonNull Application application) {
        super(application);
        this.songRepository = new SongRepository(application);
        this.authManager = new AuthManager(application);
    }
    
    /**
     * Load existing song for editing
     */
    public void loadSongForEdit(long songId) {
        isLoading.setValue(true);
        isEditMode.setValue(true);
        
        // Load song data in background thread
        Future<Song> future = songRepository.getSongByIdSync(songId);
        try {
            Song song = future.get();
            if (song != null) {
                currentSong.setValue(song);
                // Set audio file name if available
                if (song.getAudioUrl() != null) {
                    String fileName = extractFileNameFromUri(song.getAudioUrl());
                    audioFileName.setValue(fileName);
                }
            } else {
                errorMessage.setValue("Không tìm thấy bài hát");
            }
        } catch (ExecutionException | InterruptedException e) {
            errorMessage.setValue("Lỗi khi tải dữ liệu bài hát");
        } finally {
            isLoading.setValue(false);
        }
    }
    
    /**
     * Initialize for new song creation
     */
    public void initializeForNewSong() {
        isEditMode.setValue(false);
        currentSong.setValue(null);
        selectedAudioUri.setValue(null);
        selectedCoverArtUri.setValue(null);
        audioFileName.setValue(null);
    }
    
    /**
     * Set selected audio file
     */
    public void setSelectedAudioFile(Uri audioUri, String fileName) {
        selectedAudioUri.setValue(audioUri);
        audioFileName.setValue(fileName);
    }
    
    /**
     * Set selected cover art
     */
    public void setSelectedCoverArt(Uri coverArtUri) {
        selectedCoverArtUri.setValue(coverArtUri);
    }
    
    /**
     * Save song (create or update)
     */
    public void saveSong(String title, String description, String genre, boolean isPublic) {
        // Validate input
        if (!validateInput(title, description)) {
            return;
        }
        
        isLoading.setValue(true);
        
        Boolean editMode = isEditMode.getValue();
        if (editMode != null && editMode) {
            updateExistingSong(title, description, genre, isPublic);
        } else {
            createNewSong(title, description, genre, isPublic);
        }
    }
    
    /**
     * Create new song
     */
    private void createNewSong(String title, String description, String genre, boolean isPublic) {
        Uri audioUri = selectedAudioUri.getValue();
        if (audioUri == null) {
            errorMessage.setValue("Vui lòng chọn file audio");
            isLoading.setValue(false);
            return;
        }
        
        long currentUserId = authManager.getCurrentUserId();
        if (currentUserId == -1) {
            errorMessage.setValue("Vui lòng đăng nhập để upload bài hát");
            isLoading.setValue(false);
            return;
        }
        
        // Copy audio file to internal storage
        String fileName = FileUtils.getFileName(getApplication(), audioUri);
        String internalPath = FileUtils.copyFileToInternalStorage(getApplication(), audioUri, fileName);

        if (internalPath == null) {
            errorMessage.setValue("Lỗi khi sao chép file audio");
            isLoading.setValue(false);
            return;
        }

        // Get audio duration
        long duration = FileUtils.getAudioDuration(getApplication(), audioUri);

        // Create new song object
        Song newSong = new Song(currentUserId, title, internalPath);
        newSong.setDescription(description);
        newSong.setGenre(genre);
        newSong.setPublic(isPublic);
        newSong.setCreatedAt(System.currentTimeMillis());
        if (duration > 0) {
            newSong.setDurationMs((int) duration);
        }

        // Set cover art if selected
        Uri coverArtUri = selectedCoverArtUri.getValue();
        if (coverArtUri != null) {
            // Copy cover art to internal storage
            String coverFileName = FileUtils.getFileName(getApplication(), coverArtUri);
            String coverInternalPath = FileUtils.copyFileToInternalStorage(getApplication(), coverArtUri, "cover_" + coverFileName);
            if (coverInternalPath != null) {
                newSong.setCoverArtUrl(coverInternalPath);
            }
        }
        
        // Save to database
        Future<Long> future = songRepository.insert(newSong);
        try {
            Long songId = future.get();
            if (songId != null && songId > 0) {
                successMessage.setValue("Bài hát đã được tải lên thành công");
            } else {
                errorMessage.setValue("Lỗi khi lưu bài hát");
            }
        } catch (ExecutionException | InterruptedException e) {
            errorMessage.setValue("Lỗi khi lưu bài hát: " + e.getMessage());
        } finally {
            isLoading.setValue(false);
        }
    }
    
    /**
     * Update existing song
     */
    private void updateExistingSong(String title, String description, String genre, boolean isPublic) {
        Song song = currentSong.getValue();
        if (song == null) {
            errorMessage.setValue("Không tìm thấy bài hát để cập nhật");
            isLoading.setValue(false);
            return;
        }
        
        // Update song properties
        song.setTitle(title);
        song.setDescription(description);
        song.setGenre(genre);
        song.setPublic(isPublic);
        
        // Update audio file if new one is selected
        Uri audioUri = selectedAudioUri.getValue();
        if (audioUri != null) {
            song.setAudioUrl(audioUri.toString());
        }
        
        // Update cover art if new one is selected
        Uri coverArtUri = selectedCoverArtUri.getValue();
        if (coverArtUri != null) {
            song.setCoverArtUrl(coverArtUri.toString());
        }
        
        // Save to database
        Future<Void> future = songRepository.update(song);
        try {
            future.get();
            successMessage.setValue("Bài hát đã được cập nhật");
        } catch (ExecutionException | InterruptedException e) {
            errorMessage.setValue("Lỗi khi cập nhật bài hát: " + e.getMessage());
        } finally {
            isLoading.setValue(false);
        }
    }
    
    /**
     * Delete current song
     */
    public void deleteSong() {
        Song song = currentSong.getValue();
        if (song == null) {
            errorMessage.setValue("Không tìm thấy bài hát để xóa");
            return;
        }
        
        isLoading.setValue(true);
        
        Future<Void> future = songRepository.delete(song);
        try {
            future.get();
            successMessage.setValue("Bài hát đã được xóa");
        } catch (ExecutionException | InterruptedException e) {
            errorMessage.setValue("Lỗi khi xóa bài hát: " + e.getMessage());
        } finally {
            isLoading.setValue(false);
        }
    }
    
    /**
     * Validate input data
     */
    private boolean validateInput(String title, String description) {
        if (title == null || title.trim().isEmpty()) {
            errorMessage.setValue("Vui lòng nhập tiêu đề bài hát");
            return false;
        }
        
        if (title.length() > 100) {
            errorMessage.setValue("Tiêu đề quá dài (tối đa 100 ký tự)");
            return false;
        }
        
        if (description != null && description.length() > 500) {
            errorMessage.setValue("Mô tả quá dài (tối đa 500 ký tự)");
            return false;
        }
        
        return true;
    }
    
    /**
     * Extract file name from URI
     */
    private String extractFileNameFromUri(String uriString) {
        if (uriString == null) return null;
        
        try {
            Uri uri = Uri.parse(uriString);
            String path = uri.getPath();
            if (path != null) {
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash != -1 && lastSlash < path.length() - 1) {
                    return path.substring(lastSlash + 1);
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        
        return "audio_file.mp3";
    }
    
    /**
     * Clear error message
     */
    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }
    
    /**
     * Clear success message
     */
    public void clearSuccessMessage() {
        successMessage.setValue(null);
    }
    
    // Getters for LiveData
    public LiveData<Song> getCurrentSong() {
        return currentSong;
    }
    
    public LiveData<Boolean> getIsEditMode() {
        return isEditMode;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }
    
    public LiveData<Uri> getSelectedAudioUri() {
        return selectedAudioUri;
    }
    
    public LiveData<Uri> getSelectedCoverArtUri() {
        return selectedCoverArtUri;
    }
    
    public LiveData<String> getAudioFileName() {
        return audioFileName;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up resources if needed
        songRepository.shutdown();
    }
}
