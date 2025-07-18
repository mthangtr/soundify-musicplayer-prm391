# Followers/Following List Screen Documentation

## 📋 Tổng quan

Màn hình Followers/Following List cho phép người dùng xem danh sách những người theo dõi (followers) và những người đang theo dõi (following) của một user cụ thể. Màn hình được thiết kế theo kiến trúc MVVM với TabLayout để chuyển đổi giữa hai danh sách và tích hợp đầy đủ chức năng tìm kiếm, follow/unfollow.

### 🏗️ Kiến trúc tổng thể

```
┌─────────────────────────────┐    ┌─────────────────────────────┐    ┌─────────────────────────────┐
│ FollowersFollowingActivity  │────│ FollowersFollowingViewModel │────│   MusicPlayerRepository     │
│      (View Layer)           │    │    (ViewModel Layer)        │    │     (Data Layer)            │
└─────────────────────────────┘    └─────────────────────────────┘    └─────────────────────────────┘
           │                                     │                                     │
           │                                     │                                     │
           ▼                                     ▼                                     ▼
┌─────────────────────────────┐    ┌─────────────────────────────┐    ┌─────────────────────────────┐
│     UserFollowAdapter       │    │        LiveData             │    │      UserFollowDao          │
│   (RecyclerView Adapter)    │    │      Observers              │    │    (Room Database)          │
└─────────────────────────────┘    └─────────────────────────────┘    └─────────────────────────────┘
```

## 🎯 Chức năng chính

### 1. Hiển thị danh sách Followers/Following
- **TabLayout**: Chuyển đổi giữa Followers và Following
- **Dynamic Tab Titles**: Hiển thị số lượng trong tab title (e.g., "Followers (25)")
- **Real-time Updates**: Cập nhật danh sách khi follow status thay đổi
- **User Information**: Avatar, display name, username, bio cho mỗi user

### 2. Tìm kiếm Users
- **Search Bar**: Tìm kiếm theo display name, username, hoặc bio
- **Real-time Filtering**: Lọc danh sách ngay khi người dùng nhập
- **Clear Search**: Nút clear để xóa tìm kiếm
- **Search Results**: Hiển thị empty state khi không tìm thấy

### 3. Follow/Unfollow Actions
- **Follow Button**: Follow user chưa được theo dõi
- **Following Button**: Unfollow user đang theo dõi
- **Immediate UI Update**: Cập nhật button state ngay lập tức
- **Toast Notifications**: Thông báo thành công/thất bại

### 4. Navigation
- **User Profile**: Click vào user item để mở profile
- **Back Navigation**: Quay lại UserProfileActivity
- **Deep Linking**: Mở với tab cụ thể (Followers/Following)

### 5. State Management
- **Loading States**: Hiển thị loading khi tải dữ liệu
- **Empty States**: Thông báo khi không có followers/following
- **Error States**: Xử lý lỗi với retry button
- **Pull-to-Refresh**: Làm mới danh sách

## 🏛️ Code Structure

### FollowersFollowingActivity.java
**Vai trò:** View layer - Quản lý UI và user interactions

**Thành phần chính:**
- TabLayout setup và tab switching logic
- Search functionality với TextWatcher
- RecyclerView với UserFollowAdapter
- SwipeRefreshLayout cho pull-to-refresh
- State management (loading, empty, error)
- Navigation handling

### FollowersFollowingViewModel.java
**Vai trò:** ViewModel layer - Business logic và state management

**Thành phần chính:**
- LiveData transformations cho followers/following
- Follow/unfollow operations
- Search và filtering logic
- Error handling và success messages
- Tab state management

### UserFollowAdapter.java
**Vai trò:** RecyclerView Adapter - Hiển thị danh sách users

**Thành phần chính:**
- User item binding với avatar, name, username, bio
- Follow button state management
- Search filtering logic
- Click listeners cho user actions
- Real-time follow status updates

## 📱 UI Components

### Layout Structure (activity_followers_following.xml)
```xml
CoordinatorLayout
├── AppBarLayout
│   ├── MaterialToolbar
│   ├── TextInputLayout (Search)
│   └── TabLayout
└── SwipeRefreshLayout
    └── FrameLayout
        ├── RecyclerView (User List)
        ├── LinearLayout (Empty State)
        ├── LinearLayout (Loading State)
        └── LinearLayout (Error State)
```

### User Item Layout (item_user_follow.xml)
```xml
MaterialCardView
└── ConstraintLayout
    ├── ShapeableImageView (Avatar)
    ├── LinearLayout (User Info)
    │   ├── TextView (Display Name)
    │   ├── TextView (Username)
    │   └── TextView (Bio)
    ├── MaterialButton (Follow)
    ├── MaterialButton (Following)
    └── TextView (Mutual Follow Indicator)
```

### Material Design Features
- **TabLayout**: Fixed tabs với indicator
- **SearchView**: Outlined TextInputLayout với search icon
- **SwipeRefreshLayout**: Pull-to-refresh functionality
- **MaterialCardView**: Elevated cards cho user items
- **CircularProgressIndicator**: Loading states
- **MaterialButton**: Follow/Following buttons

## 🔄 LiveData Flow

### Data Observers
```java
// Followers data
viewModel.getFollowers().observe(this, followers -> {
    if (currentTab == TAB_FOLLOWERS) {
        updateUsersList(followers);
        updateTabTitle(TAB_FOLLOWERS, followers.size());
    }
});

// Following data
viewModel.getFollowing().observe(this, following -> {
    if (currentTab == TAB_FOLLOWING) {
        updateUsersList(following);
        updateTabTitle(TAB_FOLLOWING, following.size());
    }
});

// Current user's following for button states
viewModel.getCurrentUserFollowing().observe(this, currentUserFollowing -> {
    List<Long> followingIds = extractUserIds(currentUserFollowing);
    adapter.setFollowingIds(followingIds);
});
```

