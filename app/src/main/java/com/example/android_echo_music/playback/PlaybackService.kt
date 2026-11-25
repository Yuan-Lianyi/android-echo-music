package com.example.android_echo_music.playback

import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.android_echo_music.playback.PlaybackCommands.applyTo
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 播放服务（[A] 的所有地）。
 *
 * [MediaSessionService] 自动处理：
 * - 后台播放（前台服务 + 常驻通知，Android 14+ 使用 `mediaPlayback` 前台服务类型）
 * - 锁屏 / 通知栏 / 蓝牙耳机 / 车机控制
 * - 拔出耳机时暂停（[ExoPlayer.Builder.setHandleAudioBecomingNoisy]）
 * - 音频焦点：来电或其他 App 播放时自动暂停 / 降低音量
 *
 * 本类额外负责：
 * - 播放模式的落点（顺序 / 单曲循环 / 随机，写回 Player 的 repeat/shuffle）
 * - 播放模式变化时同步通知栏按钮图标（[PlaybackCommands]）
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    /** 通知栏按钮刷新用（按钮图标跟随播放模式变化）。 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var buttonSyncJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            // 自动处理音频焦点：来电 / 其他 App 播放时暂停或降低音量
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
            // 拔耳机时自动暂停
            .setHandleAudioBecomingNoisy(true)
            // 耳机线控 / 通知栏的「快退快进」步长
            .setSeekBackIncrementMs(SEEK_BACK_INCREMENT_MS)
            .setSeekForwardIncrementMs(SEEK_FORWARD_INCREMENT_MS)
            .build()

        // 初始播放模式：顺序 + 列表循环（与 PlayerController 的初始 state 一致）
        PlayMode.SEQUENTIAL.applyTo(player)

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(SessionCallback())
            .build()

        // 播放模式变化时同步通知栏按钮的图标与文案
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(
                        Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                        Player.EVENT_REPEAT_MODE_CHANGED
                    )
                ) {
                    scheduleButtonSync()
                }
            }
        })
    }

    /** 把通知栏按钮偏好推给所有 Controller（含系统媒体控件），带 150ms 防抖。 */
    private fun scheduleButtonSync() {
        val session = mediaSession ?: return
        buttonSyncJob?.cancel()
        buttonSyncJob = scope.launch {
            delay(BUTTON_SYNC_DELAY_MS)
            runCatching {
                session.setMediaButtonPreferences(PlaybackCommands.preferences(this@PlaybackService, session))
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        // 用户从最近任务划掉 App：没有在播就彻底停止服务，避免残留通知
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        scope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    /**
     * 会话回调：下发通知栏按钮 + 接收按钮点击。
     *
     * 播放 / 暂停 / 切歌这类标准操作 Media3 已经映射到 Player，
     * 不需要（也不应该）在这里重复实现。
     */
    private inner class SessionCallback : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult =
            MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                .setMediaButtonPreferences(PlaybackCommands.preferences(this@PlaybackService, session))
                .build()

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> =
            PlaybackCommands.handleCommand(this@PlaybackService, session, customCommand)
    }

    private companion object {
        const val SEEK_BACK_INCREMENT_MS = 10_000L
        const val SEEK_FORWARD_INCREMENT_MS = 10_000L
        const val BUTTON_SYNC_DELAY_MS = 150L
    }
}