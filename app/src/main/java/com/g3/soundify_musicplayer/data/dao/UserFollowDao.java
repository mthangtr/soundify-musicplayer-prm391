package com.g3.soundify_musicplayer.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.musicplayer_prm.data.entity.User;
import com.example.musicplayer_prm.data.entity.UserFollow;

import java.util.List;

@Dao
public interface UserFollowDao {
    
    @Insert
    void insert(UserFollow userFollow);
    
    @Delete
    void delete(UserFollow userFollow);
    
    @Query("SELECT u.* FROM users u INNER JOIN user_follows uf ON u.id = uf.followee_id WHERE uf.follower_id = :userId ORDER BY uf.created_at DESC")
    LiveData<List<User>> getFollowing(long userId);
    
    @Query("SELECT u.* FROM users u INNER JOIN user_follows uf ON u.id = uf.follower_id WHERE uf.followee_id = :userId ORDER BY uf.created_at DESC")
    LiveData<List<User>> getFollowers(long userId);
    
    @Query("SELECT COUNT(*) FROM user_follows WHERE follower_id = :userId")
    int getFollowingCount(long userId);
    
    @Query("SELECT COUNT(*) FROM user_follows WHERE followee_id = :userId")
    int getFollowersCount(long userId);
    
    @Query("SELECT COUNT(*) FROM user_follows WHERE follower_id = :followerId AND followee_id = :followeeId")
    int isFollowing(long followerId, long followeeId);
    
    @Query("DELETE FROM user_follows WHERE follower_id = :followerId AND followee_id = :followeeId")
    void unfollow(long followerId, long followeeId);
    
    @Query("SELECT * FROM user_follows WHERE follower_id = :followerId AND followee_id = :followeeId")
    UserFollow getFollowRelation(long followerId, long followeeId);
    
    @Query("SELECT followee_id FROM user_follows WHERE follower_id = :userId")
    List<Long> getFollowingIds(long userId);
} 