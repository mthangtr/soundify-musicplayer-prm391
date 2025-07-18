# Edit Profile Screen Documentation

## 📋 Tổng quan

Màn hình Edit Profile cho phép người dùng chỉnh sửa thông tin hồ sơ cá nhân, bao gồm ảnh đại diện, thông tin cơ bản và thay đổi mật khẩu. Màn hình được thiết kế theo kiến trúc MVVM và tích hợp hoàn toàn với hệ thống Room database và AuthManager hiện có.

### 🏗️ Kiến trúc tổng thể

```
┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐
│  EditProfileActivity│────│ EditProfileViewModel│────│   UserRepository    │
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

### 1. Chỉnh sửa ảnh đại diện
- **Chọn từ camera**: Chụp ảnh mới bằng camera
- **Chọn từ thư viện**: Chọn ảnh có sẵn từ gallery
- **Xem trước**: Hiển thị ảnh đã chọn ngay lập tức
- **Lưu trữ**: Copy ảnh vào internal storage và cập nhật database

### 2. Chỉnh sửa thông tin cơ bản
- **Display Name**: Tên hiển thị (bắt buộc, tối đa 100 ký tự)
- **Username**: Tên người dùng (bắt buộc, unique, 3-50 ký tự)
- **Email**: Địa chỉ email (bắt buộc, unique, định dạng hợp lệ)
- **Bio**: Tiểu sử (tùy chọn, tối đa 500 ký tự)

### 3. Thay đổi mật khẩu
- **Mật khẩu hiện tại**: Xác minh mật khẩu cũ
- **Mật khẩu mới**: Tối thiểu 6 ký tự
- **Xác nhận mật khẩu**: Phải khớp với mật khẩu mới

### 4. Xác thực theo thời gian thực
- **Validation ngay lập tức**: Kiểm tra lỗi khi người dùng nhập
- **Kiểm tra trùng lặp**: Username và email không được trùng với người khác
- **Thông báo lỗi**: Hiển thị lỗi rõ ràng cho từng trường

## 🏛️ Code Structure

### EditProfileActivity.java
**Vai trò:** View layer - Quản lý UI và user interactions

**Thành phần chính:**
- UI components initialization với findViewById
- ActivityResultLauncher cho image selection
- LiveData observers cho state management
- Real-time validation với TextWatcher
- Permission handling cho camera/storage

### EditProfileViewModel.java
**Vai trò:** ViewModel layer - Business logic và state management

**Thành phần chính:**
- Repository integration
- LiveData cho UI state và validation errors
- Background operations với ExecutorService
- Input validation logic
- Image handling logic

### FileUtils.java (Updated)
**Vai trò:** Utility class - File operations

**Cập nhật:**
- Hỗ trợ copy cả audio và image files
- Tự động phân loại file theo extension
- Delete file functionality cho cleanup

## 📱 UI Components

### Layout Structure (activity_edit_profile.xml)
```xml
CoordinatorLayout
├── AppBarLayout
│   └── MaterialToolbar
└── NestedScrollView
    └── ConstraintLayout
        ├── MaterialCardView (Profile Picture)
        ├── TextInputLayout (Display Name)
        ├── TextInputLayout (Username)
        ├── TextInputLayout (Email)
        ├── TextInputLayout (Bio)
        ├── TextInputLayout (Current Password)
        ├── TextInputLayout (New Password)
        ├── TextInputLayout (Confirm Password)
        └── LinearLayout (Action Buttons)
```

### Material Design Features
- **ShapeableImageView**: Circular profile picture với camera overlay
- **TextInputLayout**: Outlined style với error states
- **MaterialButton**: Primary và outline styles
- **CircularProgressIndicator**: Loading state
- **AlertDialog**: Image selection và unsaved changes

## 🔄 LiveData Observers

### State Management
```java
// User data observer
viewModel.getCurrentUser().observe(this, user -> {
    if (user != null) {
        populateUserData(user);
    }
});

