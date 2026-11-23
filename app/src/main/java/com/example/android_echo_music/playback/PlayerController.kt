package com.example.android_echo_music.playback

import com.example.android_echo_music.data.model.Song
import kotlinx.coroutines.flow.StateFlow

/**
 * 对外唯一播放控制接口（[A] 维护）。
 *
 * 约定：UI 只依赖本接口，不出现 ExoPlayer / MediaController 类型。
 */
interface PlayerController {

    /** 当前播放状态，UI 用 collect 订阅。 */
    val state: StateFlow<PlaybackState>

    /** 播放 [song]，并以 [queue] 作为当前播放队列。 */
    fun play(song: Song, queue: List<Song>)

    /** 播放 / 暂停切换。 */
    fun toggle()

    fun next()

    fun previous()

    fun seekTo(positionMs: Long)

    fun setMode(mode: PlayMode)
}