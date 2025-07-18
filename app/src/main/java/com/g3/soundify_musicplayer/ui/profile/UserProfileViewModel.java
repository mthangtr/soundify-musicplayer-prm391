package com.g3.soundify_musicplayer.ui.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.repository.PlaylistRepository;
import com.g3.soundify_musicplayer.data.repository.SongRepository;
import com.g3.soundify_musicplayer.data.repository.UserRepository;
import com.g3.soundify_musicplayer.utils.AuthManager;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * ViewModel for User Profile Screen
 * Manages user data, follow status, and user's public content
 */
public class UserProfileViewModel extends AndroidViewModel {
    
    private final UserRepository userRepository;
    private final SongRepository songRepository;
    private final PlaylistRepository playlistRepository;
    private final AuthManager authManager;
    
    // LiveData for UI state
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isOwnProfile = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isFollowing = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    
    // Stats LiveData
    private final MutableLiveData<Integer> followersCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> followingCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> songsCount = new MutableLiveData<>(0);
    
    // Content LiveData
    private final MutableLiveData<List<Song>> publicSongs = new MutableLiveData<>();
    private final MutableLiveData<List<Playlist>> publicPlaylists = new MutableLiveData<>();
    
    // Current user ID
    private long currentUserId = -1;
    
    public UserProfileViewModel(@NonNull Application application) {
        super(application);
        this.userRepository = new UserRepository(application);
        this.songRepository = new SongRepository(application);
        this.playlistRepository = new PlaylistRepository(application);
        this.authManager = new AuthManager(application);
    }
    