// Loading state observer
viewModel.getIsLoading().observe(this, isLoading -> {
    loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    buttonSave.setEnabled(!isLoading);
});

// Validation error observers
viewModel.getDisplayNameError().observe(this, error -> {
    textInputDisplayName.setError(error);
});
```

## 📂 File Handling

### Image Selection Flow
1. **Permission Check**: Kiểm tra READ_EXTERNAL_STORAGE permission
2. **Selection Dialog**: Camera hoặc Gallery
3. **Image Processing**: Copy vào internal storage
4. **UI Update**: Hiển thị ảnh đã chọn
5. **Database Update**: Lưu đường dẫn mới

### File Storage Strategy
```
/data/data/com.g3.soundify_musicplayer/files/
├── audio/
│   └── (existing audio files)
└── images/
    ├── avatar_1_1642345678901.jpg
    ├── avatar_2_1642345678902.jpg
    └── ...
```

## ✅ Validation Rules

### Input Validation
- **Display Name**: Required, max 100 characters
- **Username**: Required, 3-50 characters, alphanumeric + underscore, unique
- **Email**: Required, valid email format, unique
- **Bio**: Optional, max 500 characters
- **Password**: Min 6 characters (if changing)
- **Confirm Password**: Must match new password

### Real-time Validation
```java
editTextUsername.addTextChangedListener(new TextWatcher() {
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        viewModel.validateUsername(s.toString());
    }
});
```

## 🔧 Usage Examples

### 1. Launch from UserProfileActivity
```java
private void editProfile() {
    Intent intent = new Intent(this, EditProfileActivity.class);
    startActivity(intent);
}
```

### 2. Launch from any Activity
```java
public void openEditProfile() {
    Intent intent = new Intent(this, EditProfileActivity.class);
    startActivity(intent);
}
```

## 🛠️ Technical Requirements

### Permissions (AndroidManifest.xml)
```xml
<!-- File access permissions -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.CAMERA" />
```

### Dependencies
- Material Design Components
- Room Database
- Glide for image loading
- ViewBinding (optional)

### Activity Registration
```xml
<activity 
    android:name=".ui.profile.EditProfileActivity"
    android:parentActivityName=".ui.profile.UserProfileActivity"
    android:theme="@style/Theme.Soundifymusicplayer"/>
```

## 🎨 Resources

### String Resources
- Form labels và hints
- Validation error messages
- Success/failure toasts
- Dialog titles và messages

### Drawable Resources
- `ic_camera.xml`: Camera icon
- `ic_arrow_back.xml`: Back navigation
- `placeholder_avatar.xml`: Default avatar

### Color Resources
- `primary_blue`: Accent color
- `black_overlay_30`: Camera overlay
- `black_overlay_50`: Loading overlay

## 🚀 Testing

### Test Scenarios
1. **Edit basic info**: Change display name, username, email, bio
2. **Change avatar**: Select from camera/gallery
3. **Change password**: Verify current password, set new password
4. **Validation**: Test all validation rules
5. **Unsaved changes**: Test back navigation with changes
6. **Loading states**: Test during save operation

## 📝 Integration Notes

### AuthManager Integration
- Sử dụng `getCurrentUserId()` để load user data
- Cập nhật session khi username thay đổi
- Hash password với SHA-256

### UserRepository Integration
- Sử dụng existing methods: `getUserById()`, `update()`
- Background operations với ExecutorService
- LiveData cho reactive updates

### FileUtils Integration
- Copy images vào internal storage
- Delete old avatar files
- Support cả audio và image files

## 🔮 Future Enhancements

### Planned Features
- Crop/resize image functionality
- Multiple avatar options
- Social media integration
- Profile visibility settings
- Account deletion option

### Performance Improvements
- Image compression
- Lazy loading
- Better error handling
- Offline support

---

**Tác giả:** PRM391 Development Team  
**Ngày cập nhật:** 2025-01-18  
**Version:** 1.0
