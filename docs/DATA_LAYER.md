# 数据层说明（B 维护）

> 适用分支：`chore/w2-data-foundation`
> 范围：Room 数据库、Repository 边界、表结构与后续 MediaStore 接入顺序。

## 1. 数据来源边界

应用是离线本地音乐播放器，数据分成两类：

1. **歌曲音频与歌曲元数据**
   - 来源：系统 `MediaStore`。
   - 音频文件仍保存在用户设备原目录，App 不复制、不下载、不入库。
   - 播放时使用 MediaStore 生成的 `content://...` URI，由 Media3/ExoPlayer 直接读取。
   - 歌曲标题、艺术家、专辑、时长、目录等实时查询，避免与系统媒体库不同步。

2. **用户产生的结构化数据**
   - 来源：Room（SQLite 抽象层）。
   - 保存：歌单、歌单-歌曲关联、收藏、播放历史。
   - Room 只保存 MediaStore 歌曲 ID，不保存音频文件，也不冗余保存完整歌曲元数据。

数据库文件位于应用私有目录：

```text
/data/data/com.example.android_echo_music/databases/echo_music.db
```

## 2. 已接入依赖

当前数据层地基使用：

- Room `2.8.5`：`androidx.room:room-runtime`、`room-ktx`
- Room Compiler 通过 KSP `2.3.14` 接入
- Kotlin Coroutines + Flow：Repository 异步查询与数据流

版本统一在 `gradle/libs.versions.toml` 管理；后续新增依赖仍需由工程负责人 C 统一 review，避免 Version Catalog 冲突。

> AGP 9 默认启用内置 Kotlin，因此 app 模块没有显式应用 `org.jetbrains.kotlin.android`。KSP 插件已验证可在当前 AGP/Gradle 组合下处理 Room 编译器。

## 3. Room 表结构

### 3.1 `playlist`：歌单

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | 歌单 ID |
| `name` | TEXT NOT NULL | 歌单名称 |
| `coverUri` | TEXT? | 歌单封面，可为空；后续也可由歌单内歌曲封面派生 |
| `createdAt` | INTEGER NOT NULL | 创建时间，毫秒时间戳 |

### 3.2 `playlist_song`：歌单-歌曲关联

| 字段 | 类型 | 说明 |
|---|---|---|
| `playlistId` | INTEGER PK, FK | 所属歌单，删除歌单时级联删除关联 |
| `songId` | INTEGER PK | MediaStore 歌曲 ID；MediaStore 不是 Room 表，不能建 Room 外键 |
| `orderIndex` | INTEGER NOT NULL | 歌单内顺序，`(playlistId, orderIndex)` 建唯一索引 |

同一首歌在同一个歌单中由联合主键 `(playlistId, songId)` 去重。

### 3.3 `favorite`：收藏

| 字段 | 类型 | 说明 |
|---|---|---|
| `songId` | INTEGER PK | MediaStore 歌曲 ID |
| `createdAt` | INTEGER NOT NULL | 收藏时间 |

### 3.4 `play_history`：最近播放

| 字段 | 类型 | 说明 |
|---|---|---|
| `songId` | INTEGER PK | MediaStore 歌曲 ID，同一首歌只保留一行 |
| `playedAt` | INTEGER NOT NULL | 最近一次播放时间，毫秒时间戳；已建索引支持倒序查询 |
| `playCount` | INTEGER NOT NULL | 累计播放次数 |

播放同一首歌时执行 upsert：已有记录则 `playedAt = 当前时间`、`playCount + 1`；不存在则插入次数为 1 的新记录。

## 4. Repository 划分

### `MusicRepository`

当前仍是 `FakeMusicRepository`，下一步由 B 替换为：

- `MediaStoreMusicSource`：封装系统媒体库查询。
- `AudioPermission`：封装 API 24–32 与 API 33+ 的音频读取权限。
- `AudioFileFilter`：时长、MIME、扩展名等过滤规则。
- `MusicRepositoryImpl`：MediaStore + 内存缓存 + Flow 搜索。

### `PlaylistRepository`

已提供 Room 实现 `PlaylistRepositoryImpl`，并在 `AppContainer.playlistRepository` 中注入：

- 歌单增删改查、歌单歌曲 ID 列表。
- 收藏状态、收藏歌曲 ID 列表。
- 播放历史记录、倒序观察和清空。

UI/ViewModel 只依赖 `PlaylistRepository` 接口，不直接使用 Room DAO 或 Entity。

## 5. UI 如何组装歌单歌曲

Room 只返回有序 `songId`，歌曲详情需要与 `MusicRepository` 的 MediaStore 歌曲列表合并。示例：

```kotlin
combine(
    playlistRepository.observePlaylistSongIds(playlistId),
    musicRepository.observeSongs()
) { songIds, songs ->
    val songsById = songs.associateBy { it.id }
    songIds.mapNotNull { songId -> songsById[songId] }
}
```

这样可以保持歌单排序；如果某个 `songId` 对应的本地文件已被删除，`mapNotNull` 会先过滤失效歌曲，后续再补关联清理。

## 6. 播放历史的接线边界

`playback/**` 按架构约定不直接依赖 Room。建议由 UI 层或应用级协调器观察 `PlayerController.state.currentSong`：

- 检测到当前歌曲 ID 变为新曲目时，调用 `PlaylistRepository.recordPlay(songId)`。
- 播放服务和播放器只负责播放与状态，不直接操作 `PlayHistoryDao`。

## 7. 数据库迁移

- 数据库版本：`1`。
- Room schema 已导出到 `app/schemas/.../1.json`，该目录需要提交到 Git，供后续迁移对比。
- 后续修改 Entity 或表结构时：
  1. `EchoMusicDatabase.version` 加 1。
  2. 编写并注册 `Migration`。
  3. 提交新生成的 schema JSON。
- 课程开发期可临时使用破坏性重建，但合并到 `main` 前不应把静默清库作为正式方案。

## 8. B 的后续顺序

1. `AudioFileFilter` + 纯 JVM 单元测试。
2. `AudioPermission` 与权限状态/设置跳转封装。
3. `MediaStoreMusicSource` 查询、字段映射和目录/封面 URI 兼容。
4. `MusicRepositoryImpl` 替换 Fake，并在授权成功后重新扫描。
5. 为 `MusicRepository` 增加排序、按 ID 集合查询等 W2/W3 所需能力。
6. 歌单详情、收藏列表、最近播放页通过 `PlaylistRepository` + `MusicRepository` 组装数据。
