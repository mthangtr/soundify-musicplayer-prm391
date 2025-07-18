# Select Songs for Playlist Functionality Documentation

## 📋 Tổng quan

Màn hình "Select Songs for Playlist" cho phép người dùng tìm kiếm, lọc và chọn nhiều bài hát để thêm vào playlist. Màn hình được thiết kế theo kiến trúc MVVM với Material Design 3, hỗ trợ real-time search, multi-selection, và tích hợp seamless với Playlist Detail.

## 🏗️ Kiến trúc tổng thể

### Component Relationship Diagram
```
┌─────────────────────────┐    ┌─────────────────────────┐    ┌─────────────────────────┐
│  PlaylistDetailActivity │────│   SelectSongsActivity   │────│  SelectSongsViewModel   │
│   (Calling Activity)    │    │    (View Controller)    │    │   (Business Logic)     │
└─────────────────────────┘    └─────────────────────────┘    └─────────────────────────┘
           │                              │                              │
           │                              │                              │
           ▼                              ▼                              ▼
┌─────────────────────────┐    ┌─────────────────────────┐    ┌─────────────────────────┐
│  ActivityResultLauncher │    │  SelectableSongAdapter  │    │    SongRepository       │
│   (Result Handling)     │    │   (RecyclerView UI)     │    │   (Data Access)         │
└─────────────────────────┘    └─────────────────────────┘    └─────────────────────────┘
                                          │                              │
                                          │                              │
                                          ▼                              ▼
                               ┌─────────────────────────┐    ┌─────────────────────────┐
                               │    Selection State      │    │    Room Database        │
                               │   (Checkbox UI)         │    │   (SQLite Storage)      │
                               └─────────────────────────┘    └─────────────────────────┘
```

### Core Components

| Component | Responsibility | Key Features |
|-----------|---------------|--------------|
| **SelectSongsActivity** | UI Controller & User Interactions | Search bar, Filter chips, Selection handling |
| **SelectSongsViewModel** | Business Logic & State Management | Filtering, Selection state, Background operations |
| **SelectableSongAdapter** | RecyclerView Management | Checkbox UI, Selection feedback, Item binding |
| **PlaylistDetailActivity** | Integration & Result Handling | ActivityResultLauncher, Success messages |

## 🎯 Chức năng chi tiết

### 1. Search Functionality

#### Real-time Search Implementation
```java
// SelectSongsActivity.java - setupSearchAndFilter()
searchEditText.addTextChangedListener(new TextWatcher() {
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        viewModel.setSearchQuery(s.toString());
    }
    // ... other methods
});
```

#### Search Logic in ViewModel
```java
// SelectSongsViewModel.java - updateFilteredSongs()
if (query != null && !query.trim().isEmpty()) {
    String searchTerm = query.trim().toLowerCase();
    List<Song> temp = new ArrayList<>();
    for (Song song : filtered) {
        String title = song.getTitle() != null ? song.getTitle().toLowerCase() : "";
        String genre = song.getGenre() != null ? song.getGenre().toLowerCase() : "";
        if (title.contains(searchTerm) || genre.contains(searchTerm)) {
            temp.add(song);
        }
    }
    filtered = temp;
}
```

#### Search Performance Features
- **Instant filtering**: No delay between typing and results
- **Case-insensitive**: Searches both title and genre fields
- **Memory efficient**: Uses ArrayList instead of streams for Android compatibility
- **Empty state handling**: Shows "No results for 'query'" message

### 2. Filter System

#### Filter Types Implementation
```java
// SelectSongsViewModel.java - FilterType enum
public enum FilterType {
    ALL_SONGS,    // Show all songs (excluding existing in playlist)
    MY_SONGS,     // Show only current user's songs
    PUBLIC_SONGS  // Show only public songs
}
```

#### Filter Logic
```java
// SelectSongsViewModel.java - updateFilteredSongs()
switch (filter) {
    case MY_SONGS:
        if (currentUserId != -1) {
            List<Song> temp = new ArrayList<>();
            for (Song song : filtered) {
                if (song.getUploaderId() == currentUserId) {
                    temp.add(song);
                }
            }
            filtered = temp;
        } else {
            filtered = new ArrayList<>(); // No user logged in
        }
        break;
    case PUBLIC_SONGS:
        List<Song> temp = new ArrayList<>();
        for (Song song : filtered) {
            if (song.isPublic()) {
                temp.add(song);
            }
        }
        filtered = temp;
        break;
}
```

#### Filter UI with Material Chips
```java
// SelectSongsActivity.java - setupSearchAndFilter()
filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
    int checkedId = checkedIds.get(0);
    SelectSongsViewModel.FilterType filter;
    
    if (checkedId == R.id.chip_all_songs) {
        filter = SelectSongsViewModel.FilterType.ALL_SONGS;
    } else if (checkedId == R.id.chip_my_songs) {
        filter = SelectSongsViewModel.FilterType.MY_SONGS;
    } else if (checkedId == R.id.chip_public_songs) {
        filter = SelectSongsViewModel.FilterType.PUBLIC_SONGS;
    }
    
    viewModel.setFilter(filter);
});
```

