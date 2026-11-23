package com.example.android_echo_music.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * 播放服务（[A] 的所有地，W2 继续扩展耳机按键、通知栏样式）。
 *
 * MediaSessionService 会自动处理：
 * - 后台播放（前台服务 + 常驻通知）
 * - 通知栏 / 锁屏播放控制
 * - 耳机线控与蓝牙按键
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            // 自动处理音频焦点：来电 / 其他 App 播放时暂停或降低音量
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
            // 拔耳机时自动暂停
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        // 用户从最近任务划掉 App：没有在播就彻底停止服务，避免残留通知
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}