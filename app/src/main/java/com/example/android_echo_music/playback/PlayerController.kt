package com.example.android_echo_music.playback

import com.example.android_echo_music.data.model.Song
import com.example.android_echo_music.playback.queue.QueueSnapshot
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

    /**
     * 下一首。
     *
     * 顺序模式按原始列表走（列表末尾回到第一首），随机模式交给 Player 随机序；
     * 单曲循环时也会切歌，不会原地打转。
     */
    fun next()

    /**
     * 上一首。
     *
     * 与系统播放器一致：播放超过 3 秒先回到本曲开头，否则切到上一首。
     */
    fun previous()

    fun seekTo(positionMs: Long)

    fun setMode(mode: PlayMode)

    // ---------------------------------------------------------------- 播放队列（F09）

    /** 队列快照；UI 的队列页直接渲染它，不接触 Media3 类型。 */
    val queue: StateFlow<QueueSnapshot>

    /** 「下一首播放」：把 [song] 插到当前歌曲之后；队列为空时直接开始播放。 */
    fun playNext(song: Song)

    /** 从队列移除第 [index] 首；移除的是当前歌曲时自动播放下一首。 */
    fun removeFromQueue(index: Int)

    /** 跳到队列第 [index] 首并播放。 */
    fun playAt(index: Int)
}