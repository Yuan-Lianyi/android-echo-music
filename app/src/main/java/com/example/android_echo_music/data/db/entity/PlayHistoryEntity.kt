package com.example.android_echo_music.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 播放历史表：同一首歌只保留一行，最近播放时间和播放次数在原记录上更新。
 */
@Entity(
    tableName = "play_history",
    indices = [Index(value = ["playedAt"])]
)
data class PlayHistoryEntity(
    @PrimaryKey val songId: Long,
    val playedAt: Long,
    val playCount: Int
)
