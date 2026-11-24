package com.example.android_echo_music.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.android_echo_music.data.db.entity.FavoriteEntity
import com.example.android_echo_music.data.db.entity.PlaylistEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room 表结构与 DAO 基础行为测试，必须在真机或模拟器上运行。
 */
@RunWith(AndroidJUnit4::class)
class PlaylistDatabaseTest {

    private lateinit var database: EchoMusicDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, EchoMusicDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun playlist_addDuplicateAndOrderedSongs_countAndCascadeDelete() = runBlocking {
        val playlistId = database.playlistDao().insertPlaylist(
            PlaylistEntity(name = "通勤歌单", createdAt = 100L)
        )

        database.playlistDao().addSongToPlaylist(playlistId, songId = 10L)
        database.playlistDao().addSongToPlaylist(playlistId, songId = 10L)
        database.playlistDao().addSongToPlaylist(playlistId, songId = 20L)

        assertEquals(
            listOf(10L, 20L),
            database.playlistDao().observeSongIds(playlistId).first()
        )
        assertEquals(2, database.playlistDao().observeSongCount(playlistId).first())

        val playlistWithCount = database.playlistDao().observePlaylistsWithSongCount().first().single()
        assertEquals("通勤歌单", playlistWithCount.playlist.name)
        assertEquals(2, playlistWithCount.songCount)

        database.playlistDao().deletePlaylistById(playlistId)

        assertTrue(database.playlistDao().observeSongIds(playlistId).first().isEmpty())
    }

    @Test
    fun favorite_upsertAndDelete() = runBlocking {
        database.favoriteDao().upsert(FavoriteEntity(songId = 30L, createdAt = 100L))

        assertTrue(database.favoriteDao().isFavorite(30L))
        assertEquals(listOf(30L), database.favoriteDao().observeFavoriteSongIds().first())

        database.favoriteDao().delete(30L)

        assertFalse(database.favoriteDao().isFavorite(30L))
        assertTrue(database.favoriteDao().observeFavoriteSongIds().first().isEmpty())
    }

    @Test
    fun playHistory_sameSongUpsert_incrementsCountAndUpdatesTime() = runBlocking {
        database.playHistoryDao().upsertPlay(songId = 40L, playedAt = 100L)
        database.playHistoryDao().upsertPlay(songId = 40L, playedAt = 200L)

        val history = database.playHistoryDao().observeHistory().first()

        assertEquals(1, history.size)
        assertEquals(40L, history.single().songId)
        assertEquals(200L, history.single().playedAt)
        assertEquals(2, history.single().playCount)
    }
}
