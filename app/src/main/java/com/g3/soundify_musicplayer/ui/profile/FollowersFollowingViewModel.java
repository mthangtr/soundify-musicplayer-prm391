package com.g3.soundify_musicplayer.ui.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.g3.soundify_musicplayer.R;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.repository.MusicPlayerRepository;
import com.g3.soundify_musicplayer.data.repository.UserRepository;
import com.g3.soundify_musicplayer.utils.AuthManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FollowersFollowingViewModel extends AndroidViewModel {

    private final MusicPlayerRepository musicPlayerRepository;
    private final UserRepository userRepository;
    private final AuthManager authManager;
    private final ExecutorService executor;

    // LiveData for UI state
    private final MutableLiveData<Long> targetUserId = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();

    // User data
    private final MutableLiveData<User> targetUser = new MutableLiveData<>();
    private final LiveData<List<User>> followers;
    private final LiveData<List<User>> following;
    private final LiveData<List<User>> currentUserFollowing;

    // Tab state
    private final MutableLiveData<Integer> currentTab = new MutableLiveData<>(0); // 0 = Followers, 1 = Following

    public FollowersFollowingViewModel(@NonNull Application application) {
        super(application);
        musicPlayerRepository = new MusicPlayerRepository(application);
        userRepository = new UserRepository(application);
        authManager = new AuthManager(application);
        executor = Executors.newFixedThreadPool(4);

        // Set up LiveData transformations
        followers = Transformations.switchMap(targetUserId, userId -> {
            if (userId != null && userId > 0) {
                return musicPlayerRepository.getFollowers(userId);
            }
            return new MutableLiveData<>();
        });

        following = Transformations.switchMap(targetUserId, userId -> {
            if (userId != null && userId > 0) {
                return musicPlayerRepository.getFollowing(userId);
            }
            return new MutableLiveData<>();
        });

        // Get current user's following list to determine follow states
        long currentUserId = authManager.getCurrentUserId();
        if (currentUserId != -1) {
            currentUserFollowing = musicPlayerRepository.getFollowing(currentUserId);
        } else {
            currentUserFollowing = new MutableLiveData<>();
        }
    }

    // Getters for LiveData
    public LiveData<User> getTargetUser() { return targetUser; }
    public LiveData<List<User>> getFollowers() { return followers; }
    public LiveData<List<User>> getFollowing() { return following; }
    public LiveData<List<User>> getCurrentUserFollowing() { return currentUserFollowing; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<Integer> getCurrentTab() { return currentTab; }

    public void setTargetUserId(long userId) {
        if (targetUserId.getValue() == null || targetUserId.getValue() != userId) {
            targetUserId.setValue(userId);
            loadTargetUser(userId);
        }
    }

    public void setCurrentTab(int tab) {
        currentTab.setValue(tab);
    }

    private void loadTargetUser(long userId) {
        executor.execute(() -> {
            try {
                User user = userRepository.getUserByIdSync(userId).get();
                targetUser.postValue(user);
            } catch (Exception e) {
                errorMessage.postValue(getApplication().getString(R.string.error_loading_user));
            }
        });
    }

    public void toggleFollowStatus(User user) {
        long currentUserId = authManager.getCurrentUserId();
        if (currentUserId == -1) {
            errorMessage.setValue(getApplication().getString(R.string.error_not_logged_in));
            return;
        }

        if (user.getId() == currentUserId) {
            errorMessage.setValue(getApplication().getString(R.string.error_cannot_follow_self));
            return;
        }

        isLoading.setValue(true);

        executor.execute(() -> {
            try {
                // Check current follow status
                List<User> currentFollowing = currentUserFollowing.getValue();
                boolean isCurrentlyFollowing = false;
                
                if (currentFollowing != null) {
                    for (User followedUser : currentFollowing) {
                        if (followedUser.getId() == user.getId()) {
                            isCurrentlyFollowing = true;
                            break;
                        }
                    }
                }

                if (isCurrentlyFollowing) {
                    // Unfollow
                    musicPlayerRepository.unfollowUser(currentUserId, user.getId()).get();
                    successMessage.postValue(getApplication().getString(
                        R.string.unfollowed_user, user.getDisplayName() != null ? 
                        user.getDisplayName() : user.getUsername()));
                } else {
                    // Follow
                    musicPlayerRepository.followUser(currentUserId, user.getId()).get();
                    successMessage.postValue(getApplication().getString(
                        R.string.followed_user, user.getDisplayName() != null ? 
                        user.getDisplayName() : user.getUsername()));
                }

            } catch (Exception e) {
                errorMessage.postValue(getApplication().getString(R.string.error_follow_action));
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public void refreshData() {
        Long userId = targetUserId.getValue();
        if (userId != null && userId > 0) {
            // The LiveData will automatically refresh when we trigger a new query
            // For now, we can just show loading state
            isLoading.setValue(true);
            
            // Simulate refresh delay
            executor.execute(() -> {
                try {
                    Thread.sleep(1000); // 1 second delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    isLoading.postValue(false);
                }
            });
        }
    }

    public String getTabTitle(int position, int count) {
        switch (position) {
            case 0:
                return getApplication().getString(R.string.followers_tab_title, count);
            case 1:
                return getApplication().getString(R.string.following_tab_title, count);
            default:
                return "";
        }
    }

    public String getEmptyStateTitle(int currentTab) {
        switch (currentTab) {
            case 0:
                return getApplication().getString(R.string.no_followers_title);
            case 1:
                return getApplication().getString(R.string.no_following_title);
            default:
                return getApplication().getString(R.string.no_users_title);
        }
    }

    public String getEmptyStateSubtitle(int currentTab, boolean isOwnProfile) {
        switch (currentTab) {
            case 0:
                return isOwnProfile ? 
                    getApplication().getString(R.string.no_followers_subtitle_own) :
                    getApplication().getString(R.string.no_followers_subtitle);
            case 1:
                return isOwnProfile ? 
                    getApplication().getString(R.string.no_following_subtitle_own) :
                    getApplication().getString(R.string.no_following_subtitle);
            default:
                return getApplication().getString(R.string.no_users_subtitle);
        }
    }

    public boolean isOwnProfile() {
        long currentUserId = authManager.getCurrentUserId();
        Long targetId = targetUserId.getValue();
        return currentUserId != -1 && targetId != null && currentUserId == targetId;
    }

    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }

    public void clearSuccessMessage() {
        successMessage.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (executor != null) {
            executor.shutdown();
        }
        if (musicPlayerRepository != null) {
            musicPlayerRepository.shutdown();
        }
        if (userRepository != null) {
            userRepository.shutdown();
        }
    }
}
