# Playlist Detail Screen Functionality Documentation

## 📋 Tổng quan

Màn hình Playlist Detail cho phép người dùng xem chi tiết playlist, quản lý danh sách bài hát, và thực hiện các thao tác như play, shuffle, edit (nếu là owner). Màn hình được thiết kế theo kiến trúc MVVM với Material Design 3.

## 🏗️ Kiến trúc tổng thể

### Component Relationship Diagram
```
┌─────────────────────────┐    ┌─────────────────────────┐    ┌─────────────────────────┐
│  PlaylistDetailActivity │────│ PlaylistDetailViewModel │────│   PlaylistRepository    │
│     (View Layer)        │    │   (Business Logic)      │    │    (Data Access)        │
└─────────────────────────┘    └─────────────────────────┘    └─────────────────────────┘
           │                              │                              │
           │                              │                              │
           ▼                              ▼                              ▼
┌─────────────────────────┐    ┌─────────────────────────┐    ┌─────────────────────────┐
│   PlaylistSongAdapter   │    │       LiveData          │    │    Room Database        │
│   (RecyclerView UI)     │    │     Observers           │    │   (SQLite Storage)      │
└─────────────────────────┘    └─────────────────────────┘    └─────────────────────────┘
```

### Core Components

| Component | Responsibility | Key Features |
|-----------|---------------|--------------|
| **PlaylistDetailActivity** | UI Controller & User Interactions | CollapsingToolbar, Action buttons, Navigation |
| **PlaylistDetailViewModel** | Business Logic & State Management | Data loading, Stats calculation, CRUD operations |
| **PlaylistSongAdapter** | RecyclerView Management | Drag-drop, Context menus, Owner-based UI |
| **PlaylistRepository** | Data Access Layer | Database operations, Song-Playlist relationships |

## 🎯 Chức năng chi tiết

### 1. Playlist Display

#### Header Information
```java
// PlaylistDetailActivity.java - updatePlaylistInfo()
private void updatePlaylistInfo(Playlist playlist) {
    if (playlist != null) {
        playlistName.setText(playlist.getName());
        playlistCover.setImageResource(R.drawable.placeholder_album_art);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(playlist.getName());
        }
    }
}
```

#### Stats Calculation
```java
// PlaylistDetailViewModel.java - updatePlaylistStats()
public void updatePlaylistStats(List<Song> songs) {
    songCount.setValue(songs.size());
    
    // Calculate total duration
    long totalMs = 0;
    for (Song song : songs) {
        if (song.getDurationMs() != null) {
            totalMs += song.getDurationMs();
        }
    }
    
    totalDuration.setValue(TimeUtils.formatDuration((int) totalMs));
}
```

#### Info String Generation
```java
// PlaylistDetailViewModel.java - getPlaylistInfoString()
public String getPlaylistInfoString() {
    StringBuilder info = new StringBuilder();
    
    // Add owner info
    if (owner != null) {
        info.append("Created by ").append(owner.getDisplayName());
    }
    
    // Add song count and duration
    if (count != null && count > 0) {
        if (info.length() > 0) info.append(" • ");
        info.append(count == 1 ? "1 song" : count + " songs");
    }
    
    if (duration != null && !duration.equals("0:00")) {
        if (info.length() > 0) info.append(" • ");
        info.append(duration);
    }
    
    return info.toString();
}
```

### 2. Songs List Management

#### RecyclerView Setup
```java
// PlaylistDetailActivity.java - setupRecyclerView()
private void setupRecyclerView() {
    adapter = new PlaylistSongAdapter(this);
    adapter.setOnSongActionListener(this);
    
    songsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    songsRecyclerView.setAdapter(adapter);
}
```

#### Song Item Binding
```java
// PlaylistSongAdapter.java - bind()
public void bind(Song song, int position) {
    // Set song info
    songTitle.setText(song.getTitle());
    artistName.setText("Unknown Artist");
    
    // Set duration
    if (song.getDurationMs() != null && song.getDurationMs() > 0) {
        duration.setText(TimeUtils.formatDuration(song.getDurationMs()));
    } else {
        duration.setText("--:--");
    }
    
    // Set cover art
    if (song.getCoverArtUrl() != null && !song.getCoverArtUrl().isEmpty()) {
        Uri coverUri = Uri.parse(song.getCoverArtUrl());
        songCover.setImageURI(coverUri);
    } else {
        songCover.setImageResource(R.drawable.placeholder_album_art);
    }
    
    // Owner-based UI visibility
    dragHandle.setVisibility(isOwner ? View.VISIBLE : View.GONE);
    menuButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
}
```

