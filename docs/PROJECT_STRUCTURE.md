# Echo Music 项目结构（v1.0）

> 依据：`docs/DEVELOPMENT.md` v0.1 第 3 章（功能清单）、第 5 章（架构与包归属）、第 6 章（数据模型）
> 作用：给三人一份统一的「文件落位图」，谁在哪个包写什么、能依赖谁，都在这里定死，减少合并冲突。
> 包名：`com.example.android_echo_music`　模块：单 `:app`（不拆多模块，课程项目够用）
> 图例：`✔` 规划时已存在　`＋` 规划时待新建　`★` 公共契约（改动需三人确认）　`[A]/[B]/[C]` 模块负责人　`[P2]` 加分项，时间富余再做

> **落地状态（W1 结束）**：`data/model`、`playback`、`ui` 骨架、`common`、`res` 均已按本图创建，图例中的 `＋` 只代表规划初稿，实际以仓库为准；运行进度与占位说明见 [`README.md`](../README.md)。

---

## 1. 仓库顶层

```
android-echo-music/
├── build.gradle.kts              ✔ 根构建脚本（C 维护）
├── settings.gradle.kts           ✔ 模块注册 + 仓库配置（C 维护，注意当前 include(":app") 重复了一次）
├── gradle.properties             ✔ 全局构建参数（C 维护）
├── gradlew / gradlew.bat         ✔ 构建入口
├── gradle/
│   ├── libs.versions.toml        ✔ 版本目录：所有依赖只能从这里加（C 单人修改）
│   └── wrapper/                  ✔ Gradle Wrapper
├── app/                          ✔ 唯一业务模块
├── docs/
│   ├── DEVELOPMENT.md            ✔ 开发文档（需求 / 计划 / 协作规范）
│   └── PROJECT_STRUCTURE.md      ＋ 本文档（结构 / 落位 / 依赖规则）
└── .gitignore                    ✔ 忽略 build/、.idea/、local.properties、*.jks
```

---

## 2. 源码包结构（`app/src/main/java/com/example/android_echo_music/`）

