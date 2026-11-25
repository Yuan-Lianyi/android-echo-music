package com.example.android_echo_music.playback.queue

import com.example.android_echo_music.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [PlayQueue] 的纯逻辑单测（不需要真机）。
 *
 * 覆盖 A 部分 W3 的队列行为：起播顺序、下一首播放、移除、循环、随机模式。
 */
class PlayQueueTest {

    private fun song(id: Long, title: String = "song-$id") = Song(
        id = id,
        title = title,
        artist = "artist",
        album = "album",
        durationMs = 60_000L,
        uri = "content://media/external/audio/media/$id"
    )

    private val list = listOf(song(1), song(2), song(3), song(4))

    @Test
    fun `startWith puts clicked song first and keeps the rest order`() {
        val queue = PlayQueue()
        queue.startWith(song(3), list)

        assertEquals(listOf(3L, 1L, 2L, 4L), queue.items.map { it.id })
        assertEquals(0, queue.indexOfCurrent)
        assertEquals(3L, queue.currentSong?.id)
    }

    @Test
    fun `next and previous wrap around the list in sequential mode`() {
        val queue = PlayQueue()
        queue.startWith(song(1), list)

        assertEquals(1, queue.nextIndex())
        queue.moveTo(3) // 最后一首 4
        assertEquals(0, queue.nextIndex()) // 循环回第一首
        assertEquals(2, queue.previousIndex())
    }

    @Test
    fun `shuffle mode delegates to the player`() {
        val queue = PlayQueue()
        queue.startWith(song(2), list)
        queue.setShuffle(true)

        assertEquals(-1, queue.nextIndex())
        assertEquals(-1, queue.previousIndex())
        assertTrue(queue.snapshot().isShuffle)
    }

    @Test
    fun `insertNext places song right after current and does not duplicate`() {
        val queue = PlayQueue()
        queue.startWith(song(1), list)

        assertTrue(queue.insertNext(song(9)))
        assertEquals(listOf(1L, 9L, 2L, 3L, 4L), queue.items.map { it.id })
        assertEquals(0, queue.indexOfCurrent)

        // 已经在队列里的歌再插一次，应该被移到当前位置之后而不是出现两份
        assertTrue(queue.insertNext(song(4)))
        assertEquals(listOf(1L, 4L, 9L, 2L, 3L), queue.items.map { it.id })
        assertEquals(1, queue.items.count { it.id == 4L })
    }

    @Test
    fun `removeAt before current shifts index`() {
        val queue = PlayQueue()
        queue.startWith(song(1), list) // [1,2,3,4]
        queue.moveTo(2) // 当前是 3

        assertEquals(song(2).id, queue.removeAt(1)?.id)
        assertEquals(1, queue.indexOfCurrent)
        assertEquals(3L, queue.currentSong?.id)
    }

    @Test
    fun `removeAt current plays the next item`() {
        val queue = PlayQueue()
        queue.startWith(song(1), list) // [1,2,3,4]
        queue.moveTo(1) // 当前是 2

        queue.removeAt(1)
        assertEquals(listOf(1L, 3L, 4L), queue.items.map { it.id })
        assertEquals(1, queue.indexOfCurrent)
        assertEquals(3L, queue.currentSong?.id)
    }

    @Test
    fun `removeAt current at tail falls back to previous`() {
        val queue = PlayQueue()
        queue.startWith(song(1), list)
        queue.moveTo(3) // 当前是 4（队尾）

        queue.removeAt(3)
        assertEquals(2, queue.indexOfCurrent)
        assertEquals(3L, queue.currentSong?.id)
    }

    @Test
    fun `removeAt last remaining song empties queue`() {
        val queue = PlayQueue()
        queue.startWith(song(1), listOf(song(1)))

        queue.removeAt(0)
        assertTrue(queue.items.isEmpty())
        assertEquals(-1, queue.indexOfCurrent)
    }

    @Test
    fun `moveToSong follows player transitions`() {
        val queue = PlayQueue()
        queue.startWith(song(1), list)

        assertTrue(queue.moveToSong(4L))
        assertEquals(3, queue.indexOfCurrent)
        assertFalse(queue.moveToSong(404L))
        assertFalse(queue.moveToSong(null))
        assertNull(PlayQueue().currentSong)
    }

    @Test
    fun `snapshot reflects current state`() {
        val queue = PlayQueue()
        queue.startWith(song(2), list)
        val snapshot = queue.snapshot()

        assertEquals(4, snapshot.songs.size)
        assertEquals(0, snapshot.currentIndex)
        assertEquals(list.sumOf { it.durationMs }, snapshot.totalDurationMs)
        assertEquals(song(2), snapshot.currentSong)
    }
}