# Media Player Backend Implementation

## Tổng quan

Đã implement đầy đủ backend logic cho Mini Player và Full Player components với context-aware navigation functionality. Implementation sử dụng MVVM architecture và tích hợp với existing SongDetailRepository và NavigationContext classes.

## Cấu trúc Implementation

### 1. Data Models

#### MediaPlayerState.java
- **CurrentPlaybackState**: Quản lý trạng thái playback hiện tại
- **QueueInfo**: Thông tin về queue và navigation context
- **PlaybackError**: Xử lý lỗi playback
- **PlaybackState enum**: IDLE, LOADING, READY, PLAYING, PAUSED, STOPPED, ERROR
- **RepeatMode enum**: OFF, ONE, ALL

#### PlaybackQueue.java
- **Queue Management**: Add, remove, reorder songs
- **Shuffle Support**: Shuffle/unshuffle với index mapping
- **Navigation**: Previous/next với repeat mode support
- **Context Integration**: Tích hợp với NavigationContext

#### NavigationContext.java
- **4 Navigation Types**: FROM_PLAYLIST, FROM_ARTIST, FROM_SEARCH, FROM_GENERAL
- **Context Data**: Song IDs, current position, context title
- **Helper Methods**: hasPrevious(), hasNext(), getPositionText()

### 2. Repository Layer

#### MediaPlayerRepository.java
Extends SongDetailRepository để có access đến song data operations.

**Core Features:**
- **Playback Control**: play(), pause(), stop(), seekTo(), togglePlayPause()
- **Queue Management**: playSongWithContext(), playNext(), playPrevious()
- **Context-Aware Navigation**: Tự động load songs based on navigation context
- **Shuffle & Repeat**: toggleShuffle(), cycleRepeatMode()
- **Mock ExoPlayer**: Simulation cho demo purposes

**Key Methods:**
```java
// Play song với navigation context
Future<Boolean> playSongWithContext(Song song, NavigationContext context)

// Context-aware navigation
Future<Boolean> playNext()
Future<Boolean> playPrevious()

// Queue operations
Future<Boolean> addToQueue(Song song)
Future<Boolean> removeFromQueue(int position)
```

### 3. ViewModel Layer

#### MiniPlayerViewModel.java
Base ViewModel cho mini player functionality.

**Features:**
- **Playback Controls**: togglePlayPause(), playNext(), playPrevious()
- **UI State Management**: expand/collapse, show/hide player
- **Progress Tracking**: seekToPercentage(), getCurrentPositionMs()
- **Context Integration**: playSong(song, context)

**LiveData Observables:**
- `currentSong`: Song hiện tại
- `isPlaying`, `isPaused`, `isLoading`: Playback states
- `progressPercentage`: Progress 0-100%
- `hasPrevious`, `hasNext`: Navigation availability
- `contextTitle`: Tên context (playlist, artist, etc.)

#### FullPlayerViewModel.java
Extends MiniPlayerViewModel với full-screen features.

**Additional Features:**
- **Enhanced Controls**: shuffle, repeat modes
- **Context Actions**: "View Playlist", "View Artist Profile", etc.
- **UI Toggles**: lyrics, queue display
- **Context Navigation**: loadSongWithContext(), onContextActionClick()

**Additional LiveData:**
- `isShuffleEnabled`: Shuffle state
- `repeatMode`: Current repeat mode
- `positionText`: "3 of 15 songs"
- `contextActionText`: Context button text

## Context-Aware Navigation Implementation

### 4 Navigation Contexts

#### 1. FROM_PLAYLIST
```java
NavigationContext context = NavigationContext.fromPlaylist(
    playlistId, "My Playlist", songIds, currentPosition
);
```
- **Navigation**: Within playlist song order
- **Display**: "3 of 15 songs"
- **Action**: "View Playlist"

#### 2. FROM_ARTIST
```java
NavigationContext context = NavigationContext.fromArtist(
    artistId, "Artist Name", songIds, currentPosition
);
```
- **Navigation**: Through artist's songs
- **Display**: "More from Artist Name"
- **Action**: "View Artist Profile"