### 3. Owner Check Logic

#### Ownership Verification
```java
// PlaylistDetailViewModel.java - checkOwnership()
private void checkOwnership(long ownerId) {
    long currentUserId = authManager.getCurrentUserId();
    isOwner.setValue(currentUserId != -1 && currentUserId == ownerId);
}
```

#### UI Updates Based on Ownership
```java
// PlaylistDetailActivity.java - updateOwnershipUI()
private void updateOwnershipUI(boolean isOwner) {
    editPlaylistButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
    fabAddSongs.setVisibility(isOwner ? View.VISIBLE : View.GONE);
}

// PlaylistSongAdapter.java - setIsOwner()
public void setIsOwner(boolean isOwner) {
    this.isOwner = isOwner;
    notifyDataSetChanged(); // Refresh all items to show/hide controls
}
```

### 4. Action Buttons Implementation

#### Play All & Shuffle
```java
// PlaylistDetailActivity.java - setupClickListeners()
private void setupClickListeners() {
    playAllButton.setOnClickListener(v -> playAllSongs());
    shuffleButton.setOnClickListener(v -> shufflePlay());
    editPlaylistButton.setOnClickListener(v -> editPlaylist());
    fabAddSongs.setOnClickListener(v -> addSongsToPlaylist());
}

private void playAllSongs() {
    // TODO: Implement play all functionality
    // This would typically start the music player with the playlist
    Toast.makeText(this, "Play All - Not implemented yet", Toast.LENGTH_SHORT).show();
}
```

#### Button State Management
```java
// PlaylistDetailActivity.java - updateSongsList()
private void updateSongsList(List<Song> songs) {
    boolean isEmpty = songs == null || songs.isEmpty();
    
    // Update action buttons state
    playAllButton.setEnabled(!isEmpty);
    shuffleButton.setEnabled(!isEmpty);
    
    // Show/hide empty state
    emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    songsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
}
```

### 5. Song Management Operations

#### Remove Song with Confirmation
```java
// PlaylistDetailActivity.java - onRemoveSong()
@Override
public void onRemoveSong(Song song, int position) {
    new AlertDialog.Builder(this)
        .setTitle(R.string.remove_song_title)
        .setMessage(getString(R.string.remove_song_message, song.getTitle()))
        .setPositiveButton(R.string.remove_confirm, (dialog, which) -> {
            viewModel.removeSongFromPlaylist(song.getId(), song.getTitle());
        })
        .setNegativeButton(R.string.button_cancel, null)
        .show();
}
```

#### Drag-and-Drop Reordering
```java
// PlaylistSongAdapter.java - moveItem()
public void moveItem(int fromPosition, int toPosition) {
    if (fromPosition < toPosition) {
        for (int i = fromPosition; i < toPosition; i++) {
            Collections.swap(songs, i, i + 1);
        }
    } else {
        for (int i = fromPosition; i > toPosition; i--) {
            Collections.swap(songs, i, i - 1);
        }
    }
    notifyItemMoved(fromPosition, toPosition);
    
    if (listener != null) {
        listener.onMoveSong(fromPosition, toPosition);
    }
}
```

#### Context Menu Actions
```java
// PlaylistSongAdapter.java - showSongMenu()
private void showSongMenu(View anchor, int position) {
    PopupMenu popup = new PopupMenu(context, anchor);
    popup.getMenuInflater().inflate(R.menu.menu_playlist_song, popup.getMenu());
    
    popup.setOnMenuItemClickListener(item -> {
        int itemId = item.getItemId();
        if (itemId == R.id.action_remove_song) {
            if (listener != null) {
                listener.onRemoveSong(song, position);
            }
            return true;
        } else if (itemId == R.id.action_move_up) {
            if (position > 0) {
                moveItem(position, position - 1);
            }
            return true;
        } else if (itemId == R.id.action_move_down) {
            if (position < songs.size() - 1) {
                moveItem(position, position + 1);
            }
            return true;
        }
        return false;
    });
    
    // Enable/disable move options based on position
    popup.getMenu().findItem(R.id.action_move_up).setEnabled(position > 0);
    popup.getMenu().findItem(R.id.action_move_down).setEnabled(position < songs.size() - 1);
    
    popup.show();
}
```

### 6. Real-time Updates với LiveData