    /**
     * Load user profile by ID
     */
    public void loadUserProfile(long userId) {
        if (currentUserId == userId) {
            return; // Already loaded
        }
        
        currentUserId = userId;
        isLoading.setValue(true);
        
        // Load user data in background
        new Thread(() -> {
            try {
                // Load user info
                Future<User> userFuture = userRepository.getUserByIdSync(userId);
                User user = userFuture.get();
                
                if (user != null) {
                    currentUser.postValue(user);
                    
                    // Check if this is own profile
                    long currentLoggedUserId = authManager.getCurrentUserId();
                    boolean isOwn = currentLoggedUserId != -1 && currentLoggedUserId == userId;
                    isOwnProfile.postValue(isOwn);
                    
                    // Load follow status if not own profile
                    if (!isOwn && currentLoggedUserId != -1) {
                        loadFollowStatus(currentLoggedUserId, userId);
                    }
                    
                    // Load stats and content
                    loadUserStats(userId);
                    loadUserContent(userId);
                    
                } else {
                    errorMessage.postValue("User not found");
                }
                
            } catch (ExecutionException | InterruptedException e) {
                errorMessage.postValue("Error loading profile");
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }
    
    /**
     * Load follow status between current user and profile user
     */
    private void loadFollowStatus(long followerId, long followeeId) {
        // TODO: Implement when FollowRepository is available
        // For now, set to false
        isFollowing.postValue(false);
    }
    
    /**
     * Load user statistics
     */
    private void loadUserStats(long userId) {
        new Thread(() -> {
            try {
                // Load followers count
                // TODO: Implement when Follow entity is available
                followersCount.postValue(0);
                
                // Load following count
                // TODO: Implement when Follow entity is available
                followingCount.postValue(0);
                
                // Load songs count (public songs only for other users)
                Future<List<Song>> songsFuture;
                long currentLoggedUserId = authManager.getCurrentUserId();
                if (currentLoggedUserId == userId) {
                    // Own profile - show all songs
                    songsFuture = songRepository.getSongsByUploaderSync(userId);
                } else {
                    // Other user's profile - show only public songs
                    songsFuture = songRepository.getPublicSongsByUploaderSync(userId);
                }
                
                List<Song> songs = songsFuture.get();
                songsCount.postValue(songs != null ? songs.size() : 0);
                
            } catch (ExecutionException | InterruptedException e) {
                // Handle error silently for stats
            }
        }).start();
    }
    
    /**
     * Load user's public content
     */
    private void loadUserContent(long userId) {
        new Thread(() -> {
            try {
                // Load public songs
                Future<List<Song>> songsFuture;
                long currentLoggedUserId = authManager.getCurrentUserId();
                if (currentLoggedUserId == userId) {
                    // Own profile - show all songs
                    songsFuture = songRepository.getSongsByUploaderSync(userId);
                } else {
                    // Other user's profile - show only public songs
                    songsFuture = songRepository.getPublicSongsByUploaderSync(userId);
                }
                
                List<Song> songs = songsFuture.get();
                publicSongs.postValue(songs);
                
                // Load public playlists
                Future<List<Playlist>> playlistsFuture;
                if (currentLoggedUserId == userId) {
                    // Own profile - show all playlists
                    playlistsFuture = playlistRepository.getPlaylistsByOwnerSync(userId);
                } else {
                    // Other user's profile - show only public playlists
                    playlistsFuture = playlistRepository.getPublicPlaylistsByOwnerSync(userId);
                }
                
                List<Playlist> playlists = playlistsFuture.get();
                publicPlaylists.postValue(playlists);
                
            } catch (ExecutionException | InterruptedException e) {
                errorMessage.postValue("Error loading user content");
            }
        }).start();
    }
    
    /**
     * Toggle follow status
     */
    public void toggleFollowStatus() {
        User user = currentUser.getValue();
        if (user == null) {
            errorMessage.setValue("User not found");
            return;
        }
        
        long currentLoggedUserId = authManager.getCurrentUserId();
        if (currentLoggedUserId == -1) {
            errorMessage.setValue("Please log in to follow users");
            return;
        }
        
        if (currentLoggedUserId == user.getId()) {
            errorMessage.setValue("Cannot follow yourself");
            return;
        }
        
        Boolean currentFollowStatus = isFollowing.getValue();
        boolean newFollowStatus = !Boolean.TRUE.equals(currentFollowStatus);
        
        isLoading.setValue(true);
        
        new Thread(() -> {
            try {
                // TODO: Implement follow/unfollow operations when FollowRepository is available
                // For now, just toggle the status
                Thread.sleep(500); // Simulate network delay
                
                isFollowing.postValue(newFollowStatus);
                
                String message = newFollowStatus ? 
                    "Now following " + user.getDisplayName() :
                    "Unfollowed " + user.getDisplayName();
                successMessage.postValue(message);
                
                // Update followers count
                Integer currentCount = followersCount.getValue();
                int newCount = currentCount != null ? currentCount : 0;
                if (newFollowStatus) {
                    newCount++;
                } else {
                    newCount = Math.max(0, newCount - 1);
                }
                followersCount.postValue(newCount);
                
            } catch (InterruptedException e) {
                errorMessage.postValue("Error updating follow status");
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }
    
    /**
     * Get formatted followers count string
     */
    public String getFollowersCountString() {
        Integer count = followersCount.getValue();
        if (count == null || count == 0) {
            return "0";
        } else if (count < 1000) {
            return count.toString();
        } else if (count < 1000000) {
            return String.format("%.1fK", count / 1000.0);
        } else {
            return String.format("%.1fM", count / 1000000.0);
        }
    }
    
    /**
     * Get formatted following count string
     */
    public String getFollowingCountString() {
        Integer count = followingCount.getValue();
        if (count == null || count == 0) {
            return "0";
        } else if (count < 1000) {
            return count.toString();
        } else {
            return String.format("%.1fK", count / 1000.0);
        }
    }
    
    /**
     * Get formatted songs count string
     */
    public String getSongsCountString() {
        Integer count = songsCount.getValue();
        if (count == null || count == 0) {
            return "0";
        } else {
            return count.toString();
        }
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
    public LiveData<User> getCurrentUser() {
        return currentUser;
    }
    
    public LiveData<Boolean> getIsOwnProfile() {
        return isOwnProfile;
    }
    
    public LiveData<Boolean> getIsFollowing() {
        return isFollowing;
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
    
    public LiveData<Integer> getFollowersCount() {
        return followersCount;
    }
    
    public LiveData<Integer> getFollowingCount() {
        return followingCount;
    }
    
    public LiveData<Integer> getSongsCount() {
        return songsCount;
    }
    
    public LiveData<List<Song>> getPublicSongs() {
        return publicSongs;
    }
    
    public LiveData<List<Playlist>> getPublicPlaylists() {
        return publicPlaylists;
    }
    
    public long getCurrentUserId() {
        return currentUserId;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up resources
        userRepository.shutdown();
        songRepository.shutdown();
        playlistRepository.shutdown();
    }
}
