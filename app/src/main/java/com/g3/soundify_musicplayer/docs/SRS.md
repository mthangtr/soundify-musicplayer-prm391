# Dự án Android Music Player (PRM391) – Đặc tả

**Màn hình & Phạm vi CRUD**

**Mục tiêu:** Xây dựng app Android (Java, API 30-35) mô phỏng kiểu SoundCloud tối giản cho sinh viên PRM391: người dùng có thể đăng nhạc, nghe nhạc của người khác, comment (không reply), like comment, tạo & quản lý playlist, lưu bài hát vào playlist, follow user khác.

**Phạm vi:** Ưu tiên CRUD rõ ràng, dễ chấm điểm, dễ demo offline (có thể mô phỏng backend bằng SQLite/Room + mock file URL nội bộ). Có thể mở rộng backend REST/Spring Boot về sau.

-----

## 1\. Phạm vi & Giả định ban đầu

Để tránh scope creep, trước khi đi vào chi tiết màn hình ta thống nhất vài giả định. Bạn có thể chỉnh lại:

| \# | Hạng mục | Giả định Base (demo PRM391) | Ghi chú mở rộng |
| :--- | :--- | :--- | :--- |
| 1 | Kiểu lưu file audio | Lưu file cục bộ trên thiết bị + đường dẫn URI; demo streaming giả lập bằng MediaPlayer từ local raw/URI | Bản nâng cao: upload lên server/ Firebase Storage. |
| 2 | Backend | Bản base: SQLite/Room local để dễ demo offline. | Mở rộng: Spring Boot REST + Retrofit sync. |
| 3 | Auth | Đơn giản: Đăng ký nội bộ (email/username + password băm giả lập). Không OAuth. | Có thể thêm đăng nhập Google/ Firebase Auth sau. |
| 4 | Upload nhạc | Người dùng chọn file (MP3), nhập metadata: title, description, genre, visibility (public/ unlisted). | Có thể phân tích ID3, upload chunk. |
| 5 | Comment | Chỉ 1 cấp (flat). Chủ bài + người dùng thường đều comment. | Không reply thread. Có like/ unlike comment. |
| 6 | Playlist | Playlist user-owned; nhiều bài hát; có thể public/private. | Chia collaborative playlist sau. |
| 7 | Follow | Một chiều: user A follow user B. | Gợi ý feed theo following. |
| 8 | Like song? | Không bắt buộc theo yêu cầu; ta sẽ gợi ý thêm optional SongLike cho dễ demo CRUD. | Bạn chọn có/không. |
| 9 | UI pattern | Bottom Nav 3 tab: Home, Search, Library. Mini-player persistent. | Chuẩn pattern dễ chấm. |
| 10 | ΑΡΙ 30-35 | Sử dụng AndroidX, ViewModel, LiveData/ Flow, Room, RecyclerView. | Có thể dùng Jetpack Compose? Nếu bạn muốn. (hiện mặc định View XML). |

-----

## 2\. Danh sách Entity Cốt lõi

Đây là nền cho DB schema và các màn hình. Tôi đánh dấu **(CRUD bắt buộc)** nơi ta cần thể hiện ít nhất 1 thao tác C/R/U/D trong app để đạt yêu cầu học phần.

### 2.1 User (CRUD bắt buộc)

- `id` (PK)
- `username` (unique)
- `displayName`
- `email`
- `passwordHash`
- `avatarUrl` (nullable)
- `bio` (nullable)
- `createdAt`

### 2.2 Song (CRUD bắt buộc)

- `id` (PK)
- `uploaderId` (FK -\> User) (phải biết ai đã upload)
- `title`
- `description`
- `audioUrl` (local path URI hoặc remote URL)
- `coverArtUrl` (nullable)
- `genre`
- `durationMs` (nullable; có thể lấy sau khi load MediaMetadataRetriever)
- `isPublic` (bool)
- `createdAt`

### 2.3 Playlist (CRUD bắt buộc)

- `id` (PK)
- `ownerId` (FK -\> User)
- `name`
- `description`
- `isPublic` (bool)
- `createdAt`

### 2.4 PlaylistSong (join N-N)

