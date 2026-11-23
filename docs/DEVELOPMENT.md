# Echo Music 开发文档（项目规划 v0.1）

> 项目：android-echo-music（Android 本地音乐播放器）
> 代码仓库：https://github.com/Yuan-Lianyi/android-echo-music
> 团队：3 人（A 播放核心 / B 数据层 / C 界面与工程化）
> 文档状态：规划稿，随需求评审更新

---

## 1. 项目概述

**定位**：一款离线优先的本地音乐播放器，界面简洁、播放稳定，覆盖「扫描 → 浏览 → 播放 → 歌单」的完整闭环。

**目标用户**：手机里存有本地音频文件、希望不依赖网络和账号即可听歌的用户。

**核心价值**：
- 无需联网、无需登录，打开即用
- 播放体验完整：后台播放、通知栏控制、耳机按键、歌词
- 界面清爽，支持深色模式

**范围边界（明确不做）**：
- ❌ 在线音乐搜索/流媒体（版权与 API 不可控）
- ❌ 账号体系、云同步、社交分享
- ❌ MV/视频播放

**课程/答辩定位**：功能闭环完整、架构分层清晰、三人分工可追溯（Git 提交记录 + 模块归属）。

---

## 2. 技术选型与约束

| 项目 | 现状/选型 | 说明 |
|---|---|---|
| 语言 | Kotlin | AGP 9 已内置 Kotlin 支持，当前项目未显式应用 `kotlin-android` |
| 最低版本 | minSdk 24 | 覆盖 Android 7.0+ |
| 目标版本 | compileSdk / targetSdk 37 | 需适配 Android 13+ 通知权限、Android 14+ 前台服务类型 |
| UI | View 体系（XML + Material 3 + ViewBinding） | 与现有模板一致，降低学习成本；不引入 Compose |
| 播放 | Media3（ExoPlayer + MediaSessionService） | 官方推荐，后台播放与通知栏控制开箱即用 |
| 数据 | Room + Repository | 歌单、收藏、播放历史持久化 |
| 异步 | Kotlin Coroutines + Flow | 扫描、查询、播放状态统一用 Flow 暴露 |
| 图片 | Coil | 专辑封面加载与缓存 |
| 导航 | Navigation Component | 单 Activity + 多 Fragment |
| 依赖注入 | 不引入 Hilt（手写单例容器） | 控制复杂度，三人协作成本更低 |
| 构建 | Gradle KTS + Version Catalog | 依赖统一在 `gradle/libs.versions.toml` 管理 |

**构建约束**：`build.gradle.kts` 与 `libs.versions.toml` 仅由 C 统一修改，避免三人同时加依赖产生冲突。

---

## 3. 功能需求

功能按优先级分三档：**P0 必须**（不做则无法演示）、**P1 重要**（构成完整产品）、**P2 加分**（时间富余再做）。

### 3.1 功能清单

| 编号 | 功能 | 优先级 | 说明 | 负责人 |
|---|---|---|---|---|
| F01 | 本地音乐扫描 | P0 | 读取 MediaStore 音频，首次全量扫描、后续增量扫描；过滤时长 < 30s 的提示音 | B |
| F02 | 媒体库列表 | P0 | 歌曲列表（标题/艺术家/时长/封面），按标题或添加时间排序 | B |
| F03 | 权限申请与引导 | P0 | Android 13+ 申请 `READ_MEDIA_AUDIO`，低版本申请 `READ_EXTERNAL_STORAGE`；拒绝后展示引导页 | B |
| F04 | 基础播放控制 | P0 | 播放/暂停、上一首/下一首、进度条拖动、当前播放高亮 | A |
| F05 | 后台播放 | P0 | MediaSessionService 前台服务，切后台/锁屏不中断 | A |
| F06 | 通知栏控制 | P0 | 通知显示歌曲信息与封面，支持播放/暂停/切歌；Android 13+ 申请 `POST_NOTIFICATIONS` | A |
| F07 | 播放模式 | P0 | 顺序播放、单曲循环、随机播放 | A |
| F08 | 播放页 | P1 | 大封面/旋转唱片、歌名歌手、进度、控制按钮、播放模式切换 | C |
| F09 | 播放队列 | P1 | 查看当前队列、下一首播放、从队列移除 | A |
| F10 | 歌单管理 | P1 | 创建/重命名/删除歌单，添加/移除歌曲，歌单详情页 | B |
| F11 | 收藏 | P1 | 歌曲红心收藏，收藏列表页 | B |
| F12 | 搜索 | P1 | 按歌曲名/艺术家/专辑/歌单模糊搜索，实时过滤 | B |
| F13 | 最近播放 | P1 | 记录播放历史，按时间倒序展示 | B |
| F14 | 歌词显示 | P1 | 解析同名 `.lrc` 文件，播放页滚动高亮；无歌词时展示占位 | C |
| F15 | 深色模式 / 主题 | P1 | 跟随系统 + 手动切换，Material 3 动态取色（Android 12+） | C |
| F16 | 空状态与错误处理 | P1 | 无音乐、权限拒绝、文件失效（被删除）等场景的友好提示 | C |
| F17 | 专辑 / 艺术家视图 | P2 | 按专辑、艺术家聚合展示，点击进入详情 | B |
| F18 | 文件夹视图 | P2 | 按目录浏览，适合整理过的本地曲库 | B |
| F19 | 睡眠定时器 | P2 | 定时停止播放（15/30/60 分钟） | A |
| F20 | 桌面小组件 | P2 | 4x1 小组件显示当前歌曲并控制播放 | C |
| F21 | 均衡器 | P2 | 基于 `AudioEffect` 的 5 段均衡器 | A |
| F22 | 设置页 | P1 | 主题、默认播放模式、扫描目录排除、关于/版本信息 | C |

