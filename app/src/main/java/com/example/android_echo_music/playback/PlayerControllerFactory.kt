package com.example.android_echo_music.playback

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

/**
 * 播放侧对外暴露的「组装函数」（[A] 维护）。
 *
 * 存在意义：把 Media3 的 `@UnstableApi` opt-in 全部收在 playback 包内。
 * [com.example.android_echo_music.AppContainer]（公共文件）只调用 [createPlayerController]，
 * 拿回稳定的 [PlayerController]，里面不出现任何 Media3 类型或注解；
 * 将来换成测试用的 Fake 实现也只需改这里。
 */
@OptIn(UnstableApi::class)
internal fun createPlayerController(context: Context): PlayerController =
    MediaSessionPlayerController(context)

/** 异步连接 [PlaybackService]；Service 未启动时由 Media3 自动拉起。 */
@OptIn(UnstableApi::class)
internal fun buildMediaController(context: Context): ListenableFuture<MediaController> =
    MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, PlaybackService::class.java))
    ).buildAsync()