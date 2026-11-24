package com.example.android_echo_music.data.repository

import com.example.android_echo_music.data.model.PlayHistory
import com.example.android_echo_music.data.model.Playlist
import kotlinx.coroutines.flow.Flow

/**
 * 歌单、收藏与播放历史的数据契约（[B] 维护）。
 *
 * 这里只暴露领域模型和歌曲 ID，不向 UI 暴露 Room Entity / DAO。
 * 歌曲详情由 [MusicRepository] 从 MediaStore 获取后再按 songId 组装。
 */
interface PlaylistRepository {

    fun observePlaylists(): Flow<List<Playlist>>

    fun observePlaylist(playlistId: Long): Flow<Playlist?>

    suspend fun createPlaylist(
        name: String,
        now: Long = System.currentTimeMillis()
    ): Long

    suspend fun renamePlaylist(playlistId: Long, name: String)

    suspend fun deletePlaylist(playlistId: Long)

    fun observePlaylistSongIds(playlistId: Long): Flow<List<Long>>

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long)

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    fun observeFavoriteSongIds(): Flow<List<Long>>

    fun observeIsFavorite(songId: Long): Flow<Boolean>

    suspend fun setFavorite(
        songId: Long,
        favorite: Boolean,
        now: Long = System.currentTimeMillis()
    )

    suspend fun toggleFavorite(songId: Long)

    fun observePlayHistory(): Flow<List<PlayHistory>>

    suspend fun recordPlay(
        songId: Long,
        playedAt: Long = System.currentTimeMillis()
    )

    suspend fun clearPlayHistory()
}