### 3.2 核心用户流程

```
首次启动 → 权限申请 → 扫描本地音乐 → 歌曲列表
   → 点击歌曲 → 播放页 → 后台播放/通知栏控制
   → 长按歌曲 → 添加到歌单 / 收藏
   → 搜索框 → 输入关键词 → 结果列表 → 播放
```

### 3.3 关键验收标准

- **F01**：500 首歌曲扫描时间 < 5s，扫描过程不阻塞 UI，可显示进度
- **F05**：播放中按 Home 键返回桌面、锁屏 10 分钟后音乐不中断
- **F06**：通知栏封面、歌名、播放状态与 App 内一致，切歌实时刷新
- **F10**：歌单增删改在杀进程重启后仍然存在（Room 持久化）
- **F14**：歌词滚动与音频进度偏差 < 300ms
- **性能**：歌曲列表 1000 条滚动不掉帧；播放页进入 < 500ms

---

## 4. 非功能需求

| 类别 | 要求 |
|---|---|
| 兼容性 | Android 7.0（API 24）~ Android 16（API 36+），重点机型覆盖 2 台真机 |
| 权限最小化 | 仅申请音频读取与通知权限，不申请存储写入 |
| 离线可用 | 全部核心功能在飞行模式下可用 |
| 稳定性 | 连续播放 1 小时无崩溃；异常捕获后不丢失播放队列 |
| 无障碍 | 所有图标按钮提供 `contentDescription`，对比度符合 Material 规范 |
| 安装包 | Release APK < 15MB |

---

## 5. 架构与模块划分

### 5.1 分层架构

```
UI (Fragment / Activity, ViewModel)
        ↓ Flow / StateFlow
Repository (MusicRepository / PlaylistRepository)
        ↓
Data Sources: MediaStore（本地音频） + Room（歌单/收藏/历史）
        ↕
Playback: MediaController ←→ MediaSessionService（ExoPlayer）
```

### 5.2 包结构与归属

```
com.example.android_echo_music
├── data/                 # 【B】数据层
│   ├── local/            #   MediaStore 扫描、权限封装
│   ├── db/               #   Room Entity / Dao / Database
│   ├── model/            #   Song、Playlist、PlayHistory（公共契约）
│   └── repository/       #   MusicRepository、PlaylistRepository
├── playback/             # 【A】播放核心
│   ├── PlaybackService   #   MediaSessionService
│   ├── PlayerController  #   对外统一控制接口
│   └── queue/            #   播放队列与模式
├── ui/                   # 【C】界面
│   ├── home/ library/    #   首页、媒体库
│   ├── player/           #   播放页、歌词
│   ├── playlist/ search/ #   歌单、搜索
│   └── settings/         #   设置
└── common/               # 公共工具（三方共用，改动需告知）
```

> 文件级落位、资源结构、分层依赖规则与「新文件放哪」决策表见 [`PROJECT_STRUCTURE.md`](PROJECT_STRUCTURE.md)。

### 5.3 公共接口约定（第 1 天三人共同确认）

