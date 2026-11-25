package com.example.android_echo_music

import android.content.Context
import com.example.android_echo_music.data.db.EchoMusicDatabase
import com.example.android_echo_music.data.repository.FakeMusicRepository
import com.example.android_echo_music.data.repository.MusicRepository
import com.example.android_echo_music.data.repository.PlaylistRepository
import com.example.android_echo_music.data.repository.PlaylistRepositoryImpl
import com.example.android_echo_music.playback.PlayerController
import com.example.android_echo_music.playback.createPlayerController

/**
 * 手写依赖容器（不引入 Hilt）。
 *
 * 替换点：
 * - [musicRepository]：B 在 W2 换成 MediaStore + Room 的真实实现，UI 不用改。
 * - [playlistRepository]：Room 已提供歌单、收藏和播放历史持久化能力，UI 后续接入。
 * - [playerController]：A 接管 MediaSession 细节，接口保持不变；
 *   具体实现类的 Media3 细节（含 `@UnstableApi` opt-in）收在
 *   `playback/PlayerControllerFactory.kt`，本文件只依赖稳定的工厂函数。
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: EchoMusicDatabase by lazy {
        EchoMusicDatabase.getInstance(appContext)
    }

    val musicRepository: MusicRepository = FakeMusicRepository()

    val playlistRepository: PlaylistRepository by lazy {
        PlaylistRepositoryImpl(database)
    }

    val playerController: PlayerController by lazy {
        createPlayerController(appContext)
    }

    init {
        // 启动即建立 MediaController 连接（约 100ms），避免首次点击时播放请求被丢弃
        playerController
    }
}