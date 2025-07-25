# 🎵 Soundify Music Player - PRM391 Project

## ✅ **Simple Architecture - Zero Queue Rule**

### **Core Components:**
- **MediaPlayerRepository**: Manages `currentSongList`, `currentIndex`, `currentListTitle`
- **QueueFragment**: Simple UI to display and manage current song list
- **MiniPlayer/FullPlayer**: Basic playback controls

### **Key Features:**
- ✅ Play songs from any screen (Home, Search, Playlist, etc.)
- ✅ Queue management with drag & drop reordering
- ✅ Next/Previous navigation within current list
- ✅ Real-time playback state synchronization

### **Zero Queue Rule:**
- No complex queue operations (add/remove individual songs)
- No shuffle/repeat modes
- Only replace entire list and play
- Simple index-based navigation

### **Usage:**
```java
// Play from any screen:
songDetailViewModel.replaceQueueAndPlay(songList, "Playlist Name", startIndex);

// Queue management:
mediaPlayerRepository.jumpToIndex(position);
mediaPlayerRepository.moveItemInList(fromPos, toPos);
```

### **Architecture:**
- **MVVM Pattern**: Clean separation with Repository pattern
- **Room Database**: Local data persistence
- **Zero Queue Rule**: Simplified queue management
- **Direct Repository Access**: QueueFragment → MediaPlayerRepository (no complex layers)

**Simple, clean, and works perfectly for student demo! 🚀**