```kotlin
// data/model/Song.kt —— 全组共享的数据契约
data class Song(
    val id: Long,              // MediaStore ID
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val albumArtUri: String?,
    val folder: String
)

// data/repository/MusicRepository.kt
interface MusicRepository {
    fun observeSongs(): Flow<List<Song>>
    suspend fun refresh(): Int
    fun search(keyword: String): Flow<List<Song>>
}

// playback/PlayerController.kt —— UI 只依赖此接口
interface PlayerController {
    val state: StateFlow<PlaybackState>   // 当前歌曲、进度、播放中、模式
    fun play(song: Song, queue: List<Song>)
    fun toggle()
    fun next()
    fun previous()
    fun seekTo(ms: Long)
    fun setMode(mode: PlayMode)
}
```

> 约定：A 和 B 各自先提供「假实现」，C 基于接口开发 UI，第 2 周替换为真实实现，避免互相等待。

---

## 6. 数据模型（Room）

| 表 | 字段 | 说明 |
|---|---|---|
| `playlist` | id, name, coverUri, createdAt | 歌单 |
| `playlist_song` | playlistId, songId, orderIndex | 歌单-歌曲关联（多对多） |
| `favorite` | songId, createdAt | 收藏 |
| `play_history` | songId, playedAt, playCount | 播放历史与次数 |

歌曲元数据不落库，实时读 MediaStore；Room 只存用户产生的数据，避免与系统媒体库不同步。

---

## 7. 里程碑计划（4 周）

| 周次 | 目标 | A 播放 | B 数据 | C 界面/工程 | 交付物 |
|---|---|---|---|---|---|
| W1 | 骨架跑通 | Media3 接入，假数据播放 | MediaStore 扫描 + 假 Repository | 导航骨架 + 列表页 + ViewBinding | 可点击列表并出声的 Demo |
| W2 | 核心闭环 | 后台播放、通知栏、播放模式 | 真实扫描、Room、歌单 CRUD | 播放页、歌单页、搜索页 | 完整「扫描→播放→建歌单」链路 |
| W3 | 体验完善 | 队列、耳机按键、音频焦点 | 收藏、最近播放、搜索优化 | 歌词、深色模式、空状态 | 可提交测试的 Beta |
| W4 | 收尾 | 稳定性修复、性能优化 | 数据一致性修复 | 图标/主题打磨、Release 打包 | Release APK + 演示视频 + 答辩 PPT |

**每周固定节点**：周一需求对齐（30min）→ 周三中期联调 → 周五合并到 `main` 并打 tag。

---

## 8. 协作规范

### 8.1 仓库模式：一个主仓库 + Collaborator（本项目采用）

| 场景 | 推荐做法 |
|---|---|
| **几个同学紧密协作做课程项目（本项目）** | **一个仓库 + collaborator + 分支 + PR** |
| 开源项目 / 外部贡献者没有写权限 | Fork + PR |
| 不信任的贡献者、需要严格隔离权限 | Fork + PR |
| 老师/平台明确要求每人 fork | 主仓库作 upstream，各自 fork，频繁同步后提 PR |

**为什么不用「各自 fork 长期维护」**：

- 代码集中，不会出现每个人的 fork 分叉太久、合并困难
- Issue、PR、CI、项目看板统一
- 权限管理简单：GitHub 里把同学加成 collaborator 即可
- 适合 3 人小团队、信任度高的协作场景

Fork 的痛点：主仓库每次更新，每个人都要同步 upstream，容易混乱；适合「贡献者模式」，不适合「天天一起改代码」的紧密协作。

### 8.2 初始配置（第 0 周完成，C 负责）

1. **建仓库**：已完成，https://github.com/Yuan-Lianyi/android-echo-music
2. **加协作者**：仓库 Settings → Collaborators → 添加 A、B，权限选 **Write**
3. **保护 main**：Settings → Branches → Add branch protection rule
   - ✅ Require a pull request before merging（至少 1 个 approval）
   - ✅ Require status checks to pass（有 CI 后开启）
   - ✅ 禁止 force push、禁止直接 push
   - ✅ 合并后自动删除分支
4. **各自 clone 并配置身份**：

   ```bash
   git clone https://github.com/Yuan-Lianyi/android-echo-music.git
   cd android-echo-music
   git config user.name "你的名字"
   git config user.email "你的邮箱"
   ```

5. **确认忽略规则**：`local.properties`、`*.jks`、`build/`、`.idea/workspace.xml` 不入库

