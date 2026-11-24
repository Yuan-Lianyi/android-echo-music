package com.example.android_echo_music.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.android_echo_music.data.db.entity.PlaylistEntity
import com.example.android_echo_music.data.db.entity.PlaylistSongEntity
import kotlinx.coroutines.flow.Flow

/** 歌单实体与歌曲数量的联合查询结果。 */
data class PlaylistWithSongCount(
    @Embedded val playlist: PlaylistEntity,
    val songCount: Int
)

@Dao
abstract class PlaylistDao {

    @Query(
        """
        SELECT p.*, COUNT(ps.songId) AS songCount
        FROM playlist AS p
        LEFT JOIN playlist_song AS ps ON ps.playlistId = p.id
        GROUP BY p.id
        ORDER BY p.createdAt DESC
        """
    )
    abstract fun observePlaylistsWithSongCount(): Flow<List<PlaylistWithSongCount>>

    @Query("SELECT * FROM playlist WHERE id = :playlistId")
    abstract fun observePlaylistById(playlistId: Long): Flow<PlaylistEntity?>

    @Query("SELECT COUNT(*) FROM playlist_song WHERE playlistId = :playlistId")
    abstract fun observeSongCount(playlistId: Long): Flow<Int>

    @Insert
    abstract suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlist SET name = :name WHERE id = :playlistId")
    abstract suspend fun renamePlaylist(playlistId: Long, name: String): Int

    @Query("UPDATE playlist SET coverUri = :coverUri WHERE id = :playlistId")
    abstract suspend fun updateCoverUri(playlistId: Long, coverUri: String?): Int

    @Query("DELETE FROM playlist WHERE id = :playlistId")
    abstract suspend fun deletePlaylistById(playlistId: Long): Int

    @Query("SELECT songId FROM playlist_song WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    abstract fun observeSongIds(playlistId: Long): Flow<List<Long>>

    @Query("SELECT COALESCE(MAX(orderIndex) + 1, 0) FROM playlist_song WHERE playlistId = :playlistId")
    abstract suspend fun nextOrderIndex(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertSongCrossRef(crossRef: PlaylistSongEntity): Long

    @Query("DELETE FROM playlist_song WHERE playlistId = :playlistId AND songId = :songId")
    abstract suspend fun removeSong(playlistId: Long, songId: Long): Int

    @Query(
        "UPDATE playlist_song SET orderIndex = :newOrderIndex " +
            "WHERE playlistId = :playlistId AND songId = :songId"
    )
    abstract suspend fun updateSongOrder(playlistId: Long, songId: Long, newOrderIndex: Int): Int

    /** 添加歌曲到歌单末尾；重复歌曲由联合主键忽略。 */
    @Transaction
    open suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        val orderIndex = nextOrderIndex(playlistId)
        insertSongCrossRef(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = songId,
                orderIndex = orderIndex
            )
        )
    }
}