- `playlistId` (FK -\> Playlist)
- `songId` (FK-\> Song)
- `position` (int; optional)
- **PK**: composite (`playlistId`, `songId`) hoặc surrogate ID.

### 2.5 Follow (user-user)

- `followerId` (FK -\> User)
- `followeeId` (FK -\> User)
- `createdAt`
- **PK**: composite (`followerId`, `followeeId`).

### 2.6 Comment

- `id` (PK)
- `songId` (FK-\> Song)
- `userId` (FK -\> User)
- `content`
- `createdAt`
- `updatedAt` (nullable)

### 2.7 CommentLike

- `commentId` (FK -\> Comment)
- `userId` (FK -\> User)
- `createdAt`
- **PK**: composite (`commentId`, `userId`) để tránh like trùng.

### 2.8 (Tùy chọn) SongLike / SongFavorite

Nếu bạn muốn có danh sách bài hát đã like (kiểu Library). Nếu không, dùng Playlist "Liked" mặc định.

-----

## 3\. Sơ đồ Quan hệ (ER – mô tả chữ)

- **User (1) -\< Song(N)**: \#1 user upload nhiều bài hát
- **User (1) -\< Playlist(N)**: \#1 user có nhiều playlist
- **Playlist(N) \>\< Song (N)**: \# qua PlaylistSong
- **User (N) \>\< User (N)**: \# Follow (followerId -\> followeeId)
- **Song(1) \< Comment(N)**: \# nhiều comment trên 1 bài hát
- **Comment(N) \>\< User (N)**: \# Like comment qua CommentLike

-----

## 4\. Kiến trúc Điều hướng Tổng thể

App theo pattern 1 `Activity` (MainActivity) + nhiều `Fragment` (Home, Search, Library, Profile...) với `BottomNavigationView`.

**Thanh điều hướng đáy (Bottom Nav Tabs):**

1.  **Home** - feed bài hát mới / từ người theo dõi.
2.  **Search** - tìm bài hát, user, playlist.
3.  **Library** - nhạc của tôi (My Songs, My Playlists, Liked/Mục đã lưu).

**Mini Player** cố định ở dưới (trên BottomNav) hoặc dạng bottom sheet expandable sang màn hình Player full.

**Auth (Login/Register)** nằm trước `MainActivity` (Activity riêng).

-----

## 5\. Danh sách Màn hình (\>=12)

Mỗi màn hình ghi: Mục đích, Entity liên quan, Hành động CRUD chính, Điều hướng vào/ra, Ghi chú kỹ thuật.
**Legend CRUD ký hiệu:**

* C = Create
* R = Read
* U = Update
* D = Delete

### 5.1 Splash / App Init Screen

* **Mục đích:** Khởi động app, load cấu hình, kiểm tra session đã đăng nhập chưa.
* **Entity:** (none trực tiếp) - đọc token user local.
* **CRUD:** R config.
* **Điều hướng:** → Login nếu chưa auth; → Home nếu đã auth.
* **Ghi chú:** Có thể dùng Lottie animation.

### 5.2 Login Screen

* **Mục đích:** Người dùng đăng nhập.
* **Entity:** User.
* **CRUD:** R xác thực user (check credential). Optional: U lưu token.
* **Điều hướng:** → Register; → Home.
* **Ghi chú:** Validate input; show demo user auto-fill.

### 5.3 Register Screen

* **Mục đích:** Tạo tài khoản người dùng mới.
* **Entity:** User.
* **CRUD:** C User (username, email, passwordHash).
* **Điều hướng:** → Login; → Edit Profile (hoặc Home) sau khi tạo.
* **Ghi chú:** Check trùng username/email.

### 5.4 Home Feed Screen (Bottom Tab \#1)

* **Mục đích:** Hiển thị danh sách bài hát mới (hoặc từ user theo dõi).
* **Entity:** Song, User, Follow.
* **CRUD:** R Song list; R Follow để lọc feed.
* **Điều hướng:** → Song Detail/Player; → User Profile khi chạm avatar.
* **Ghi chú:** RecyclerView + paging.

### 5.5 Song Detail + Full Player Screen