### State Management
```java
// Loading state
viewModel.getIsLoading().observe(this, isLoading -> {
    swipeRefreshLayout.setRefreshing(isLoading);
    if (isLoading) showLoadingState();
    else hideLoadingState();
});

// Error handling
viewModel.getErrorMessage().observe(this, error -> {
    if (error != null) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        showErrorState();
    }
});
```

## 🔧 Usage Examples

### 1. Launch from UserProfileActivity
```java
// Open Followers tab
private void viewFollowers() {
    User user = viewModel.getCurrentUser().getValue();
    if (user != null) {
        Intent intent = FollowersFollowingActivity.createIntent(
            this, user.getId(), user.getUsername(), 
            FollowersFollowingActivity.TAB_FOLLOWERS);
        startActivity(intent);
    }
}

// Open Following tab
private void viewFollowing() {
    User user = viewModel.getCurrentUser().getValue();
    if (user != null) {
        Intent intent = FollowersFollowingActivity.createIntent(
            this, user.getId(), user.getUsername(), 
            FollowersFollowingActivity.TAB_FOLLOWING);
        startActivity(intent);
    }
}
```

### 2. Launch from any Activity
```java
// Basic launch
Intent intent = FollowersFollowingActivity.createIntent(context, userId);
startActivity(intent);

// With username hint
Intent intent = FollowersFollowingActivity.createIntent(context, userId, username);
startActivity(intent);

// With specific tab
Intent intent = FollowersFollowingActivity.createIntent(
    context, userId, username, FollowersFollowingActivity.TAB_FOLLOWING);
startActivity(intent);
```

### 3. Handle Follow Actions
```java
@Override
public void onFollowClick(User user, int position, boolean isCurrentlyFollowing) {
    viewModel.toggleFollowStatus(user);
    
    // Update adapter immediately for better UX
    adapter.updateFollowStatus(user.getId(), !isCurrentlyFollowing);
}
```

## 🛠️ Technical Implementation

### Search Functionality
```java
private void setupSearch() {
    editTextSearch.addTextChangedListener(new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            adapter.filterUsers(s.toString());
            updateEmptyState();
        }
    });
}
```

### Tab Switching
```java
tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        currentTab = tab.getPosition();
        viewModel.setCurrentTab(currentTab);
        updateCurrentTabData();
        clearSearch();
    }
});
```

### Follow Status Updates
```java
public void updateFollowStatus(long userId, boolean isFollowing) {
    if (isFollowing) {
        if (!followingIds.contains(userId)) {
            followingIds.add(userId);
        }
    } else {
        followingIds.remove(userId);
    }
    
    // Update specific item
    for (int i = 0; i < filteredUsers.size(); i++) {
        if (filteredUsers.get(i).getId() == userId) {
            notifyItemChanged(i);
            break;
        }
    }
}
```

## 📋 State Management

### Empty States
- **No Followers**: "No Followers" với subtitle khác nhau cho own profile vs other profile
- **No Following**: "Not Following Anyone" với subtitle phù hợp
- **No Search Results**: "No Results" với query string
- **Loading**: Circular progress indicator với "Loading users..."
- **Error**: Error icon với retry button

### UI States
```java
private void updateEmptyState() {
    boolean isEmpty = adapter.getItemCount() == 0;
    boolean isSearching = !editTextSearch.getText().toString().trim().isEmpty();
    
    if (isEmpty && !isSearching) {
        emptyStateTitle.setText(viewModel.getEmptyStateTitle(currentTab));
        emptyStateSubtitle.setText(viewModel.getEmptyStateSubtitle(currentTab, viewModel.isOwnProfile()));
        showEmptyState();
    } else if (isEmpty && isSearching) {
        emptyStateTitle.setText(R.string.no_search_results_title);
        emptyStateSubtitle.setText(getString(R.string.no_search_results_subtitle, query));
        showEmptyState();
    } else {
        hideEmptyState();
    }
}
```

## 🎨 Resources

### String Resources
- Tab titles với count: "Followers (%d)", "Following (%d)"
- Empty state messages cho different scenarios
- Error messages cho network/database errors
- Success messages cho follow actions
- Search hints và placeholders

### Drawable Resources
- `ic_search.xml`: Search icon
- `ic_group.xml`: Empty state icon
- `ic_error.xml`: Error state icon
- `bg_mutual_follow.xml`: Background cho mutual follow indicator

### Color Resources
- `primary_blue`: Accent color cho tabs và buttons
- `primary_blue_light`: Background cho mutual follow indicator
- `text_tertiary`: Subtle text color
- `accent_red`: Error states

## 🚀 Performance Considerations

### Efficient RecyclerView
- ViewHolder pattern với proper view recycling
- Efficient filtering với background operations
- Glide image loading với caching
- Proper lifecycle management

### Memory Management
- ExecutorService shutdown trong onCleared()
- Repository shutdown để cleanup resources
- Proper LiveData observers lifecycle

### Network Efficiency
- Pull-to-refresh chỉ khi cần thiết
- Batch operations cho follow/unfollow
- Caching user data để tránh duplicate requests

---

**Tác giả:** PRM391 Development Team  
**Ngày cập nhật:** 2025-01-18  
**Version:** 1.0
