# Upload/Edit Song Screen Documentation

## 📋 Tổng quan

Màn hình Upload/Edit Song cho phép người dùng tải lên bài hát mới hoặc chỉnh sửa bài hát đã có. Màn hình này được thiết kế theo kiến trúc MVVM và tích hợp hoàn toàn với hệ thống Room database của ứng dụng.

### 🏗️ Kiến trúc tổng thể

```
┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐
│   UploadSongActivity│────│ UploadSongViewModel │────│   SongRepository    │
│   (View Layer)      │    │  (ViewModel Layer)  │    │  (Data Layer)       │
└─────────────────────┘    └─────────────────────┘    └─────────────────────┘
           │                           │                           │
           │                           │                           │
           ▼                           ▼                           ▼
┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐
│    UI Components    │    │     LiveData        │    │    Room Database    │
│  (Material Design)  │    │   Observers         │    │   (SQLite)          │
└─────────────────────┘    └─────────────────────┘    └─────────────────────┘
```

## 🎯 Chức năng chính

### 1. Upload bài hát mới

**Flow hoạt động:**
1. Người dùng chọn file audio (.mp3) từ device storage
2. Tùy chọn chọn ảnh bìa cho bài hát
3. Nhập metadata: title, description, genre, visibility
4. Hệ thống validate input data
5. Copy files vào internal storage
6. Tạo Song entity và lưu vào Room database
7. Hiển thị success message và đóng màn hình

**Code example:**
```java
// Khởi tạo màn hình upload
Intent intent = UploadSongActivity.createUploadIntent(this);
startActivity(intent);
```

### 2. Edit bài hát hiện có

**Flow hoạt động:**
1. Load dữ liệu bài hát từ database theo songId
2. Populate các trường input với dữ liệu hiện có
3. Cho phép người dùng cập nhật thông tin
4. Validate và save changes vào database
5. Hiển thị nút Delete để xóa bài hát

**Code example:**
```java
// Khởi tạo màn hình edit
Intent intent = UploadSongActivity.createEditIntent(this, songId);
startActivity(intent);
```

### 3. Delete bài hát

**Flow hoạt động:**
1. Hiển thị confirmation dialog
2. Nếu user xác nhận, xóa bài hát khỏi database
3. Xóa các files liên quan (audio, cover art)
4. Hiển thị success message và đóng màn hình

## 🏛️ Code Structure

### UploadSongActivity.java
**Vai trò:** View layer - Quản lý UI và user interactions

**Thành phần chính:**
- UI components initialization với findViewById
- ActivityResultLauncher cho file selection
- LiveData observers cho state management
- Input validation và error handling

```java
public class UploadSongActivity extends AppCompatActivity {
    private UploadSongViewModel viewModel;
    
    // UI components
    private ShapeableImageView imageViewCoverArt;
    private Button buttonSelectAudio;
    private TextInputEditText editTextTitle;
    // ... other UI components
    
    // File selection launchers
    private ActivityResultLauncher<Intent> audioPickerLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
}
```

### UploadSongViewModel.java
**Vai trò:** ViewModel layer - Business logic và state management

**Thành phần chính:**
- Repository integration
- LiveData cho UI state
- Background operations với ExecutorService
- Input validation logic

```java
public class UploadSongViewModel extends AndroidViewModel {
    private final SongRepository songRepository;
    private final AuthManager authManager;
    
    // LiveData for UI state
    private final MutableLiveData<Song> currentSong;
    private final MutableLiveData<Boolean> isEditMode;
    private final MutableLiveData<Boolean> isLoading;
    private final MutableLiveData<String> errorMessage;
}
```

### FileUtils.java
**Vai trò:** Utility class - File operations và metadata extraction

**Chức năng:**
- Copy files từ external storage vào internal storage
- Extract file name và size từ URI
- Get audio duration với MediaMetadataRetriever
- File validation (audio/image types)

```java
public class FileUtils {
    public static String copyFileToInternalStorage(Context context, Uri sourceUri, String fileName);
    public static long getAudioDuration(Context context, Uri audioUri);
    public static String getFileName(Context context, Uri uri);
    public static boolean isValidAudioFile(Context context, Uri audioUri);
}
```

## 📱 UI Components

### Layout Structure (activity_upload_song.xml)
```xml
ScrollView
└── ConstraintLayout
    ├── ShapeableImageView (Cover Art)
    ├── Button (Select Audio File)
    ├── TextInputLayout (Title)
    ├── TextInputLayout (Description)
    ├── TextInputLayout (Genre Dropdown)
    ├── MaterialSwitch (Visibility)
    └── LinearLayout (Save/Delete Buttons)
```

### Material Design Components
- **ShapeableImageView**: Cover art với rounded corners
- **TextInputLayout**: Outlined style cho consistent design
- **AutoCompleteTextView**: Genre selection dropdown
- **MaterialSwitch**: Public/Private visibility toggle
- **Material Buttons**: Primary và outline styles

## 🔄 LiveData Observers

