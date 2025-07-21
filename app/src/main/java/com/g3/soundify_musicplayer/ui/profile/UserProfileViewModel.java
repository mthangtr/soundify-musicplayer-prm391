package com.g3.soundify_musicplayer.ui.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.repository.MusicPlayerRepository;
import com.g3.soundify_musicplayer.data.repository.PlaylistRepository;
import com.g3.soundify_musicplayer.data.dto.SongWithUploaderInfo;
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
    private final MusicPlayerRepository musicPlayerRepository;
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
    private final MutableLiveData<List<SongWithUploaderInfo>> publicSongsWithUploaderInfo = new MutableLiveData<>();
    
    // Current user ID
    private long currentUserId = -1;
    
    public UserProfileViewModel(@NonNull Application application) {
        super(application);
        this.userRepository = new UserRepository(application);
        this.songRepository = new SongRepository(application);
        this.playlistRepository = new PlaylistRepository(application);
        this.musicPlayerRepository = new MusicPlayerRepository(application);
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
        loadUserProfileInternal(userId);
    }

    /**
     * Force refresh user profile (even if same userId)
     */
    public void refreshUserProfile(long userId) {
        currentUserId = userId;
        loadUserProfileInternal(userId);
    }

    /**
     * Internal method to load user profile data
     */
    private void loadUserProfileInternal(long userId) {
        android.util.Log.d("UserProfileViewModel", "Loading user profile for userId: " + userId);
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
        if (followerId == -1 || followerId == followeeId) {
            // Not logged in or viewing own profile
            isFollowing.postValue(false);
            return;
        }

        new Thread(() -> {
            try {
                Future<Boolean> followStatusFuture = musicPlayerRepository.isFollowing(followerId, followeeId);
                Boolean followStatus = followStatusFuture.get();
                isFollowing.postValue(followStatus != null ? followStatus : false);
            } catch (ExecutionException | InterruptedException e) {
                isFollowing.postValue(false);
                android.util.Log.e("UserProfileViewModel", "Error loading follow status", e);
            }
        }).start();
    }
    
    /**
     * Load user statistics
     */
    private void loadUserStats(long userId) {
        new Thread(() -> {
            try {
                // Load followers count using MusicPlayerRepository
                Future<Integer> followersCountFuture = musicPlayerRepository.getFollowersCount(userId);
                Integer followersCountValue = followersCountFuture.get();
                followersCount.postValue(followersCountValue != null ? followersCountValue : 0);

                // Load following count using MusicPlayerRepository
                Future<Integer> followingCountFuture = musicPlayerRepository.getFollowingCount(userId);
                Integer followingCountValue = followingCountFuture.get();
                followingCount.postValue(followingCountValue != null ? followingCountValue : 0);
                
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
                // Handle error - set default values
                followersCount.postValue(0);
                followingCount.postValue(0);
                songsCount.postValue(0);
                android.util.Log.e("UserProfileViewModel", "Error loading user stats", e);
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
                Future<List<SongWithUploaderInfo>> songsWithInfoFuture;
                long currentLoggedUserId = authManager.getCurrentUserId();
                if (currentLoggedUserId == userId) {
                    // Own profile - show all songs
                    songsFuture = songRepository.getSongsByUploaderSync(userId);
                    songsWithInfoFuture = songRepository.getSongsByUploaderWithInfoSync(userId);
                } else {
                    // Other user's profile - show only public songs
                    songsFuture = songRepository.getPublicSongsByUploaderSync(userId);
                    songsWithInfoFuture = songRepository.getPublicSongsByUploaderWithInfoSync(userId);
                }

                List<Song> songs = songsFuture.get();
                List<SongWithUploaderInfo> songsWithInfo = songsWithInfoFuture.get();
                publicSongs.postValue(songs);
                publicSongsWithUploaderInfo.postValue(songsWithInfo);
                
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
        
        // Optimistic UI update
        isFollowing.setValue(newFollowStatus);

        new Thread(() -> {
            try {
                // Perform actual follow/unfollow operation
                if (newFollowStatus) {
                    // Follow
                    musicPlayerRepository.followUser(currentLoggedUserId, user.getId()).get();
                } else {
                    // Unfollow
                    musicPlayerRepository.unfollowUser(currentLoggedUserId, user.getId()).get();
                }

                String message = newFollowStatus ?
                    "Now following " + user.getDisplayName() :
                    "Unfollowed " + user.getDisplayName();
                successMessage.postValue(message);

                // Reload user stats to get accurate counts
                loadUserStats(user.getId());

            } catch (ExecutionException | InterruptedException e) {
                // Revert optimistic update on error
                isFollowing.postValue(!newFollowStatus);
                errorMessage.postValue("Error updating follow status");
                android.util.Log.e("UserProfileViewModel", "Error toggling follow status", e);
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

    public LiveData<List<SongWithUploaderInfo>> getPublicSongsWithUploaderInfo() {
        return publicSongsWithUploaderInfo;
    }

    public LiveData<List<Playlist>> getPublicPlaylists() {
        return publicPlaylists;
    }
    
    public long getCurrentUserId() {
        return currentUserId;
    }

    public void refreshUserData() {
        User user = currentUser.getValue();
        if (user != null) {
            loadUserProfileInternal(user.getId());
        }
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
