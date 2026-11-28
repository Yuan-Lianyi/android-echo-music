package com.example.android_echo_music.playback

import android.content.Context
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.example.android_echo_music.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * 通知栏 / 系统媒体控件上「自定义按钮」的通道（[A] 维护）。
 *
 * Media3 默认通知只有「上一首 / 播放暂停 / 下一首」三个按钮，
 * 播放模式切换需要 App 自己声明：
 * 1. [PlaybackService.onConnect] 用 [preferences] 下发按钮清单；
 * 2. 用户点击时系统把命令发回 [PlaybackService.onCustomCommand]，
 *    由 Service 直接改 Player 的状态，不经过 UI（界面在后台被杀也能用）。
 *
 * 播放模式在 Player 上的读写（[PlayMode.applyTo] / [readMode]）也放在这里，
 * 保证 PlayerController 与通知栏用同一份实现，不会两边不一致。
 */
@UnstableApi
object PlaybackCommands {

    /** 通知栏「切换播放模式」按钮的命令。 */
    const val CMD_CYCLE_MODE = "com.example.android_echo_music.CYCLE_MODE"

    /**
     * 通知栏按钮偏好。
     *
     * 内置的上一首 / 播放暂停 / 下一首由 Media3 自动按槽位排好；
     * 这里追加一个「播放模式」按钮，图标与文案随当前模式变化。
     *
     * 图标使用 Media3 内置的 Material 图标常量：媒体通知会强制只保留 alpha 并涂白，
     * 内置图标不会出现失真。
     */
    fun preferences(context: Context, session: MediaSession): List<CommandButton> {
        val mode = readMode(session.player)
        val (icon, label) = when (mode) {
            PlayMode.SEQUENTIAL -> CommandButton.ICON_REPEAT_ALL to R.string.mode_sequential
            PlayMode.REPEAT_ONE -> CommandButton.ICON_REPEAT_ONE to R.string.mode_repeat_one
            PlayMode.SHUFFLE -> CommandButton.ICON_SHUFFLE_ON to R.string.mode_shuffle
        }
        return listOf(
            CommandButton.Builder(icon)
                .setSessionCommand(SessionCommand(CMD_CYCLE_MODE, Bundle.EMPTY))
                .setDisplayName(context.getString(label))
                .setEnabled(true)
                .build()
        )
    }

    /** 处理来自通知栏的按钮点击，返回是否已消费。 */
    fun handleCommand(
        context: Context,
        session: MediaSession,
        command: SessionCommand
    ): ListenableFuture<SessionResult> {
        if (command.customAction != CMD_CYCLE_MODE) {
            return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }
        val player = session.player
        readMode(player).next().applyTo(player)
        // 模式变了，通知栏按钮图标与文案也要跟着变
        session.setMediaButtonPreferences(preferences(context, session))
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    /** 把播放模式写入 Player（顺序 / 单曲循环 / 随机）。 */
    fun PlayMode.applyTo(player: Player) {
        player.shuffleModeEnabled = this == PlayMode.SHUFFLE
        player.repeatMode = when (this) {
            // 列表放完循环播放，避免播完最后一首就停
            PlayMode.SEQUENTIAL, PlayMode.SHUFFLE -> Player.REPEAT_MODE_ALL
            PlayMode.REPEAT_ONE -> Player.REPEAT_MODE_ONE
        }
    }

    /** 从 Player 读回当前模式。 */
    fun readMode(player: Player): PlayMode = when {
        player.shuffleModeEnabled -> PlayMode.SHUFFLE
        player.repeatMode == Player.REPEAT_MODE_ONE -> PlayMode.REPEAT_ONE
        else -> PlayMode.SEQUENTIAL
    }
}