```
com.example.android_echo_music/
├── EchoMusicApplication.kt       ＋ 进程入口：初始化数据库、通知渠道、AppContainer [C]
├── AppContainer.kt               ＋ 手写单例容器（替代 Hilt）：提供 Repository / PlayerController [C 维护，A/B 加 provider]
├── MainActivity.kt               ✔ 单 Activity：NavHostFragment 宿主 + 权限请求回调 [C]
│
├── data/                         # 【B】数据层：只关心「数据从哪来、怎么存」，不碰 UI
│   ├── model/                    # 全组共享的数据契约（只放不可变的纯 Kotlin 数据类）
│   │   ├── Song.kt               ★ MediaStore 歌曲（id/title/artist/album/durationMs/uri/albumArtUri/folder）
│   │   ├── Playlist.kt           ★ 歌单（id/name/coverUri/createdAt/songCount）
│   │   └── PlayHistory.kt        ★ 播放记录（songId/playedAt/playCount）
│   ├── local/                    # 系统媒体库 + 权限
│   │   ├── MediaStoreMusicSource.kt    MediaStore 查询、全量/增量扫描、按目录过滤
│   │   ├── AudioFileFilter.kt          音频 MIME 白名单、时长 <30s 过滤规则（可单测）
│   │   └── AudioPermission.kt          API 33+ → READ_MEDIA_AUDIO；24–32 → READ_EXTERNAL_STORAGE；判断 + 引导
│   ├── db/                       # Room：只存用户产生的数据，歌曲元数据不落库
│   │   ├── EchoMusicDatabase.kt        Database 定义 + 单例构建
│   │   ├── entity/
│   │   │   ├── PlaylistEntity.kt
│   │   │   ├── PlaylistSongEntity.kt   歌单-歌曲多对多（playlistId + songId + orderIndex + 联合主键）
│   │   │   ├── FavoriteEntity.kt
│   │   │   └── PlayHistoryEntity.kt
│   │   └── dao/
│   │       ├── PlaylistDao.kt          歌单 CRUD、排序更新、关联增删
│   │       ├── FavoriteDao.kt
│   │       └── PlayHistoryDao.kt       upsert 累加 playCount、按时间倒序查询
│   └── repository/               # 对外只暴露接口，实现可替换（W1 假实现 → W2 真实实现）
│       ├── MusicRepository.kt    ★ observeSongs() / refresh() / search()
│       ├── MusicRepositoryImpl.kt      MediaStore + 内存缓存 + Flow 合并
│       ├── PlaylistRepository.kt       歌单 / 收藏 / 历史的组合接口
│       ├── PlaylistRepositoryImpl.kt   Room 实现
│       └── FakeMusicRepository.kt      W1 临时假数据，W2 结束后删除
│
├── playback/                     # 【A】播放核心：不依赖 ui 包，不直接读 Room
│   ├── PlaybackService.kt        ＋ MediaSessionService：ExoPlayer + MediaSession，前台服务常驻
│   ├── PlayerController.kt       ★ 对外唯一控制接口（state / play / toggle / next / previous / seekTo / setMode）
│   ├── PlaybackState.kt          ★ 当前歌曲、队列索引、进度、isPlaying、模式、buffering
│   ├── PlayMode.kt               ★ 顺序 / 单曲循环 / 随机（枚举）
│   ├── MediaPlayerController.kt  MediaController 封装：异步连接、Player.Listener、进度轮询 → StateFlow
│   ├── FakePlayerController.kt   W1 临时实现（无声或 ToneGenerator），W2 替换为真实播放
│   ├── queue/
│   │   ├── PlayQueue.kt          队列增删、下一首/上一首计算、随机序生成
│   │   └── QueueSnapshot.kt      队列对外快照（UI 展示用，不可变）
│   ├── SleepTimer.kt             [P2] 睡眠定时器（15/30/60 分钟）
│   └── equalizer/EqualizerController.kt   [P2] AudioEffect 五段均衡器
│
├── ui/                           # 【C】界面层：只通过接口拿数据，不 new Room / MediaStore
│   ├── home/
│   │   └── HomeFragment.kt       首页：快捷入口 + 最近播放横滑
│   ├── library/
│   │   ├── SongListFragment.kt   媒体库歌曲列表（RecyclerView + DiffUtil）
│   │   ├── SongListViewModel.kt  StateFlow<List<Song>>、排序方式、扫描进度
│   │   ├── SongAdapter.kt        ListAdapter：标题/艺术家/时长/封面，当前播放高亮
│   │   ├── AlbumFragment.kt      [P2] 专辑聚合视图
│   │   ├── ArtistFragment.kt     [P2] 艺术家聚合视图
│   │   └── FolderFragment.kt     [P2] 文件夹浏览
│   ├── player/
│   │   ├── PlayerFragment.kt     播放页：封面/唱片、进度条、控制按钮、模式切换
│   │   ├── PlayerViewModel.kt    订阅 PlayerController.state，处理拖动与按钮事件
│   │   └── lyrics/
│   │       ├── LrcParser.kt      .lrc 解析（时间戳 → 行），可单测
│   │       ├── LyricLine.kt      时间戳 + 文本
│   │       └── LyricView.kt      自定义 View：滚动高亮，跟随进度偏差 <300ms
│   ├── queue/
│   │   └── QueueFragment.kt      播放队列弹层（查看 / 下一首播放 / 移除）—— F09 归 A 实现 [A]
│   ├── playlist/
│   │   ├── PlaylistListFragment.kt    歌单列表 + 新建
│   │   ├── PlaylistDetailFragment.kt  歌单详情：歌曲列表、重命名、移除
│   │   ├── PlaylistAdapter.kt
│   │   ├── PlaylistViewModel.kt
│   │   └── AddToPlaylistSheet.kt      长按歌曲「添加到歌单」底部弹层
│   ├── search/
│   │   ├── SearchFragment.kt     搜索框 + 实时结果（防抖 300ms）
│   │   └── SearchViewModel.kt    keyword → 歌曲/专辑/歌单合并结果
│   ├── settings/
│   │   ├── SettingsFragment.kt   主题、默认播放模式、扫描排除目录、关于
│   │   └── SettingsViewModel.kt  基于 SharedPreferences / DataStore 的偏好读写
│   ├── theme/
│   │   └── ThemeController.kt    跟随系统 / 手动切换，Material 3 动态取色（API 31+）
│   └── common/                   # 视图层工具（只服务 UI）
│       ├── ViewExt.kt            ViewBinding 释放、防止重复点击、显隐扩展
│       └── EmptyStateView.kt     统一空状态 / 错误占位（无音乐、权限拒绝、文件失效）
│
├── common/                       # 三方共用工具（改动前在群里说一声）
│   ├── TimeFormat.kt             mm:ss 格式化、时长聚合
│   ├── AppDispatchers.kt         IO / Default 调度器注入（便于单测）
│   └── Result.kt                 统一的成功/失败包装（可选，避免 UI 各处 try-catch）
│
└── widget/                       # [P2] 桌面小组件 [C]
    └── PlayerWidgetProvider.kt   AppWidgetProvider 4x1
```

---

## 3. 资源结构（`app/src/main/res/`）

