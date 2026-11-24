package com.example.android_echo_music.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.android_echo_music.data.db.entity.PlayHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlayHistoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertHistory(history: PlayHistoryEntity): Long

    @Query(
        """
        UPDATE play_history
        SET playedAt = :playedAt, playCount = playCount + 1
        WHERE songId = :songId
        """
    )
    protected abstract suspend fun incrementPlayCount(songId: Long, playedAt: Long): Int

    /** 同一首歌已有记录则累加次数并更新最近播放时间，否则插入首次播放记录。 */
    @Transaction
    open suspend fun upsertPlay(songId: Long, playedAt: Long) {
        val updatedRows = incrementPlayCount(songId, playedAt)
        if (updatedRows == 0) {
            insertHistory(
                PlayHistoryEntity(
                    songId = songId,
                    playedAt = playedAt,
                    playCount = 1
                )
            )
        }
    }

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC")
    abstract fun observeHistory(): Flow<List<PlayHistoryEntity>>

    @Query("DELETE FROM play_history")
    abstract suspend fun clearHistory()
}