* **Mục đích:** Phát bài hát; xem metadata; danh sách comment; hành động.
* **Entity:** Song, User (uploader), Comment, CommentLike, (PlaylistSong add), (SongLike optional).
* **CRUD:** R Song; C Comment; U Comment (sửa của mình); D Comment (xóa của mình); C/D CommentLike; C thêm vào Playlist.
* **Điều hướng:** → Home; popup Add to Playlist; → Uploader Profile.
* **Ghi chú:** MediaPlayer/ExoPlayer (AndroidX Media3). Đồng bộ progress bar.

### 5.6 Mini Player (Persistent Component)

* **Mục đích:** Điều khiển Play/Pause/Next/Prev nhanh.
* **Entity:** Queue (tạm), Song hiện tại.
* **CRUD:** R trạng thái; U trạng thái playback.
* **Điều hướng:** tap → Song Detail.

### 5.7 Search Screen (Bottom Tab \#2)

* **Mục đích:** Tìm kiếm bài hát / user / playlist.
* **Entity:** Song, User, Playlist.
* **CRUD:** R truy vấn.
* **Điều hướng:** → Song Detail; → User Profile; → Playlist Detail.
* **Ghi chú:** Tab nội bộ 3 danh mục hoặc filter chip.

### 5.8 Library Screen (Bottom Tab \#3, dạng ViewPager 2 Tab)

Chứa 3 sub-tab:

1.  **My Songs** (bài hát tôi upload) - CRUD Song.
2.  **My Playlists** - CRUD Playlist.
3.  **Liked / Saved** - nếu dùng Playlist "Yêu thích" mặc định hoặc bảng `SongLike`.

<!-- end list -->

* **Entity:** Song, Playlist, PlaylistSong, SongLike (optional).
* **CRUD:** C/U/D Song; C/U/D Playlist; C/D PlaylistSong.
* **Điều hướng:** → Upload Song; → Playlist Detail; → Song Detail.

### 5.9 Upload Song Screen / Edit Song Screen

* **Mục đích:** Chọn file audio, nhập metadata, upload.
* **Entity:** Song.
* **CRUD:** C Song; U Song (edit metadata); D Song (delete).
* **Điều hướng:** Library/My Songs; → Song Detail sau khi tạo.
* **Ghi chú:** Lấy file từ SAF (Storage Access Framework); hiển thị waveform optional.

### 5.10 Playlist Detail Screen

* **Mục đích:** Xem danh sách bài hát trong playlist; phát tất cả; quản lý.
* **Entity:** Playlist, PlaylistSong, Song.
* **CRUD:** R playlist; U playlist (đổi tên, public/private); C thêm bài; D gỡ bài; D playlist.
* **Điều hướng:** Library/My Playlists; Add Song: launch selector; → Song Detail.
* **Ghi chú:** Drag reorder (optional).

### 5.11 Select Songs for Playlist Dialog/Screen

* **Mục đích:** Chọn nhiều bài để thêm vào playlist.
* **Entity:** Song[..](..).
* **CRUD:** R danh sách; C PlaylistSong.
* **Điều hướng:** → Playlist Detail; multi-select confirm.
* **Ghi chú:** Checkbox multi-select RecyclerView.

### 5.12 User Profile Screen (Public Profile)

* **Mục đích:** Xem thông tin user khác; follow/unfollow; xem các bài hát & playlist public của họ.
* **Entity:** User, Follow, Song, Playlist.
* **CRUD:** C/D Follow; R Song/Playlist.
* **Điều hướng:** → từ Home/Search; → Song Detail; → Playlist Detail.
* **Ghi chú:** Hiển thị follower/following count.

### 5.13 Edit Profile Screen (Own Profile Settings)

* **Mục đích:** Cập nhật avatar, displayName, bio.
* **Entity:** User.
* **CRUD:** U User; D account (optional).
* **Điều hướng:** → Settings hoặc menu profile.

### 5.14 Followers / Following List Screen

* **Mục đích:** Xem danh sách user mình theo dõi hoặc theo dõi mình.
* **Entity:** Follow, User.
* **CRUD:** R follow list; C/D Follow ngay trong list (toggle).
* **Điều hướng:** → User Profile; → User Profile khác.

### 5.15 Settings Screen (Optional nhưng dễ lấy điểm)

* **Mục đích:** Đăng xuất, theme, clear cache, info app.
* **Entity:** User session local.
* **CRUD:** U local prefs; D session.
* **Điều hướng:** → từ menu avatar; → back.