```
res/
├── layout/
│   ├── activity_main.xml              ✔ 已存在（改为 NavHostFragment 容器）
│   ├── fragment_home.xml
│   ├── fragment_song_list.xml         列表 + 搜索入口 + 排序按钮
│   ├── fragment_player.xml            大封面 / 唱片旋转 / 进度 / 控制区
│   ├── fragment_queue.xml
│   ├── fragment_playlist_list.xml
│   ├── fragment_playlist_detail.xml
│   ├── fragment_search.xml
│   ├── fragment_settings.xml
│   ├── item_song.xml                  标题 / 艺术家 / 时长 / 封面 / 更多
│   ├── item_playlist.xml
│   ├── view_empty_state.xml
│   └── widget_player.xml              [P2]
├── navigation/
│   └── nav_main.xml                   单 Activity 导航图（home / library / playlist / search / settings / player）
├── menu/
│   ├── menu_bottom_nav.xml            底部导航：首页、媒体库、歌单、搜索
│   ├── menu_song_item.xml             收藏、下一首播放、添加到歌单、查看详情
│   └── menu_player_more.xml
├── drawable/                          ic_play / ic_pause / ic_skip_next / ic_skip_previous /
│                                      ic_repeat / ic_repeat_one / ic_shuffle / ic_favorite /
│                                      ic_search / ic_playlist_add / ic_settings / ic_music_note /
│                                      placeholder_album（+ 深色模式变体用 tint / 主题色）
├── mipmap-*/                          ✔ 已有启动图标
├── values/
│   ├── strings.xml                     ✔ 所有文案集中（公共文件，避免多人改冲突）
│   ├── colors.xml                      ✔ 语义色（surface / onSurface / accent…）
│   ├── themes.xml                      ✔ Material 3 主题
│   └── dimens.xml                     ＋ 统一间距、圆角、播放页尺寸
├── values-night/
│   └── themes.xml                      ✔ 深色模式覆盖
└── xml/
    ├── backup_rules.xml                ✔
    ├── data_extraction_rules.xml       ✔
    └── player_widget_info.xml          [P2]
```

---

## 4. 分层依赖规则（评审时逐条对）

```
        ui/**  ──────────►  PlayerController（接口）/ PlaybackState / PlayMode
          │  │
          │  └───────────►  MusicRepository / PlaylistRepository（接口）+ data/model
          │
          └─────────────►  common/**

   playback/**  ────────►  data/model + common/**        （禁止依赖 ui、禁止直接查 Room）
   data/**      ────────►  data/model + common/**        （禁止依赖 ui / playback）
   common/**    ────────►  仅 Android SDK 与三方库       （禁止依赖上面任何业务包）
```

必须遵守的四条：

1. **UI 不碰实现**：Fragment/ViewModel 只认 `MusicRepository`、`PlayerController` 接口，不出现 `MediaStore`、`Room`、`ExoPlayer` 类型。
2. **播放不碰界面**：`playback/**` 里不出现 `Fragment`、`View`、`R.string`（通知文案除外，集中在 Service 内）。
3. **数据不碰播放**：`data/**` 不引用 Media3。
4. **契约先行**：新增/修改 `Song`、`PlaybackState`、`PlayMode`、两个接口 = 三人都要知道，当天口头确认后再提 PR。

---

## 5. 公共契约（第 1 天必须合入，之后只增不改字段）

| 文件 | 内容 | 约定 |
|---|---|---|
| `data/model/Song.kt` | id、title、artist、album、durationMs、uri、albumArtUri、folder | 字段名不分包、不带 UI 字段（如 `isPlaying`） |
| `data/model/Playlist.kt` | id、name、coverUri、createdAt、songCount | songCount 为查询派生值，非存储字段 |
| `data/model/PlayHistory.kt` | songId、playedAt、playCount | playedAt 存毫秒时间戳 |
| `playback/PlaybackState.kt` | currentSong、queueIndex、positionMs、durationMs、isPlaying、mode、isBuffering | 不可变 data class，UI 直接渲染 |
| `playback/PlayMode.kt` | SEQUENTIAL、REPEAT_ONE、SHUFFLE | 顺序即「列表放完停止 / 循环列表」由 PlayQueue 决定，枚举不再细分 |
| `data/repository/MusicRepository.kt` | observeSongs / refresh / search | 接口方法只返回 Flow 或 suspend，不暴露 Cursor |
| `playback/PlayerController.kt` | state / play / toggle / next / previous / seekTo / setMode | 方法均为同步返回，异步结果走 state |

---

## 6. 文件归属与落地周次

