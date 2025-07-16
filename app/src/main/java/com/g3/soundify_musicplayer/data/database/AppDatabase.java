package com.g3.soundify_musicplayer.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.musicplayer_prm.data.dao.CommentDao;
import com.example.musicplayer_prm.data.dao.CommentLikeDao;
import com.example.musicplayer_prm.data.dao.PlaylistDao;
import com.example.musicplayer_prm.data.dao.PlaylistSongDao;
import com.example.musicplayer_prm.data.dao.SongDao;
import com.example.musicplayer_prm.data.dao.SongLikeDao;
import com.example.musicplayer_prm.data.dao.UserDao;
import com.example.musicplayer_prm.data.dao.UserFollowDao;
import com.example.musicplayer_prm.data.entity.Comment;
import com.example.musicplayer_prm.data.entity.CommentLike;
import com.example.musicplayer_prm.data.entity.Playlist;
import com.example.musicplayer_prm.data.entity.PlaylistSong;
import com.example.musicplayer_prm.data.entity.Song;
import com.example.musicplayer_prm.data.entity.SongLike;
import com.example.musicplayer_prm.data.entity.User;
import com.example.musicplayer_prm.data.entity.UserFollow;

@Database(
    entities = {
        User.class,
        Song.class,
        Playlist.class,
        PlaylistSong.class,
        UserFollow.class,
        Comment.class,
        CommentLike.class,
        SongLike.class
    },
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "soundpify_database";
    private static volatile AppDatabase INSTANCE;
    
    // Abstract methods for DAOs
    public abstract UserDao userDao();
    public abstract SongDao songDao();
    public abstract PlaylistDao playlistDao();
    public abstract PlaylistSongDao playlistSongDao();
    public abstract UserFollowDao userFollowDao();
    public abstract CommentDao commentDao();
    public abstract CommentLikeDao commentLikeDao();
    public abstract SongLikeDao songLikeDao();
    
    // Singleton pattern
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration() // For development only
                    .build();
                }
            }
        }
        return INSTANCE;
    }
    
    public static void destroyInstance() {
        INSTANCE = null;
    }
} 