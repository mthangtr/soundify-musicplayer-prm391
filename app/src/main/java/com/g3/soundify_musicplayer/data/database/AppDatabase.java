package com.g3.soundify_musicplayer.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.g3.soundify_musicplayer.data.dao.CommentDao;
import com.g3.soundify_musicplayer.data.dao.CommentLikeDao;
import com.g3.soundify_musicplayer.data.dao.PlaylistDao;
import com.g3.soundify_musicplayer.data.dao.PlaylistSongDao;
import com.g3.soundify_musicplayer.data.dao.SongDao;
import com.g3.soundify_musicplayer.data.dao.SongLikeDao;
import com.g3.soundify_musicplayer.data.dao.UserDao;
import com.g3.soundify_musicplayer.data.dao.UserFollowDao;
import com.g3.soundify_musicplayer.data.dao.RecentlyPlayedDao;
import com.g3.soundify_musicplayer.data.dao.PlaylistAccessDao;
import com.g3.soundify_musicplayer.data.entity.Comment;
import com.g3.soundify_musicplayer.data.entity.CommentLike;
import com.g3.soundify_musicplayer.data.entity.Playlist;
import com.g3.soundify_musicplayer.data.entity.PlaylistSong;
import com.g3.soundify_musicplayer.data.entity.Song;
import com.g3.soundify_musicplayer.data.entity.SongLike;
import com.g3.soundify_musicplayer.data.entity.User;
import com.g3.soundify_musicplayer.data.entity.UserFollow;
import com.g3.soundify_musicplayer.data.entity.RecentlyPlayed;
import com.g3.soundify_musicplayer.data.entity.PlaylistAccess;

@Database(
    entities = {
        User.class,
        Song.class,
        Playlist.class,
        PlaylistSong.class,
        UserFollow.class,
        Comment.class,
        CommentLike.class,
        SongLike.class,
        RecentlyPlayed.class,
        PlaylistAccess.class
    },
    version = 3,
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
    public abstract RecentlyPlayedDao recentlyPlayedDao();
    public abstract PlaylistAccessDao playlistAccessDao();
    
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
                    // .addMigrations(MIGRATION_1_2)  // TODO: Fix migration
                    .fallbackToDestructiveMigration() // For development - will recreate DB
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