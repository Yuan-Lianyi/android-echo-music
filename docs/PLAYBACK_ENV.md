# 环境配置与提交规范（A 播放核心）

> 适用项目：android-echo-music（Android 本地音乐播放器，课程项目）
> 分支：`feature/playback`　负责模块：`app/src/main/java/com/example/android_echo_music/playback/`
> 本文档对应 DEVELOPMENT.md 中「A 播放核心：播放引擎与后台」的模块说明

---

## 一、环境配置

以下版本是**实测可编译通过**的组合，换版本前先在本地跑一遍 `./gradlew :app:assembleDebug`。

### 1. 开发工具

| 工具 | 版本 | 说明 |
|---|---|---|
| Android Studio | 2026.1.3（AI-261.x） | 打开项目根目录 `android-echo-music`，不是 `app/` |
| IntelliJ IDEA | 2026.2.2（可选） | 也能打开，但缺 Logcat / 设备管理器，调试 MediaSession 不方便 |
| 项目路径 | **必须纯英文** | 放在 `D:\asd\android-echo-music`；路径含中文时 AGP 会直接拒绝构建 |

### 2. JDK

| 项 | 值 |
|---|---|
| Gradle JDK | **OpenJDK 25**（`C:\Users\鸢\.jdks\openjdk-25.0.1`） |
| 配置位置 | `Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK` 选 25 |
| 为什么是 25 | `gradle/gradle-daemon-jvm.properties` 里 `toolchainVersion=25`，同时 AGP 9.x 要求 JDK 17+ |

### 3. Gradle

| 项 | 值 |
|---|---|
| Gradle Wrapper | **9.9.1**（`gradle/wrapper/gradle-wrapper.properties`） |
| 配置缓存 | `org.gradle.configuration-cache=true`（已在 gradle.properties 开启） |
| 国内加速 | Gradle 发行版从 `services.gradle.org` 经常超时，可用腾讯镜像 `https://mirrors.cloud.tencent.com/gradle/gradle-9.9.1-bin.zip` 预先下载放进 `~/.gradle/wrapper/dists/` |

### 4. Android SDK

| 项 | 值 |
|---|---|
| SDK 路径 | `D:\Androidsdk`（写在 `local.properties` 的 `sdk.dir`，此文件不进 Git） |
| compileSdk / targetSdk | **37**（`platforms/android-37.0` 必须已安装） |
| build-tools | 36.0.0 |
| minSdk | 24（Android 7.0） |

### 5. 构建版本与依赖（Version Catalog 统一管理）

版本号只在 `gradle/libs.versions.toml` 里改，单人负责，避免合并冲突。

| 依赖 | 版本 | A 模块用到的能力 |
|---|---|---|
| AGP | 9.3.3 | — |
| Media3（exoplayer + session） | **1.11.1** | ExoPlayer、MediaSessionService、通知栏、播放模式按钮 |
| Kotlin Coroutines | 1.11.0 | StateFlow、进度轮询、按钮同步防抖 |
| Lifecycle | 2.11.1 | ViewModel / repeatOnLifecycle |
| Room | 2.8.5 | B 的数据层（A 不依赖） |
| KSP | 2.3.14 | Room 编译器 |
| Coil | 2.7.0 | 封面加载 |
| Material | 1.14.0 | Material 3 主题 |

### 6. 权限与 Manifest（已由 C 配置好，A 只使用）

| 声明 | 用途 |
|---|---|
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 后台播放前台服务（Android 14+ 必须） |
| `POST_NOTIFICATIONS` | Android 13+ 通知栏控制，首次播放会弹授权 |
| `WAKE_LOCK` | 锁屏播放 |
| `<service android:name=".playback.PlaybackService" android:foregroundServiceType="mediaPlayback" android:exported="true">` | 必须带 `androidx.media3.session.MediaSessionService` 的 intent-filter |

### 7. 常用命令

