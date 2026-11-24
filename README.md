# Echo Music

离线优先的 Android 本地音乐播放器（课程项目）。需求、分工与里程碑见 [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md)，目标架构与文件落位见 [`docs/PROJECT_STRUCTURE.md`](docs/PROJECT_STRUCTURE.md)。

## 当前进度

### `chore/w2-data-foundation`：W2 数据层地基（本分支）

- **Room 地基**：接入 Room 2.8.5 + KSP，导出数据库 schema v1。
- **持久化表**：`playlist`、`playlist_song`、`favorite`、`play_history`。
- **Repository**：`PlaylistRepositoryImpl` 已在 `AppContainer.playlistRepository` 注入，支持歌单 CRUD、收藏和播放历史数据流。
- **仍待替换**：`MusicRepository` 仍是 `FakeMusicRepository`，下一步接 MediaStore 扫描与运行时音频权限。
- 数据层边界、表结构和接线方式见 [`docs/DATA_LAYER.md`](docs/DATA_LAYER.md)。

### `main`：W1 工程与界面骨架

已完成：

- **构建底座**：Version Catalog 统一依赖，开启 ViewBinding / BuildConfig，minSdk 24 / targetSdk 37
- **权限与清单**：`READ_MEDIA_AUDIO`、旧版存储权限、`POST_NOTIFICATIONS`、前台服务与 `mediaPlayback` 类型，`PlaybackService` 已注册
- **导航骨架**：单 Activity + Navigation Component，底部五个入口（首页 / 媒体库 / 歌单 / 搜索 / 设置）+ 全屏播放页
- **媒体库列表**：RecyclerView + `ListAdapter`（DiffUtil），封面 / 标题 / 艺术家 / 时长，空状态提示
- **播放页**：封面、进度拖动、播放暂停、上一首 / 下一首、播放模式切换，状态由 `StateFlow` 驱动
- **播放链路**：Media3 `MediaSessionService`（ExoPlayer + MediaSession）+ `MediaController`，支持后台播放、通知栏控制、音频焦点、拔耳机暂停
- **搜索页**：输入防抖 250ms，按歌名 / 艺术家 / 专辑过滤

仍是占位（按分工继续）：

| 模块 | 现状 | 下一步 |
|---|---|---|
| `data/repository/FakeMusicRepository` | 8 首假数据，都指向 `res/raw/demo_tone.wav` | B：换成 MediaStore 扫描 + 权限封装 |
| 歌单页 / 设置页 | 歌单页仍是静态占位；Room 歌单数据层已在 `chore/w2-data-foundation` 提供 | C 补歌单列表、详情与设置项，ViewModel 接 `PlaylistRepository` |
| 播放队列 / 歌词 / 深色模式切换 | 未实现 | A 补队列；C 补歌词解析与主题 |
| 收藏 / 最近播放 | Room 表与 Repository 已在数据层分支提供，UI 未接 | C 接收藏列表与最近播放页；播放状态变化由 UI/协调层记录历史 |

## 运行

```bash
# 1. 确认 local.properties 里的 sdk.dir 指向本机 Android SDK
# 2. 编译调试包
./gradlew :app:assembleDebug

# 3. 安装到已连接的设备 / 模拟器
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

首次进入「媒体库」会看到 8 首示例歌曲（内置音频片段），点击任意一首即可播放。

## 代码结构

```
app/src/main/java/com/example/android_echo_music/
├── EchoMusicApplication.kt   进程入口
├── AppContainer.kt           手写依赖容器（替换点集中在这里）
├── MainActivity.kt           单 Activity + NavHost
├── data/
│   ├── model/                Song / Playlist / PlayHistory 共享契约
│   ├── db/                   Room Entity / DAO / Database
│   └── repository/           MusicRepository 假实现 + PlaylistRepository Room 实现
├── playback/
│   ├── PlaybackService.kt    MediaSessionService（后台播放 / 通知栏）
│   ├── PlayerController.kt   对外唯一控制接口
│   ├── PlaybackState.kt / PlayMode.kt
│   └── MediaSessionPlayerController.kt
├── ui/
│   ├── home / library / playlist / search / settings / player
│   └── common/AppExt.kt
└── common/TimeFormat.kt
```

依赖方向固定为 `ui → PlayerController / MusicRepository 接口 → data/model`，UI 层不出现 Media3、Room、MediaStore 类型。

## 验证记录

### `chore/w2-data-foundation`

- `./gradlew :app:assembleDebug :app:testDebugUnitTest`：通过
- `./gradlew :app:lintDebug :app:assembleDebugAndroidTest`：通过
- `./gradlew :app:assembleDebugAndroidTest`：通过；当前无连接设备，`connectedDebugAndroidTest` 待真机/模拟器执行

### `main`（W1）

- `./gradlew :app:assembleDebug`：通过
- `./gradlew :app:lintDebug`：通过（剩余为 Overdraw 等提示级警告）