### State Management
```java
// Edit mode observer
viewModel.getIsEditMode().observe(this, isEditMode -> {
    updateUIForMode(isEditMode);
});

// Loading state observer
viewModel.getIsLoading().observe(this, isLoading -> {
    buttonSave.setEnabled(!isLoading);
    buttonDelete.setEnabled(!isLoading);
});

// Error handling observer
viewModel.getErrorMessage().observe(this, errorMessage -> {
    if (errorMessage != null) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        viewModel.clearErrorMessage();
    }
});
```

## 📂 File Handling

### Audio File Selection
```java
private void selectAudioFile() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("audio/*");
    audioPickerLauncher.launch(intent);
}
```

### File Storage Strategy
1. **External Storage**: User selects files từ device storage
2. **Internal Storage**: App copies files vào private directory
3. **Database**: Lưu internal file paths trong Song entity
4. **Cleanup**: Xóa files khi delete bài hát

### File Path Structure
```
/data/data/com.g3.soundify_musicplayer/files/
├── audio/
│   ├── song_audio_1.mp3
│   ├── song_audio_2.mp3
│   └── ...
└── images/
    ├── cover_image_1.jpg
    ├── cover_image_2.png
    └── ...
```

## ✅ Validation Rules

### Input Validation
- **Title**: Required, max 100 characters
- **Description**: Optional, max 500 characters
- **Audio File**: Required for new songs
- **Genre**: Selected from predefined list
- **Cover Art**: Optional, image files only

### Error Messages
```java
private boolean validateInput(String title, String description) {
    if (title == null || title.trim().isEmpty()) {
        errorMessage.setValue("Vui lòng nhập tiêu đề bài hát");
        return false;
    }
    
    if (title.length() > 100) {
        errorMessage.setValue("Tiêu đề quá dài (tối đa 100 ký tự)");
        return false;
    }
    
    if (description != null && description.length() > 500) {
        errorMessage.setValue("Mô tả quá dài (tối đa 500 ký tự)");
        return false;
    }
    
    return true;
}
```

## 🔧 Usage Examples

### 1. Launch Upload Screen
```java
// Từ MainActivity hoặc Library Fragment
public void openUploadScreen() {
    Intent intent = UploadSongActivity.createUploadIntent(this);
    startActivity(intent);
}
```

### 2. Launch Edit Screen
```java
// Từ Song Detail hoặc My Songs list
public void editSong(long songId) {
    Intent intent = UploadSongActivity.createEditIntent(this, songId);
    startActivity(intent);
}
```

### 3. Integration với Navigation
```java
// Trong Fragment
private void navigateToUpload() {
    Intent intent = UploadSongActivity.createUploadIntent(requireActivity());
    startActivity(intent);
}
```

## 🛠️ Technical Requirements

### Permissions (AndroidManifest.xml)
```xml
<!-- File access permissions -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
    android:maxSdkVersion="28" />

<!-- Media access on Android 13+ -->
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

### Dependencies (build.gradle.kts)
```kotlin
dependencies {
    // Material Design
    implementation("com.google.android.material:material:1.11.0")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.7.2")
    annotationProcessor("androidx.room:room-compiler:2.7.2")
    
    // ViewBinding (enabled)
    buildFeatures {
        viewBinding = true
    }
}
```

### Activity Registration
```xml
<activity 
    android:name=".ui.upload.UploadSongActivity"
    android:parentActivityName=".MainActivity"
    android:theme="@style/Theme.Soundifymusicplayer"/>
```

## 🎨 Resources

### String Resources
- Upload/Edit titles và labels
- Validation error messages
- Success/failure toasts
- Genre array cho dropdown

### Drawable Resources
- `ic_audio_file.xml`: Audio file icon
- `ic_default_cover_art.xml`: Default cover art placeholder

### Layout Resources
- `activity_upload_song.xml`: Main layout với Material Design

## 🚀 Testing

### Demo Activity
```java
// UploadDemoActivity.java - Để test functionality
public class UploadDemoActivity extends AppCompatActivity {
    // Simple buttons để test upload và edit modes
}
```

### Test Scenarios
1. **Upload new song**: Select audio, add metadata, save
2. **Edit existing song**: Load data, modify, update
3. **Delete song**: Confirmation dialog, remove from database
4. **Validation**: Test required fields và length limits
5. **File handling**: Test different audio/image formats

## 📝 Notes

### Performance Considerations
- File operations chạy trên background threads
- LiveData observers tự động cleanup khi Activity destroyed
- ExecutorService được shutdown trong ViewModel.onCleared()

### Error Handling
- Network errors (nếu có remote storage)
- File access permissions
- Storage space limitations
- Invalid file formats

### Future Enhancements
- Batch upload multiple songs
- Audio waveform visualization
- Cloud storage integration
- Advanced metadata extraction (ID3 tags)

---

**Tác giả:** PRM391 Development Team  
**Ngày cập nhật:** 2025-01-18  
**Version:** 1.0
