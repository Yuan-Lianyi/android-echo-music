package com.example.android_echo_music.playback.queue

import com.example.android_echo_music.data.model.Song

/**
 * 纯逻辑的播放队列（[A] 维护，可单元测试）。
 *
 * 只负责「列表层面的增删与位置计算」，不碰 Media3、不碰 Android 类型：
 * - 起播时构建队列（点击的歌曲排到最前面，符合本地播放器习惯）
 * - 队列内插入 / 移除 / 清空
 * - 顺序模式下计算上一首、下一首在原始列表中的下标
 *
 * 随机播放的真实顺序由 Media3 Player 的内部随机序负责，本类不重复实现。
 */
class PlayQueue(initialSongs: List<Song> = emptyList()) {

    private var songs: List<Song> = initialSongs.toList()
    private var currentIndex = -1
    private var shuffle = false

    /** 当前队列（不可变）。 */
    val items: List<Song> get() = songs

    val size: Int get() = songs.size

    val currentSong: Song? get() = songs.getOrNull(currentIndex)

    /** 当前歌曲下标，-1 表示尚未播放。 */
    val indexOfCurrent: Int get() = currentIndex

    val isShuffle: Boolean get() = shuffle

    /**
     * 用「点击的歌曲 + 其所在列表」替换整个队列。
     *
     * @return 新的当前下标（从 0 开始，点击的歌永远是第一首）
     */
    fun startWith(song: Song, list: List<Song>): Int {
        songs = buildList {
            add(song)
            list.forEach { if (it.id != song.id) add(it) }
        }
        currentIndex = 0
        return currentIndex
    }

    /** 跳到指定下标；越界时返回 false。 */
    fun moveTo(index: Int): Boolean {
        if (index !in songs.indices) return false
        currentIndex = index
        return true
    }

    /** Media3 切歌后同步真实位置（按歌曲 id 找回）。 */
    fun moveToSong(songId: Long?): Boolean {
        if (songId == null) return false
        val index = songs.indexOfFirst { it.id == songId }
        if (index < 0) return false
        currentIndex = index
        return true
    }

    /** 设置播放模式；随机模式只影响 [nextIndex]/[previousIndex] 的返回值。 */
    fun setShuffle(enabled: Boolean) {
        shuffle = enabled
    }

    /** 歌曲在队列中的下标，找不到返回 -1。 */
    fun indexOf(songId: Long): Int = songs.indexOfFirst { it.id == songId }

    /**
     * 下一首的下标：顺序模式循环列表，随机模式返回 -1（交给 Player 随机序）。
     */
    fun nextIndex(): Int {
        if (songs.isEmpty()) return -1
        if (shuffle) return -1
        return (currentIndex + 1).mod(songs.size)
    }

    /**
     * 上一首的下标：顺序模式循环列表，随机模式返回 -1。
     *
     * 注意与 [com.example.android_echo_music.playback.PlayerController.previous] 的区别：
     * 耳机线控 / 通知栏的「上一首」遵循系统习惯（播放超过 3 秒先回到开头），
     * 队列页按钮则稳定切到上一首。
     */
    fun previousIndex(): Int {
        if (songs.isEmpty()) return -1
        if (shuffle) return -1
        val base = if (currentIndex <= 0) songs.size else currentIndex
        return (base - 1).mod(songs.size)
    }

    /**
     * 「下一首播放」：把歌曲插到当前歌曲之后。
     *
     * 歌曲已在队列里时先摘下来再插，避免出现重复条目。
     *
     * @return 是否插入成功
     */
    fun insertNext(song: Song): Boolean {
        if (songs.isEmpty()) {
            songs = listOf(song)
            currentIndex = 0
            return true
        }
        val currentId = currentSong?.id ?: return false
        val source = songs.filterNot { it.id == song.id }
        val anchor = source.indexOfFirst { it.id == currentId }
        if (anchor < 0) return false
        songs = source.toMutableList().apply { add(anchor + 1, song) }
        currentIndex = anchor
        return true
    }

    /**
     * 从队列移除。
     *
     * 移除的是当前歌曲时，当前下标落到同一位置的下一条（即「下一首顶上来」）。
     */
    fun removeAt(index: Int): Song? {
        if (index !in songs.indices) return null
        val removed = songs[index]
        songs = songs.filterIndexed { i, _ -> i != index }
        currentIndex = when {
            songs.isEmpty() -> -1
            index < currentIndex -> currentIndex - 1
            index == currentIndex -> index.coerceAtMost(songs.size - 1)
            else -> currentIndex
        }
        return removed
    }

    /** 清空。 */
    fun clear() {
        songs = emptyList()
        currentIndex = -1
    }

    /** 用一组歌曲重置队列（外部会话改动了媒体项时使用），尽量保持当前歌曲位置。 */
    fun replaceAll(newSongs: List<Song>) {
        val currentId = currentSong?.id
        songs = newSongs
        currentIndex = if (currentId != null) songs.indexOfFirst { it.id == currentId } else -1
    }

    /** 生成对外快照。 */
    fun snapshot(): QueueSnapshot = QueueSnapshot(
        songs = songs,
        currentIndex = currentIndex,
        isShuffle = shuffle
    )
}