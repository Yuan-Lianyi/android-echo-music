package com.example.android_echo_music.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 歌单表：只存歌单元信息，不存歌曲元数据。
 */
@Entity(tableName = "playlist")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val coverUri: String? = null,
    val createdAt: Long
)