### 3. Multi-Selection System

#### Selection State Management
```java
// SelectSongsViewModel.java - toggleSongSelection()
public void toggleSongSelection(long songId) {
    Set<Long> selected = selectedSongIds.getValue();
    if (selected == null) {
        selected = new HashSet<>();
    } else {
        selected = new HashSet<>(selected); // Create new set to trigger observer
    }
    
    if (selected.contains(songId)) {
        selected.remove(songId);
    } else {
        selected.add(songId);
    }
    
    selectedSongIds.setValue(selected);
    selectionCount.setValue(selected.size());
}
```

#### Visual Feedback in Adapter
```java
// SelectableSongAdapter.java - bind()
boolean isSelected = selectedSongIds != null && selectedSongIds.contains(song.getId());
checkbox.setChecked(isSelected);
selectionOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);

// Update item appearance based on selection
float alpha = isSelected ? 0.7f : 1.0f;
songTitle.setAlpha(alpha);
artistName.setAlpha(alpha);
```

#### Selection Counter
```java
// SelectSongsViewModel.java - getSelectionCountString()
public String getSelectionCountString() {
    Integer count = selectionCount.getValue();
    if (count == null || count == 0) {
        return "None selected";
    } else if (count == 1) {
        return "1 selected";
    } else {
        return count + " selected";
    }
}
```

### 4. Exclude Existing Songs

#### Loading Existing Songs
```java
// SelectSongsViewModel.java - loadExistingSongs()
private void loadExistingSongs() {
    if (currentPlaylistId == -1) return;
    
    Future<List<Song>> future = playlistRepository.getSongsInPlaylistSync(currentPlaylistId);
    try {
        List<Song> existingSongs = future.get();
        Set<Long> existingIds = new HashSet<>();
        if (existingSongs != null) {
            for (Song song : existingSongs) {
                existingIds.add(song.getId());
            }
        }
        existingSongIds.setValue(existingIds);
    } catch (ExecutionException | InterruptedException e) {
        existingSongIds.setValue(new HashSet<>());
    }
}
```

#### Filtering Out Existing Songs
```java
// SelectSongsViewModel.java - updateFilteredSongs()
// Exclude existing songs in playlist
if (existing != null && !existing.isEmpty()) {
    List<Song> temp = new ArrayList<>();
    for (Song song : filtered) {
        if (!existing.contains(song.getId())) {
            temp.add(song);
        }
    }
    filtered = temp;
}
```

### 5. Add to Playlist Operation

#### Background Operation
```java
// SelectSongsViewModel.java - addSelectedSongsToPlaylist()
public void addSelectedSongsToPlaylist() {
    List<Song> selectedSongs = getSelectedSongs();
    if (selectedSongs.isEmpty()) {
        errorMessage.setValue("No songs selected");
        return;
    }
    
    isLoading.setValue(true);
    
    // Add songs to playlist in background
    new Thread(() -> {
        try {
            int addedCount = 0;
            for (Song song : selectedSongs) {
                Future<Void> future = playlistRepository.addSongToPlaylist(currentPlaylistId, song.getId());
                future.get();
                addedCount++;
            }
            
            String message = addedCount == 1 ? 
                "1 song added to playlist" : 
                addedCount + " songs added to playlist";
            successMessage.postValue(message);
            
        } catch (Exception e) {
            errorMessage.postValue("Error adding songs to playlist");
        } finally {
            isLoading.postValue(false);
        }
    }).start();
}
```

## 🔄 Code Flow Analysis

### 1. Activity Launch Flow
```
PlaylistDetailActivity.addSongsToPlaylist() → SelectSongsActivity.createIntent() 
→ selectSongsLauncher.launch() → SelectSongsActivity.onCreate() 
→ ViewModel.initializeForPlaylist() → Load existing songs → Setup UI observers
```

### 2. Search Flow
```
User types in search → TextWatcher.onTextChanged() → ViewModel.setSearchQuery() 
→ MediatorLiveData triggers → updateFilteredSongs() → Apply search filter 
→ filteredSongs.setValue() → Observer updates RecyclerView → UI refresh
```

### 3. Filter Flow
```
User clicks filter chip → ChipGroup.OnCheckedStateChangeListener → ViewModel.setFilter() 
→ MediatorLiveData triggers → updateFilteredSongs() → Apply filter logic 
→ filteredSongs.setValue() → Observer updates RecyclerView → UI refresh
```

### 4. Selection Flow
```
User clicks song/checkbox → Adapter.OnSongSelectionListener → Activity.onSongSelectionChanged() 
→ ViewModel.toggleSongSelection() → Update selectedSongIds → Update selectionCount 
→ Observer updates selection counter → Observer updates adapter selection state → Visual feedback
```