### 8.3 日常开发流程（三人都一样）

```bash
# 1. 每次开工前同步主分支
git switch main && git pull

# 2. 开自己的功能分支
git switch -c feature/playback     # 或 feature/library、feature/ui

# 3. 开发 + 本地自测，小步提交
git add -p && git commit -m "feat: 支持通知栏切歌"

# 4. 推送到自己的远程分支
git push -u origin feature/playback

# 5. 在 GitHub 发起 Pull Request
#    描述里写清：改了什么 / 怎么验证 / 影响哪些模块

# 6. 至少 1 人 review 通过后 Squash merge 到 main

# 7. 删掉已合并的本地分支，回到 main 同步
git switch main && git pull
git branch -d feature/playback
```

**Review 约定**：建议跨模块互审（A 审 B、B 审 C、C 审 A），review 时重点看接口是否破坏公共契约、是否有硬编码路径与权限遗漏。

### 8.4 分支与提交规范

- **分支**：`feature/<模块>`、`fix/<问题>`、`docs/<主题>`；`main` 受保护，只能通过 PR 合并
- **PR 粒度**：一个 PR 只做一件事，控制在 400 行以内，方便 review
- **提交信息**：`feat:` / `fix:` / `docs:` / `refactor:` / `chore:` 前缀
- **提交内容**：不提交构建产物、APK、`local.properties`、签名文件
- **任务看板**：GitHub Issues + Projects，按 F01–F22 建 Issue 并指派负责人

### 8.5 冲突预防与处理

- 公共文件（`build.gradle.kts`、`libs.versions.toml`、`Song.kt`、`strings.xml`）改动前在群里说一声，且由单点负责人修改
- 每天开工先 `git pull`，不要攒一周再合并
- 冲突在本地解决后重新提交，**禁止 force push 到 `main`**
- 遇到无法判断的冲突，找对应模块负责人一起解决

### 8.6 如果老师要求必须 fork

折中流程：主仓库作为 upstream，每人 fork，开发前同步，再提 PR 回主仓库。

```bash
git remote add upstream https://github.com/Yuan-Lianyi/android-echo-music.git
git fetch upstream
git switch main && git merge upstream/main   # 或 rebase
git push origin main
```

注意：这只是满足平台要求的「贡献者模式」，日常协作仍建议在主仓库内用分支 + PR，避免长期分叉。

### 8.7 代码风格

- 统一 Kotlin 官方风格，提交前 Android Studio 格式化（Ctrl+Alt+L）
- 变量/函数用英文命名，注释与文档可用中文

---

## 9. 风险与应对

| 风险 | 影响 | 应对 |
|---|---|---|
| 权限适配踩坑（Android 13/14 差异） | F01/F03 阻塞 | W1 先做权限专项验证，用两台不同系统版本真机 |
| 后台播放被系统杀 | F05 不达标 | 前台服务 + `mediaPlayback` 类型声明；关闭电池优化引导 |
| 歌词文件缺失 | F14 无法演示 | 内置 2 首带 `.lrc` 的示例音频；无歌词时展示「暂无歌词」 |
| 大列表卡顿 | 体验差 | Paging 3 或 RecyclerView DiffUtil + 封面缩略图 |
| 三人合并冲突 | 进度延误 | 严格按包归属提交，公共文件单点负责 |
| 时间不足 | P1 功能缺失 | 优先保 P0，P2 直接砍；W3 结束做一次范围裁剪 |

---

## 10. 交付物清单

1. Release APK（可安装、可演示）
2. 源代码（GitHub 仓库，提交记录可追溯三人分工）
3. 开发文档（本文档，随进度更新）
4. 需求/设计说明 + 数据库设计（可合并进本文档）
5. 测试记录（真机兼容性、核心流程用例）
6. 答辩 PPT 与 2–3 分钟演示视频

---

## 附录：功能优先级速览

- **P0（6 项，F01–F07）**：扫描、列表、权限引导、基础播放、后台播放、通知栏控制、播放模式
- **P1（9 项，F08–F16、F22）**：播放页、队列、歌单、收藏、搜索、最近播放、歌词、深色模式、空状态、设置
- **P2（5 项，F17–F21）**：专辑/艺术家视图、文件夹视图、睡眠定时、桌面小组件、均衡器

> 说明：本文档基于「本地音乐播放器」的常规需求拟定。若课程有指定功能点（如必须实现在线音乐或特定 UI 效果），请在需求评审时补充并调整优先级。