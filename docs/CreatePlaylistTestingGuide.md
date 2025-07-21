# Create Playlist Feature - Testing Guide

## Tổng quan
Hướng dẫn test chức năng Create Playlist đã được implement trong Soundify Music Player app.

## Pre-requisites
- App đã được build thành công (`./gradlew assembleDebug`)
- Device/emulator đã được setup
- APK đã được install

## Test Scenarios

### 1. Basic Create Playlist Flow

#### Test Case 1.1: Successful Playlist Creation
**Steps:**
1. Launch app
2. Login với credentials: `admin` / `123`
3. Navigate to Library tab (bottom navigation)
4. Switch to "My Playlists" tab
5. Verify empty state hiển thị với "Create Playlist" button
6. Click "Create Playlist" button
7. Enter playlist name: "Test Playlist 1"
8. Click "Create" button

**Expected Results:**
- Dialog appears với input field
- After clicking Create: Loading state (button disabled, text "Creating...")
- Success toast: "Playlist 'Test Playlist 1' created successfully"
- Playlist appears trong list với "0 songs"
- Created date formatted correctly (e.g., "Created Dec 21, 2024")

#### Test Case 1.2: Multiple Playlist Creation
**Steps:**
1. Continue from Test Case 1.1
2. Click "Create Playlist" button again
3. Enter playlist name: "Test Playlist 2"
4. Click "Create"

**Expected Results:**
- Second playlist created successfully
- Both playlists visible trong list
- Newest playlist appears at top (sorted by created_at DESC)

### 2. Validation Testing

#### Test Case 2.1: Empty Name Validation
**Steps:**
1. Click "Create Playlist" button
2. Leave input field empty
3. Click "Create" button

**Expected Results:**
- Error toast: "Please enter a playlist name"
- Dialog remains open
- No playlist created

#### Test Case 2.2: Whitespace-only Name
**Steps:**
1. Click "Create Playlist" button
2. Enter only spaces: "   "
3. Click "Create" button

**Expected Results:**
- Error toast: "Please enter a playlist name"
- Dialog remains open
- No playlist created

#### Test Case 2.3: Very Long Name
**Steps:**
1. Click "Create Playlist" button
2. Enter very long name (>100 characters)
3. Click "Create" button

**Expected Results:**
- Playlist created successfully
- Name truncated trong UI với ellipsis
- Full name stored trong database

### 3. UI State Testing

#### Test Case 3.1: Loading State
**Steps:**
1. Click "Create Playlist" button
2. Enter valid name
3. Observe button state when clicking "Create"

**Expected Results:**
- Button immediately disabled
- Button text changes to "Creating..."
- After success: Button enabled again, text back to "Create Playlist"

#### Test Case 3.2: Dialog Cancellation
**Steps:**
1. Click "Create Playlist" button
2. Enter some text
3. Click "Cancel" button

**Expected Results:**
- Dialog closes
- No playlist created
- No error messages

### 4. Data Persistence Testing

#### Test Case 4.1: App Restart
**Steps:**
1. Create a playlist successfully
2. Close app completely
3. Reopen app và login
4. Navigate to Library → My Playlists

**Expected Results:**
- Created playlist still visible
- Song count still "0 songs"
- Created date preserved

#### Test Case 4.2: Tab Switching
**Steps:**
1. Create a playlist
2. Switch to "My Songs" tab
3. Switch back to "My Playlists" tab

**Expected Results:**
- Playlist still visible
- No data loss
- UI state preserved

### 5. Error Handling Testing

#### Test Case 5.1: User Not Logged In
**Steps:**
1. Logout user (if possible)
2. Try to create playlist

**Expected Results:**
- Error toast: "Please login first"
- No playlist created

#### Test Case 5.2: Network/Database Issues
**Steps:**
1. Simulate database error (if possible)
2. Try to create playlist

**Expected Results:**
- Error toast với appropriate message
- Button re-enabled
- User can retry

### 6. Integration Testing

#### Test Case 6.1: Song Count Updates
**Steps:**
1. Create a playlist
2. Add songs to playlist (if functionality available)
3. Return to My Playlists tab

**Expected Results:**
- Song count updates from "0 songs" to actual count
- Count formatted correctly ("1 song" vs "2 songs")

#### Test Case 6.2: Playlist Navigation
**Steps:**
1. Create a playlist
2. Click on playlist item

**Expected Results:**
- Navigation to PlaylistDetailActivity (if implemented)
- Or appropriate placeholder message

### 7. Performance Testing

#### Test Case 7.1: Multiple Rapid Creations
**Steps:**
1. Rapidly create multiple playlists
2. Observe app performance

**Expected Results:**
- No crashes
- All playlists created successfully
- UI remains responsive

#### Test Case 7.2: Large Number of Playlists
**Steps:**
1. Create 20+ playlists
2. Scroll through list

**Expected Results:**
- Smooth scrolling
- All playlists visible
- No memory issues

## Automated Testing Commands

### Build và Install
```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n com.g3.soundify_musicplayer/.data.Activity.SplashActivity
```

### Database Verification
```bash
# Access app database (requires root)
adb shell
su
cd /data/data/com.g3.soundify_musicplayer/databases/
sqlite3 soundpify_database

# Check playlists table
.tables
SELECT * FROM playlists;
SELECT * FROM playlist_songs;
```

## Bug Reporting Template

### Bug Report Format
```
**Title:** [Brief description]

**Steps to Reproduce:**
1. Step 1
2. Step 2
3. Step 3

**Expected Result:**
[What should happen]

**Actual Result:**
[What actually happened]

**Environment:**
- Device: [Device model]
- Android Version: [Version]
- App Version: [Version]

**Screenshots/Logs:**
[Attach if available]
```

## Test Results Checklist

- [ ] Basic playlist creation works
- [ ] Empty name validation works
- [ ] Loading states display correctly
- [ ] Success messages appear
- [ ] Error handling works
- [ ] Data persists after app restart
- [ ] UI updates in real-time
- [ ] Multiple playlists can be created
- [ ] Song count displays correctly
- [ ] Performance is acceptable

## Known Issues
- Lint warnings về deprecated onBackPressed (không ảnh hưởng functionality)
- Room constructor warnings (không ảnh hưởng functionality)

## Next Steps After Testing
1. Fix any bugs discovered
2. Implement playlist navigation
3. Add edit/delete functionality
4. Integrate với media player
5. Add playlist sharing features