| 区域 | 负责人 | 首次落地 | 依赖前置 |
|---|---|---|---|
| `build.gradle.kts`、`libs.versions.toml`、`AndroidManifest.xml` | C（单人改） | W1 第 1 天 | 无 |
| `data/model/**`、`playback/PlayerController.kt` 等契约 | A + B 共同提交 | W1 第 1 天 | 无 |
| `ui/**` 导航骨架、列表页、`common/**` | C | W1 | 契约 |
| `playback/**`（Service、Fake → 真实） | A | W1–W2 | 契约 |
| `data/local/**`、`data/repository/**` | B | W1（假）→ W2（真） | 契约 |
| `data/db/**` | B | W2 | Room 依赖（C 加） |
| `ui/player/**`、`ui/playlist/**`、`ui/search/**`、`ui/settings/**` | C | W2–W3 | 契约 |
| `widget/**`、`playback/equalizer/**`、`playback/SleepTimer.kt` | C / A | W3–W4（P2） | 主流程完成 |

---

## 7. 新文件该放哪（决策表）

| 你要加的东西 | 放到 | 顺带检查 |
|---|---|---|
| 新的系统数据来源（如文件系统、SAF） | `data/local/` | 是否要 `AudioPermission` 新分支 |
| 新表 / 新查询 | `data/db/entity|dao` | 数据库 version 是否 +1、是否需要 Migration |
| 新业务聚合（收藏列表、最近播放） | `data/repository/` | 接口先加，实现后补 |
| 新页面 / 弹层 | `ui/<所属域>/` | `res/layout/fragment_*`、`nav_main.xml` 注册 |
| 播放状态、队列、模式相关 | `playback/`（队列进 `playback/queue/`） | 是否需要同步进 `PlaybackState` |
| 通用工具（格式化、调度器） | `common/` | 群里说一声，避免和 C 重复 |
| 只服务某个界面的小工具 | `ui/common/` | 不要污染 `common/` |
| 单元测试 | `app/src/test/java/.../<被测类>Test.kt` | 纯逻辑（LrcParser、PlayQueue、搜索过滤） |
| 需要真机的测试 | `app/src/androidTest/java/...` | DAO、MediaStore 扫描 |

---

## 8. 需由 C 在 W1 补齐的构建与清单项

- `app/build.gradle.kts`：`buildFeatures { viewBinding = true }`；JDK/Kotlin 版本统一（建议 17）。
- `gradle/libs.versions.toml` 增补：Media3（exoplayer + session）、Room（runtime/ktx + KSP 编译器）、Kotlin Coroutines、Navigation（fragment/ui-ktx）、Coil、Lifecycle（viewmodel/runtime-ktx）、SharedPreferences/DataStore。
- `settings.gradle.kts`：去掉重复的 `include(":app")`。
- `AndroidManifest.xml`：
  - `READ_MEDIA_AUDIO`（33+）、`READ_EXTERNAL_STORAGE`（`maxSdkVersion="32"`）、`POST_NOTIFICATIONS`（33+）
  - `FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_MEDIA_PLAYBACK`（34+）、`WAKE_LOCK`
  - `android:name=".EchoMusicApplication"`
  - `PlaybackService`：`android:foregroundServiceType="mediaPlayback"`、`android:exported="true"`、intent-filter `androidx.media3.session.MediaSessionService`
- 签名与打包：`optimization { enable = false }` 目前关闭混淆；W4 再开 R8 + keep 规则（`app/src/main/keepRules/rules.keep` 已在）。

---

## 9. 第一周落地顺序（3 个 PR）

1. **PR-1 工程底座（C）**：补齐依赖（版本目录）+ ViewBinding + Manifest 权限/Service 声明 + Application 空实现；验收：`./gradlew :app:assembleDebug` 通过。
2. **PR-2 公共契约（A + B）**：`data/model`、`PlaybackState`、`PlayMode`、两个接口、`AppContainer` 骨架；验收：编译通过，接口无实现。
3. **PR-3 可点击 Demo（C + A + B）**：导航骨架 + 假数据列表（`FakeMusicRepository`）+ 假播放器（`FakePlayerController`）+ 点击出声；验收：列表点击进入播放页并出声，返回不崩。
4. **W2 起**：A 用真实 Media3 替换 Fake、B 用真实 MediaStore + Room 替换 Fake，C 只改 UI，不动契约。

> 空目录不会进 Git：每建一个包，必须同时放至少一个文件（哪怕是骨架类），否则 review 时看不到结构。

---

## 10. 完成自检

- [ ] `./gradlew :app:assembleDebug` 通过，无废弃 API 警告堆积
- [ ] `ui/` 下无 `import android.provider.MediaStore` / `androidx.media3.*` / `androidx.room.*`
- [ ] `playback/` 下无 `androidx.fragment.*` / `ViewModel`
- [ ] `data/` 下无 `androidx.media3.*`
- [ ] `nav_main.xml` 中每个 Fragment 都能到达，返回栈行为正确
- [ ] `Song` 字段与 `MediaStore` 查询列一一对应，无「UI 专用字段」
- [ ] 新表已改 `EchoMusicDatabase` version 且有 Migration 或明确 fallback