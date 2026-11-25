package com.example.android_echo_music.playback.queue

import com.example.android_echo_music.data.model.Song

/**
 * 播放队列快照（[A] 维护，UI 只读）。
 *
 * 队列的真实顺序由 Media3 Player 维护（它负责随机序与循环），
 * 这里保存的是「原始列表 + 当前播放位置」，供 UI 展示「第几首 / 共几首」
 * 以及计算上一首、下一首。
 *
 * 之所以不把 Media3 的随机序号透出来，是因为 Player 的 shuffle 顺序对上层是内部状态，
 * 硬解出下标会在切歌时出现错位；这里只保证 UI 需要的两个数字准确。
 */
data class QueueSnapshot(
    /** 队列里的歌曲，顺序与用户点击列表时一致。 */
    val songs: List<Song> = emptyList(),
    /** 当前歌曲在 [songs] 中的下标，-1 表示还没有播放。 */
    val currentIndex: Int = -1,
    /** 是否处于随机播放。 */
    val isShuffle: Boolean = false
) {
    /** 队列是否为空。 */
    val isEmpty: Boolean get() = songs.isEmpty()

    /** 当前歌曲。 */
    val currentSong: Song? get() = songs.getOrNull(currentIndex)

    /** 总时长（毫秒），队列页底部统计用。 */
    val totalDurationMs: Long get() = songs.sumOf { it.durationMs }

    /**
     * 计算「下一首」在原始列表中的下标。
     *
     * - 顺序播放：走到队尾后回到第一首（播放模式本身就是循环列表）
     * - 随机播放：返回 -1，交给 Player 的内部随机序决定
     */
    fun nextIndex(): Int {
        if (songs.isEmpty()) return -1
        if (isShuffle) return -1
        return (currentIndex + 1).mod(songs.size)
    }

    /**
     * 计算「上一首」在原始列表中的下标。
     *
     * Media3 的 `seekToPreviousMediaItem` 在播放超过 3 秒时只会跳到开头，
     * 队列页的「上一首」按钮需要稳定地切到前一首，所以单独计算。
     */
    fun previousIndex(): Int {
        if (songs.isEmpty()) return -1
        if (isShuffle) return -1
        val base = if (currentIndex <= 0) songs.size else currentIndex
        return (base - 1).mod(songs.size)
    }
}