```bash
# 编译调试包（输出 app/build/outputs/apk/debug/app-debug.apk）
./gradlew :app:assembleDebug

# 跑单元测试（PlayQueueTest 等）
./gradlew :app:testDebugUnitTest

# 代码检查（0 error 才能提交）
./gradlew :app:lintDebug

# 装到已连接的手机 / 模拟器
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 二、Git 分支与 Commit 提交规范

### 1. 分支模型

| 分支 | 用途 | 规则 |
|---|---|---|
| `main` | 最稳定、可演示 | **禁止直接 push**，只能 PR 合并 |
| `feature/playback` | A 的播放核心开发分支 | 从 `main` 拉出，只放 `playback/` 相关改动 |
| `feature/<模块名>` | B 数据层、C 界面 | 例：`feature/library`、`feature/ui` |

**分支命名规则**（与 team 约定保持一致）：

- 新功能：`feature/<模块名>`，例：`feature/playback`、`feature/queue`
- Bug 修复：`fix/<问题简述>`，例：`fix/prev-rewind-3s`

### 2. 提交信息格式（必须带前缀）

```
feat: 新增功能
fix: 修复 bug
refactor: 代码重构，不改业务逻辑
docs: 更新文档 / 注释 / README
style: 格式化代码（仅缩进、空格、命名调整）
chore: 构建配置、依赖升级、杂项
```

示例：

```
feat(playback): 播放队列支持下一首播放与移除
fix(playback): 单曲循环下点下一首不再原地重播
docs(playback): 补充 A 模块环境配置与提交规范
```

### 3. 提交范围（A 模块）

| 要提交 | 不要提交 |
|---|---|
| `app/src/main/java/.../playback/**` | `.idea/` 下的个人 IDE 配置 |
| `app/src/test/java/.../playback/**` | `local.properties`（已在 .gitignore） |
| `app/src/main/java/.../AppContainer.kt`（接线处） | `build/`、`.gradle/`、`.kotlin/` |
| `gradle/wrapper/gradle-wrapper.properties`（版本升级） | 别人负责的 `ui/`、`data/` 文件 |

每次提交前自检：

```bash
git status                                  # 确认没有夹带无关文件
git diff --stat                             # 看改动范围
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug   # 三项全绿再提交
```

### 4. 提交操作步骤

```bash
# 1. 切到开发分支
git checkout -b feature/playback          # 首次
# 或
git checkout feature/playback             # 已有分支

# 2. 精确添加 A 相关文件（不要 git add .）
git add app/src/main/java/com/example/android_echo_music/playback/ \
        app/src/test/java/com/example/android_echo_music/playback/ \
        app/src/main/java/com/example/android_echo_music/AppContainer.kt \
        gradle/wrapper/gradle-wrapper.properties \
        docs/PLAYBACK_ENV.md

# 3. 提交
git commit -m "feat(playback): 接入播放队列与通知栏播放模式切换"

# 4. 首次推送并建立远程跟踪
git push -u origin feature/playback
```

### 5. 常见 Git 阻塞问题

| 报错 | 原因 | 解决 |
|---|---|---|
| `untracked files would lose ... Aborting` | 切分支时目录里有**未跟踪文件**，目标分支上没有该目录 | 先把未跟踪文件 `git add` 提交，或临时 `git stash -u`，切完分支再 `git stash pop` |
| `Updates were rejected` | 远程有新提交 | `git pull --rebase origin feature/playback` 后再 push |
| 中文路径构建失败 | 项目放在含中文字符的目录 | 把项目挪到纯英文路径（如 `D:\asd\`） |

---

## 三、A 模块的代码约定

- `playback/` 内**不得**出现 `Fragment`、`ViewModel`、`Room`、`MediaStore` 类型（分层规则见 PROJECT_STRUCTURE.md）
- 所有 Media3 的 `@UnstableApi` opt-in 只允许出现在 `playback/` 包内，UI 层不受影响
- 公共契约（`PlayerController`、`PlaybackState`、`PlayMode`）改动必须先和 B、C 确认
- 新增队列 / 播放逻辑优先写成纯 Kotlin，放进 `PlayQueue` 并补单测，便于无真机验证

---

## 四、当前 A 模块完成度

| 功能 | 状态 | 位置 |
|---|---|---|
| Media3/ExoPlayer 接入 | 完成 | `playback/PlaybackService.kt` |
| 播放队列 | 逻辑完成（UI 待 C 接） | `playback/queue/PlayQueue.kt`、`PlayerController.queue` |
| 切歌 / 循环 / 随机 | 完成 | `playback/PlaybackCommands.kt`、`PlayerController.next/previous` |
| 音频焦点 | 完成 | `PlaybackService` 的 `setAudioAttributes(handleAudioFocus = true)` |
| 耳机与蓝牙按键 | 完成（真机需验证） | MediaSession 自动路由 + `setHandleAudioBecomingNoisy(true)` |
| MediaSessionService + 通知栏 + 后台播放 | 完成（真机需验证） | `PlaybackService` 前台服务 + 通知栏播放模式按钮 |