package com.example.android_echo_music.data.repository

import com.example.android_echo_music.data.db.EchoMusicDatabase
import com.example.android_echo_music.data.db.dao.FavoriteDao
import com.example.android_echo_music.data.db.dao.PlayHistoryDao
import com.example.android_echo_music.data.db.dao.PlaylistDao
import com.example.android_echo_music.data.db.entity.FavoriteEntity
import com.example.android_echo_music.data.db.entity.PlaylistEntity
import com.example.android_echo_music.data.model.PlayHistory
import com.example.android_echo_music.data.model.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * 基于 Room 的歌单、收藏和播放历史实现。
 */
class PlaylistRepositoryImpl(
    private val database: EchoMusicDatabase,
    private val playlistDao: PlaylistDao = database.playlistDao(),
    private val favoriteDao: FavoriteDao = database.favoriteDao(),
    private val playHistoryDao: PlayHistoryDao = database.playHistoryDao()
) : PlaylistRepository {

    override fun observePlaylists(): Flow<List<Playlist>> {
        return playlistDao.observePlaylistsWithSongCount().map { rows ->
            rows.map { row ->
                row.playlist.toDomain(songCount = row.songCount)
            }
        }
    }

    override fun observePlaylist(playlistId: Long): Flow<Playlist?> {
        return combine(
            playlistDao.observePlaylistById(playlistId),
            playlistDao.observeSongCount(playlistId)
        ) { playlist, songCount ->
            playlist?.toDomain(songCount)
        }
    }

    override suspend fun createPlaylist(name: String, now: Long): Long {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "歌单名称不能为空" }
        return playlistDao.insertPlaylist(
            PlaylistEntity(
                name = normalizedName,
                createdAt = now
            )
        )
    }

    override suspend fun renamePlaylist(playlistId: Long, name: String) {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "歌单名称不能为空" }
        val updatedRows = playlistDao.renamePlaylist(playlistId, normalizedName)
        require(updatedRows > 0) { "歌单不存在：$playlistId" }
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylistById(playlistId)
    }

    override fun observePlaylistSongIds(playlistId: Long): Flow<List<Long>> {
        return playlistDao.observeSongIds(playlistId)
    }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        playlistDao.addSongToPlaylist(playlistId, songId)
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSong(playlistId, songId)
    }

    override fun observeFavoriteSongIds(): Flow<List<Long>> {
        return favoriteDao.observeFavoriteSongIds()
    }

    override fun observeIsFavorite(songId: Long): Flow<Boolean> {
        return favoriteDao.observeIsFavorite(songId)
    }

    override suspend fun setFavorite(songId: Long, favorite: Boolean, now: Long) {
        if (favorite) {
            favoriteDao.upsert(FavoriteEntity(songId = songId, createdAt = now))
        } else {
            favoriteDao.delete(songId)
        }
    }

    override suspend fun toggleFavorite(songId: Long) {
        val currentlyFavorite = favoriteDao.isFavorite(songId)
        setFavorite(
            songId = songId,
            favorite = !currentlyFavorite,
            now = System.currentTimeMillis()
        )
    }

    override fun observePlayHistory(): Flow<List<PlayHistory>> {
        return playHistoryDao.observeHistory().map { entities ->
            entities.map { entity ->
                PlayHistory(
                    songId = entity.songId,
                    playedAt = entity.playedAt,
                    playCount = entity.playCount
                )
            }
        }
    }

    override suspend fun recordPlay(songId: Long, playedAt: Long) {
        playHistoryDao.upsertPlay(songId, playedAt)
    }

    override suspend fun clearPlayHistory() {
        playHistoryDao.clearHistory()
    }

    private fun PlaylistEntity.toDomain(songCount: Int): Playlist {
        return Playlist(
            id = id,
            name = name,
            coverUri = coverUri,
            createdAt = createdAt,
            songCount = songCount
        )
    }
}