### 5.16 About / Help Screen (Mini, optional)

* **Mục đích:** Giới thiệu nhóm, hướng dẫn sử dụng, thông tin phiên bản.
* **Entity:** none.

-----

## 6\. Mapping Màn hình ↔ CRUD Minh họa (Bảng Tổng hợp)

| Màn hình | User | Song | Playlist | PlaylistSong | Comment | CommentLike | Follow |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Splash** | R(session) | | | | | | |
| **Login** | R(auth) | | | | | | |
| **Register** | C | | | | | | |
| **Home** | R(uploader) | R(list) | | | | | R(follow to filter) |
| **Song Detail** | R(uploader) | R(one) | Add to playlist(C) | C/D playlistSong | C/U/D | C/D | |
| **Library (My Songs)**| R(me) | C/U/D mine | | | | | |
| **Library (My Playlists)**| | | C/U/D | C/D | | | |
| **Upload Song** | | C/U/D | | | | | |
| **Playlist Detail** | | R | U/D | C/D | | | |
| **Select Songs** | | R | | C | | | |
| **User Profile** | R | R | R | | | | C/D |
| **Edit Profile** | U/D | | | | | | |
| **Followers/ Following**| R | | | | | | C/D |
| **Settings** | U local | | | | | | |

-----

## 7\. Luồng Người Dùng Chính (User Flows)

### 7.1 Đăng ký → Đăng nhập → Upload bài hát đầu tiên

1.  Mở app → Splash → Login.
2.  Chọn "Đăng ký" → nhập email/username/password → tạo User.
3.  Điều hướng về Home hoặc Edit Profile để cập nhật avatar.
4.  Vào Library → tab My Songs → nút "+" → Upload Song Screen.
5.  Chọn file mp3 → nhập title/desc/genre → Lưu → Song được tạo (uploaderId = user hiện tại).
6.  Trở lại My Songs list thấy bài mới.

### 7.2 Nghe nhạc & Comment

1.  Từ Home, chọn bài hát → Song Detail/Player.
2.  Phát nhạc; mini player chạy.
3.  Scroll comment: nhập comment → C Comment.
4.  Chạm icon tim comment của người khác → C CommentLike; chạm lại → D CommentLike.

### 7.3 Tạo playlist & thêm bài

1.  Library → My Playlists → "+" → nhập tên playlist → C Playlist.
2.  Mở Playlist Detail → Add Songs → Select Songs list → chọn bài → Confirm → C PlaylistSong.
3.  Trong Playlist Detail: swipe left item để Remove → D PlaylistSong.
4.  Menu đổi tên playlist → U Playlist.

### 7.4 Follow user khác & xem feed

1.  Search user → User Profile.
2.  Nhấn Follow → C Follow.
3.  Về Home feed đặt filter "Following" để xem bài của user đã follow.
4.  Unfollow trong profile hoặc Followers list → D Follow.

-----

## 8\. Thiết kế DB (Phiên bản SQLite Demo)

Nếu bạn đồng ý với entity ở mục 2, tôi sẽ sinh toàn bộ câu lệnh SQL chuẩn `CREATE TABLE` (có FK, `ON DELETE CASCADE` nơi hợp lý) + script `INSERT` mẫu để bạn tạo ER Diagram trong Draw.io/dbdiagram.io.

### 8.1 Kiểu dữ liệu SQLite gợi ý

- `INTEGER` cho id auto-increment.
- `TEXT` cho chuỗi.
- `INTEGER` (0/1) cho boolean.
- `INTEGER` millis epoch cho thời gian hoặc `TEXT` ISO8601.

### 8.2 Khóa ngoại & Cascade

- Xóa User → nên cascade Song, Playlist, Follow liên quan? (có thể restrict để tránh mất dữ liệu). Với bài demo: `ON DELETE CASCADE` cho Follow, CommentLike, Comment; Song có thể set `ON DELETE CASCADE`.
- `PlaylistSong` cascade theo Playlist & Song.

Bạn hãy nói rõ mong muốn để tôi cấu hình đúng.

-----

## 9\. API REST (Nếu cần Spring Boot backend)

Tôi liệt kê endpoint RESTful tối giản (dạng /api/v1/...). Có thể dùng JSON.