#### Observer Setup
```java
// PlaylistDetailActivity.java - setupObservers()
private void setupObservers() {
    // Observe playlist data
    viewModel.getCurrentPlaylist().observe(this, this::updatePlaylistInfo);
    
    // Observe playlist owner
    viewModel.getPlaylistOwner().observe(this, this::updateOwnerInfo);
    
    // Observe ownership status
    viewModel.getIsOwner().observe(this, isOwner -> {
        if (isOwner != null) {
            updateOwnershipUI(isOwner);
            adapter.setIsOwner(isOwner);
        }
    });
    
    // Observe songs list
    viewModel.getSongsInPlaylist().observe(this, this::updateSongsList);
    
    // Observe song count for header
    viewModel.getSongCount().observe(this, count -> {
        if (count != null) {
            songsHeader.setText(viewModel.getSongsCountString());
        }
    });
}
```

#### Data Flow on Changes
```java
// PlaylistDetailViewModel.java - removeSongFromPlaylist()
public void removeSongFromPlaylist(long songId, String songTitle) {
    Future<Void> future = playlistRepository.removeSongFromPlaylist(currentPlaylistId, songId);
    try {
        future.get();
        successMessage.setValue("\"" + songTitle + "\" removed from playlist");
        // LiveData automatically triggers UI update
    } catch (ExecutionException | InterruptedException e) {
        errorMessage.setValue("Error removing song from playlist");
    }
}
```

## 🔄 Code Flow Analysis

### 1. Playlist Loading Flow
```
User opens playlist → Activity gets playlistId from Intent → ViewModel.loadPlaylist() 
→ Repository queries database → LiveData emits playlist data → Observer updates UI
```

### 2. Song List Loading Flow
```
ViewModel.getSongsInPlaylist() → Repository.getSongsInPlaylist() → PlaylistSongDao query
→ LiveData<List<Song>> → Observer updates RecyclerView → Adapter.setSongs() → UI refresh
```

### 3. User Interaction Flow
```
User clicks song → Adapter.OnSongActionListener → Activity.onSongClick() 
→ Start music player (TODO: implement)

User drags song → Adapter.moveItem() → Collections.swap() → notifyItemMoved() 
→ listener.onMoveSong() → ViewModel.updateSongPosition() → Database update
```

### 4. Remove Song Flow
```
User clicks menu → showSongMenu() → PopupMenu → action_remove_song 
→ listener.onRemoveSong() → Activity shows AlertDialog → User confirms 
→ ViewModel.removeSongFromPlaylist() → Repository removes → LiveData updates → UI refresh
```

## 📱 UI Components Breakdown

### Layout Structure
```xml
CoordinatorLayout
├── AppBarLayout
│   └── CollapsingToolbarLayout
│       ├── LinearLayout (Playlist Header)
│       │   ├── ShapeableImageView (Cover Art)
│       │   ├── TextView (Playlist Name)
│       │   └── TextView (Playlist Info)
│       └── Toolbar
├── NestedScrollView
│   └── LinearLayout
│       ├── LinearLayout (Action Buttons)
│       │   ├── Button (Play All)
│       │   └── Button (Shuffle)
│       ├── Button (Edit Playlist - Owner only)
│       └── LinearLayout (Songs Section)
│           ├── TextView (Songs Header)
│           ├── RecyclerView (Songs List)
│           └── LinearLayout (Empty State)
└── FloatingActionButton (Add Songs - Owner only)
```

### Material Design Features
- **CollapsingToolbarLayout**: Parallax scrolling effect
- **ShapeableImageView**: Rounded corners for cover art
- **Material Buttons**: Primary and outlined styles
- **FloatingActionButton**: Add songs action
- **PopupMenu**: Context menu for song actions
- **AlertDialog**: Confirmation dialogs

## 🎯 Key Design Patterns

### 1. **MVVM Pattern**
- **View**: Activity handles UI and user interactions
- **ViewModel**: Manages business logic and data state
- **Model**: Repository and Room entities

### 2. **Observer Pattern**
- LiveData observers for reactive UI updates
- Automatic UI refresh when data changes

### 3. **Adapter Pattern**
- RecyclerView.Adapter bridges data and UI
- ViewHolder pattern for efficient view recycling

### 4. **Strategy Pattern**
- Different UI behavior based on ownership
- Conditional visibility and functionality

## 🚀 Performance Considerations

### 1. **Efficient RecyclerView**
- ViewHolder pattern prevents findViewById() calls
- DiffUtil for smart list updates (can be added)
- Image loading with URI caching

### 2. **Background Operations**
- All database operations on background threads
- ExecutorService in Repository layer
- LiveData handles thread switching

### 3. **Memory Management**
- Proper cleanup in onCleared()
- Weak references in observers
- Resource cleanup in onDestroy()

---

**Tác giả:** PRM391 Development Team  
**Ngày cập nhật:** 2025-01-18  
**Version:** 1.0
