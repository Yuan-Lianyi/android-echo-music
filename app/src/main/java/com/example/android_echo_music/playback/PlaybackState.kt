package com.example.android_echo_music.playback

import com.example.android_echo_music.data.model.Song

/**
 * 播放状态（[A] 维护，UI 直接渲染）。
 *
 * 不可变 data class，每次变化整体替换，避免 UI 读到半更新的字段。
 */
data class PlaybackState(
    val currentSong: Song? = null,
    val queueIndex: Int = -1,
    val queueSize: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val mode: PlayMode = PlayMode.SEQUENTIAL
)