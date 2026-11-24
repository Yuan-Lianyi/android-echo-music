package com.example.android_echo_music.data.model

/**
 * 一首歌曲的最近播放记录（全组共享契约）。
 *
 * @param playedAt 最近一次播放时间，毫秒时间戳。
 * @param playCount 累计播放次数。
 */
data class PlayHistory(
    val songId: Long,
    val playedAt: Long,
    val playCount: Int
)
