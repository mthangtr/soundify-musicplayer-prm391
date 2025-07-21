# Song Detail Backend Implementation

## Tổng quan

Đã implement đầy đủ backend logic cho Song Detail functionality sử dụng Room/SQLite với các operations CRUD hoàn chỉnh. Implementation tuân theo kiến trúc MVVM và sử dụng ExecutorService để đảm bảo tất cả database operations chạy trên background threads.

## Cấu trúc Implementation

### 1. Enhanced Repository Classes

#### SongRepository
- **Thêm methods mới:**
  - `getSongWithMetadata()` - Lấy song với metadata bổ sung
  - `isSongAccessible()` - Kiểm tra quyền truy cập song
  - `getMoreSongsByUploader()` - Lấy thêm bài hát từ cùng uploader
  - `getRelatedSongsByGenre()` - Lấy bài hát liên quan theo genre

#### MusicPlayerRepository  
- **Enhanced Song Like Operations:**
  - `toggleSongLike()` - Toggle trạng thái like/unlike
  - `getSongLikeInfo()` - Lấy thông tin like (status + count) trong một call
  - `SongLikeInfo` helper class

- **Enhanced Comment Operations:**
  - `toggleCommentLike()` - Toggle like comment
  - `getCommentLikeInfo()` - Lấy thông tin like comment
  - `deleteCommentById()` - Xóa comment theo ID
  - `CommentLikeInfo` helper class

#### PlaylistRepository
- **Enhanced Playlist Operations:**
  - `getUserPlaylistsForAddSong()` - Lấy playlists của user cho dialog "Add to Playlist"
  - `addSongToMultiplePlaylists()` - Thêm song vào nhiều playlists cùng lúc
  - `getPlaylistIdsContainingSong()` - Lấy danh sách playlist IDs chứa song
  - `createPlaylistWithSong()` - Tạo playlist mới và thêm song vào
  - `getPlaylistInfo()` - Lấy thông tin playlist với song count
  - `PlaylistInfo` helper class

### 2. SongDetailRepository - Repository Tổng Hợp

Tạo một repository tổng hợp kết hợp tất cả operations cần thiết cho Song Detail screen:

```java
public class SongDetailRepository {
    // Kết hợp SongRepository, MusicPlayerRepository, PlaylistRepository
    // Cung cấp interface thống nhất cho Song Detail functionality
    
    // Method quan trọng:
    public Future<SongDetailData> getSongDetailData(long songId, long userId)
    // Lấy tất cả data cần thiết trong một call
}
```

### 3. SongDetailViewModel - ViewModel Mẫu

Tạo ViewModel demonstrate cách sử dụng SongDetailRepository:

```java
public class SongDetailViewModel extends AndroidViewModel {
    // Quản lý UI state với LiveData
    // Xử lý user interactions
    // Error handling và loading states
}
```

## Các Operations Đã Implement

### Song Like Functionality ✅
- **Like/Unlike song:** `toggleSongLike(songId, userId)`
- **Get like count:** `getSongLikeInfo(songId, userId)`  
- **Check if user liked:** Included in `SongLikeInfo`
- **Get users who liked:** `getUsersWhoLikedSong(songId)`

### Song Comments Functionality ✅
- **Add comment:** `addComment(songId, userId, content)`
- **Edit comment:** `updateComment(comment)`
- **Delete comment:** `deleteComment(comment)` hoặc `deleteCommentById(commentId)`
- **Get comments:** `getCommentsBySong(songId)`
- **Get comment count:** `getCommentCountBySong(songId)`
- **Like/Unlike comments:** `toggleCommentLike(commentId, userId)`

### Add to Playlist Functionality ✅
- **Get user playlists:** `getUserPlaylistsForAddSong(userId)`
- **Add to playlist:** `addSongToPlaylist(playlistId, songId)`
- **Add to multiple playlists:** `addSongToMultiplePlaylists(songId, playlistIds)`
- **Check if song in playlist:** `isSongInPlaylist(playlistId, songId)`
- **Get playlists containing song:** `getPlaylistIdsContainingSong(songId, userId)`
- **Create playlist with song:** `createPlaylistWithSong(...)`

## Cách Sử Dụng

### 1. Trong Fragment/Activity

```java
public class SongDetailFragment extends Fragment {
    private SongDetailViewModel viewModel;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SongDetailViewModel.class);
        
        // Load song detail
        long songId = getArguments().getLong("song_id");
        long userId = getCurrentUserId();
        viewModel.loadSongDetail(songId, userId);
        
        // Observe data
        observeViewModel();
    }
    
    private void observeViewModel() {
        viewModel.getCurrentSong().observe(this, song -> {
            // Update UI with song info
        });
        
        viewModel.getIsLiked().observe(this, isLiked -> {
            // Update like button state
        });
        
        viewModel.getLikeCount().observe(this, count -> {
            // Update like count display
        });
        
        // ... other observers
    }
    
    private void onLikeButtonClick() {
        viewModel.toggleLike(songId, userId);
    }
    
    private void onAddCommentClick(String content) {
        viewModel.addComment(songId, userId, content);
    }
    
    private void onAddToPlaylistClick(List<Long> selectedPlaylistIds) {
        viewModel.addSongToPlaylists(songId, selectedPlaylistIds);
    }
}
```

### 2. Direct Repository Usage

```java
// Nếu không muốn dùng ViewModel, có thể dùng repository trực tiếp
SongDetailRepository repository = new SongDetailRepository(application);

// Get comprehensive song data
repository.getSongDetailData(songId, userId)
    .thenAccept(data -> {
        if (data != null) {
            // Update UI on main thread
            runOnUiThread(() -> {
                updateSongInfo(data.song);
                updateLikeButton(data.isLiked, data.likeCount);
                updateCommentCount(data.commentCount);
            });
        }
    });

// Toggle like
repository.toggleSongLike(songId, userId)
    .thenAccept(isLiked -> {
        runOnUiThread(() -> updateLikeButton(isLiked));
    });
```

## Đặc Điểm Kỹ Thuật

### Thread Safety
- Tất cả database operations chạy trên background threads sử dụng ExecutorService
- UI updates thông qua LiveData hoặc callbacks trên main thread
- Không block UI thread

### Error Handling
- Try-catch blocks trong tất cả async operations
- Error messages được expose qua LiveData
- Graceful degradation cho non-critical operations

### Performance Optimization
- Batch operations cho multiple playlist additions
- Helper classes để return multiple values trong một call
- Efficient queries với proper indexing

### Memory Management
- Proper cleanup trong `onCleared()` methods
- ExecutorService shutdown khi không cần thiết
- Repository lifecycle management

## Testing

Để test các operations:

```java
// Test trong unit test hoặc integration test
@Test
public void testSongLikeToggle() {
    // Given
    long songId = 1L;
    long userId = 1L;
    
    // When
    Boolean result = repository.toggleSongLike(songId, userId).get();
    
    // Then
    assertTrue(result); // First toggle should return true (liked)
    
    // When toggle again
    Boolean result2 = repository.toggleSongLike(songId, userId).get();
    
    // Then
    assertFalse(result2); // Second toggle should return false (unliked)
}
```

## Kết Luận

Backend logic cho Song Detail functionality đã được implement hoàn chỉnh với:
- ✅ Complete CRUD operations cho song likes, comments, và playlist operations
- ✅ Thread-safe background operations
- ✅ Comprehensive error handling
- ✅ Performance optimizations
- ✅ Clean architecture với separation of concerns
- ✅ Ready for UI integration

Code đã sẵn sàng để integrate với UI components và có thể mở rộng dễ dàng cho các features tương lai.