### 5. Add Songs Flow
```
User clicks Done → Activity.onDoneClick() → ViewModel.addSelectedSongsToPlaylist() 
→ Background thread → Repository.addSongToPlaylist() for each song → Database insert 
→ Success message → Activity.finish() with result → PlaylistDetailActivity receives result → Show toast
```

## 🔗 Integration với Playlist Detail

### ActivityResultLauncher Setup
```java
// PlaylistDetailActivity.java - setupActivityResultLauncher()
selectSongsLauncher = registerForActivityResult(
    new ActivityResultContracts.StartActivityForResult(),
    result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            int addedCount = result.getData().getIntExtra(SelectSongsActivity.RESULT_ADDED_COUNT, 0);
            if (addedCount > 0) {
                String message = addedCount == 1 ? 
                    "1 song added to playlist" : 
                    addedCount + " songs added to playlist";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        }
    }
);
```

### Launch Integration
```java
// PlaylistDetailActivity.java - addSongsToPlaylist()
private void addSongsToPlaylist() {
    Playlist playlist = viewModel.getCurrentPlaylist().getValue();
    if (playlist != null) {
        Intent intent = SelectSongsActivity.createIntent(this, playlist.getId(), playlist.getName());
        selectSongsLauncher.launch(intent);
    }
}
```

### Result Handling
```java
// SelectSongsActivity.java - success message observer
viewModel.getSuccessMessage().observe(this, successMessage -> {
    if (successMessage != null && !successMessage.isEmpty()) {
        Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();
        viewModel.clearSuccessMessage();
        
        // Return success result
        Intent resultIntent = new Intent();
        Integer count = viewModel.getSelectionCount().getValue();
        resultIntent.putExtra(RESULT_ADDED_COUNT, count != null ? count : 0);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
});
```

## 🚀 Performance Considerations

### 1. Real-time Search Optimization
- **No debouncing needed**: Filtering is fast enough for real-time
- **Memory efficient**: Manual loops instead of Java 8 streams
- **Case-insensitive caching**: toLowerCase() called once per search

### 2. Large Dataset Handling
```java
// Efficient filtering without creating intermediate collections
List<Song> temp = new ArrayList<>();
for (Song song : filtered) {
    if (matchesFilter(song)) {
        temp.add(song);
    }
}
filtered = temp;
```

### 3. Memory Management
```java
// SelectSongsViewModel.java - onCleared()
@Override
protected void onCleared() {
    super.onCleared();
    // Clean up resources
    songRepository.shutdown();
    playlistRepository.shutdown();
}
```

### 4. Background Thread Operations
- **Database operations**: All playlist additions on background threads
- **UI thread safety**: Using postValue() for cross-thread LiveData updates
- **Progress indication**: Loading states prevent multiple operations

## 📱 UI/UX Features

### Material Design Components
```xml
<!-- Search with Material TextInputLayout -->
<com.google.android.material.textfield.TextInputLayout
    style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
    app:startIconDrawable="@drawable/ic_search"
    app:endIconMode="clear_text">

<!-- Filter with Material Chips -->
<com.google.android.material.chip.ChipGroup
    app:singleSelection="true"
    app:selectionRequired="true">
    
    <com.google.android.material.chip.Chip
        style="@style/Widget.MaterialComponents.Chip.Filter" />
```

### Loading States
```java
// SelectSongsActivity.java - updateSongsList()
private void updateSongsList(List<Song> songs) {
    boolean isEmpty = songs == null || songs.isEmpty();
    boolean isLoading = Boolean.TRUE.equals(viewModel.getIsLoading().getValue());
    
    songsRecyclerView.setVisibility(!isEmpty && !isLoading ? View.VISIBLE : View.GONE);
    emptyStateLayout.setVisibility(isEmpty && !isLoading ? View.VISIBLE : View.GONE);
    loadingLayout.setVisibility(isLoading ? View.VISIBLE : View.GONE);
}
```

### Visual Selection Feedback
```xml
<!-- Selection overlay in item layout -->
<View
    android:id="@+id/view_selection_overlay"
    android:background="?attr/colorPrimary"
    android:alpha="0.08"
    android:visibility="gone" />
```

### Responsive Design
- **Bottom action bar**: Fixed position với selection counter
- **Scrollable content**: RecyclerView với proper padding
- **Keyboard handling**: Search field với proper IME options

## 🎯 Key Design Patterns

### 1. **MVVM Pattern**
- **View**: Activity handles UI và user interactions
- **ViewModel**: Manages filtering, selection, và business logic
- **Model**: Repository và Room entities

### 2. **Observer Pattern**
- **MediatorLiveData**: Combines multiple data sources for filtering
- **LiveData observers**: Reactive UI updates
- **Selection state**: Real-time counter updates

### 3. **Strategy Pattern**
- **Filter types**: Different filtering strategies
- **Selection handling**: Toggle vs direct selection

### 4. **Command Pattern**
- **Background operations**: Encapsulated add-to-playlist commands
- **Result handling**: Success/error command execution

---

**Tác giả:** PRM391 Development Team  
**Ngày cập nhật:** 2025-01-18  
**Version:** 1.0