### 9.1 Auth

- `POST /auth/register` {username, email, password}
- `POST /auth/login` {username, password} -\> token

### 9.2 Users

- `GET /users/{id}`
- `PUT /users/{id}` (edit profile)
- `GET /users/{id}/songs`
- `GET /users/{id}/playlists`
- `GET /users/{id}/followers` / `following`

### 9.3 Songs

- `POST /songs` (upload metadata + file multipart)
- `GET /songs/{id}`
- `PUT /songs/{id}`
- `DELETE /songs/{id}`
- `GET /songs?query=...&uploader=...`

### 9.4 Playlists

- `POST /playlists`
- `GET /playlists/{id}`
- `PUT /playlists/{id}`
- `DELETE /playlists/{id}`
- `POST /playlists/{id}/songs` {songId}
- `DELETE /playlists/{id}/songs/{songId}`

### 9.5 Comments

- `POST /songs/{songId}/comments`
- `GET /songs/{songId}/comments`
- `PUT /comments/{id}`
- `DELETE /comments/{id}`

### 9.6 Comment Likes

- `POST /comments/{id}/likes` (current user)
- `DELETE /comments/{id}/likes`

### 9.7 Follow

- `POST /users/{id}/follow` (follow that user)
- `DELETE /users/{id}/follow`

-----

## 10\. Lớp Data & Repository (Android)

Sử dụng pattern MVVM + Repository.

```
Data Layer
├─ local/Room
│  ├─ UserDao, SongDao, PlaylistDao, CommentDao, FollowDao, ...
│  └─ Entities @Entity(tableName="...") mapping DB columns
├─ remote/ApiService (Retrofit) - optional
└─ Repository hợp nhất (ưu tiên cache -> network)
```

ViewModel cho từng màn hình: `HomeViewModel`, `SongDetailViewModel`, `LibraryViewModel`, `PlaylistViewModel`, `ProfileViewModel`...

-----

## 11\. Thành phần Kỹ thuật Chính

| Thành phần | Thư viện Gợi ý | Ghi chú |
| :--- | :--- | :--- |
| **Playback** | Media3 ExoPlayer | Hỗ trợ streaming & local file. |
| **Image load** | Glide hoặc Coil | Glide dễ cho Java. |
| **DB** | Room ORM | Sinh DAO + LiveData. |
| **DI** | Hilt hoặc thủ công | Hilt bonus điểm. |
| **Async** | Kotlin Flow? Bạn dùng Java → LiveData + Executor/Coroutines Java wrappers. | |
| **UI** | RecyclerView + DiffUtil | Chuẩn PRM391. |
| **Auth** | SharedPreferences token | Đơn giản. |

-----

## 12\. Ma trận Phân rã Công việc (WBS / Sprint)

**Sprint 0 - Khởi tạo**

* Tạo project, module, dependency.
* Model entity & Room DB.
* Khung Navigation (Login → MainActivity + BottomNav).

**Sprint 1 - User/Auth**

* Register + Login + lưu session.
* Edit Profile.
* Avatar load.

**Sprint 2 - Song Upload & Playback**

* Upload chọn file + metadata.
* Danh sách My Songs.
* ExoPlayer + Mini-player + Song Detail.

**Sprint 3 - Playlist**

* CRUD Playlist.
* Add/remove bài hát.
* Phát playlist.

**Sprint 4 - Comment & Like Comment**

* Hiển thị comment theo bài.
* Thêm comment.
* Chỉnh sửa/xóa comment của mình.
* Like/unlike comment.

**Sprint 5 - Follow & Feed**

* Follow/unfollow user.
* Home feed filter theo Following.
* Profile public/Followers list.

**Sprint 6 - Polish & Demo**

* Search toàn cục.
* Settings, About.
* Seed dữ liệu demo.
* Kiểm thử API mức cơ bản.

-----

## 13\. Kiểm thử & Dữ liệu Seed Demo

Để dễ chấm bài, tạo script chèn nhanh 3-5 user, 10 bài hát (file stub), 3 playlist, vài follow, vài comment.

* Có thể đóng gói file mp3 sample dung lượng nhỏ (\<200KB) để commit.
* Khi cài app lần đầu: nếu DB trống hỏi "Import sample data?".