#### 3. FROM_SEARCH
```java
NavigationContext context = NavigationContext.fromSearch(
    "search query", songIds, currentPosition
);
```
- **Navigation**: Through search results
- **Display**: "Search: 'query'"
- **Action**: "Back to Search Results"

#### 4. FROM_GENERAL
```java
NavigationContext context = NavigationContext.fromGeneral(
    "Recently Played", songIds, currentPosition
);
```
- **Navigation**: Through related/recent songs
- **Display**: Context title
- **Action**: "Back to Browse"

## Usage Examples

### 1. Playing Song from Playlist

```java
// In PlaylistFragment
MiniPlayerViewModel miniPlayer = new ViewModelProvider(this).get(MiniPlayerViewModel.class);

// Create navigation context
List<Long> playlistSongIds = getPlaylistSongIds();
NavigationContext context = NavigationContext.fromPlaylist(
    playlistId, playlistName, playlistSongIds, selectedPosition
);

// Play song with context
miniPlayer.playSong(selectedSong, context);
```

### 2. Full Player with Context

```java
// In FullPlayerFragment
FullPlayerViewModel fullPlayer = new ViewModelProvider(this).get(FullPlayerViewModel.class);

// Load song with context
fullPlayer.loadSongWithContext(songId, navigationContext);

// Observe context changes
fullPlayer.getPositionText().observe(this, positionText -> {
    // Update "3 of 15 songs" display
});

fullPlayer.getContextActionText().observe(this, actionText -> {
    // Update context button text
});
```

### 3. Context-Aware Navigation

```java
// Previous/Next buttons automatically use context
fullPlayer.playPrevious(); // Goes to previous song in current context
fullPlayer.playNext();     // Goes to next song in current context

// Context action button
fullPlayer.onContextActionClick(); // Navigate back to source
```

## Technical Features

### Thread Safety
- Tất cả database operations chạy trên background threads
- UI updates thông qua LiveData trên main thread
- ExecutorService cho async operations

### Error Handling
- Comprehensive error handling với PlaybackError class
- Error messages exposed qua LiveData
- Graceful degradation cho failed operations

### Performance Optimization
- Efficient queue management với lazy loading
- Smart shuffle algorithm với index mapping
- Background position updates

### Mock Implementation
- Simulated ExoPlayer cho demo purposes
- Mock progress updates every second
- Realistic playback state transitions

## Integration Points

### With Existing Code
- **SongDetailRepository**: Song data operations
- **NavigationContext**: Context-aware navigation
- **Room Database**: Song, Playlist, User entities
- **MVVM Architecture**: LiveData, ViewModel patterns

### Future Integration
- **ExoPlayer**: Replace mock implementation
- **MediaSession**: Android media controls
- **Notifications**: Background playback notifications
- **Audio Focus**: Handle audio interruptions

## Demo Usage

### Basic Playback
```java
// Start playback
miniPlayerViewModel.togglePlayPause();

// Seek to position
miniPlayerViewModel.seekToPercentage(50); // 50%

// Navigation
miniPlayerViewModel.playNext();
miniPlayerViewModel.playPrevious();
```

### Advanced Features
```java
// Shuffle and repeat
fullPlayerViewModel.toggleShuffle();
fullPlayerViewModel.cycleRepeatMode();

// UI toggles
fullPlayerViewModel.toggleLyrics();
fullPlayerViewModel.toggleQueue();
```

## Architecture Benefits

### Separation of Concerns
- **Repository**: Data và business logic
- **ViewModel**: UI state management
- **Fragment**: UI rendering và user interaction

### Scalability
- Easy to extend với new navigation contexts
- Modular design cho future features
- Clean interfaces cho testing

### Maintainability
- Clear separation between mock và real implementation
- Consistent error handling patterns
- Well-documented public APIs

## Kết Luận

Backend logic cho media player đã được implement hoàn chỉnh với:
- ✅ Context-aware navigation cho 4 loại navigation
- ✅ Complete playback state management
- ✅ Queue operations với shuffle/repeat support
- ✅ MVVM architecture với LiveData
- ✅ Thread-safe background operations
- ✅ Mock implementation ready cho demo
- ✅ Clean integration points cho future enhancements

Code đã sẵn sàng để integrate với UI components và có thể dễ dàng thay thế mock implementation bằng real ExoPlayer integration.