-----

## 14\. Các Điểm Chấm Điểm (Rubric dự kiến PRM391 – ước lượng)

Mỗi trường có rubric khác nhau; tham khảo chung:

* Cấu trúc project rõ ràng (Activity/Fragment, Adapter, Model) - 10%
* Chức năng Auth cơ bản - 10%
* CRUD trên \>=3 thực thể với UI (Song, Playlist, Comment) - 30%
* Phát nhạc hoạt động - 15%
* Điều hướng + UX hợp lý – 10%
* Tìm kiếm / Lọc - 10%
* Bonus: Follow, Like comment, mini-player, Dark mode - 15%

-----

## 15\. Mock UI Wireframe Nhanh (ASCII)

**Bottom Nav Layout**

```
+----------------------------------------+
| AppBar: Logo / Search / Avatar         |
+----------------------------------------+
|                                        |
|      RecyclerView SongCard             |
|  [Cover] Title Uploader Play ►         |
|                                        |
+----------------------------------------+
| MiniPlayer: Song A          [||] [>]  |
+----------------------------------------+
| [Home]   [Search]   [Library]        |
+----------------------------------------+
```

**Song Detail**

```
+----------------------------------------+
|               [CoverArt]               |
|                                        |
| Title                                  |
| Uploader (avatar, Follow btn)          |
| [Play/Pause] [---------O---]           |
| [Add to Playlist] [Share]              |
| -------------------------------------- |
| Comments                               |
| [UserAvatar] comment text    [♥ count] |
| [Add comment box]                      |
+----------------------------------------+
```

**Playlist Detail**

```
+----------------------------------------+
| Playlist Name (edit icon)              |
| Owner                                  |
| [Play All →]    [Add Songs +]          |
| -------------------------------------- |
| List                                   |
| 1. Song A                              |
| 2. Song B                              |
+----------------------------------------+
```

-----

## 16\. Cần Bạn Xác Nhận / Tùy Chỉnh

Hãy phản hồi giúp mình mấy điểm sau để mình sinh tiếp phần SQL CREATE TABLE + INSERT sample (và nếu cần, class Entity + DAO Room):

1.  Backend hay SQLite local? (Giải pháp base PRM391 chọn cái nào?)
2.  Có cần bảng `SongLike` riêng không, hay dùng Playlist "Liked" mặc định?
3.  Có cần trường đếm (cached counts) trong bảng User/Song (vd: `followerCount`, `playCount`), hay tính nội suy khi query?
4.  Chính sách ON DELETE CASCADE: Có cascade xóa dữ liệu con khi xóa cha không? (vd xóa user thì xóa luôn song/comment?)
5.  Muốn script seed demo? Bao nhiêu user/song/playlist? Có file stub mp3 placeholder không?

Chỉ cần trả lời nhanh 5 câu trên; mình sẽ tạo tiếp bước 2: Full SQL chuẩn + quan hệ để bạn copy vào dbdiagram.io/ Draw.io.

-----

## 17\. Ghi chú Ngắn về Khối lượng Code

Để giữ scope trong tầm sinh viên:

* Tránh viết media service phức tạp; dùng Media3 default.
* Không làm streaming chunk; đọc file local/URL trực tiếp.
* API network có thể mock JSON từ GitHub raw.
* Tập trung CRUD UI + Room + Adapter.

**Kết luận**
Bạn đã xác định đúng vấn đề: Song phải có `uploaderId` để biết ai đăng, và như vậy ta mới phân biệt nghệ sĩ/creator & listener (mọi user đều có thể upload, tức "prosumer"). Đối với app PRM391, mọi tài khoản mặc định là user-creator; không cần role riêng Artist vs Listener để giảm phức tạp. Nếu sau này cần phân quyền, chỉ cần thêm cột `role` trong User.

Chờ bạn phản hồi 5 điểm cấu hình ở Mục 16. Sau khi bạn chốt, mình sẽ:

1.  Sinh full SQL (SQLite + MySQL/SQ Server tương đương nếu cần).
2.  Sinh ER script dbdiagram.io.
3.  Đề xuất class Java Entity + DAO Room.
4.  Phác skeleton project Android Studio.

Bạn trả lời nhé\!