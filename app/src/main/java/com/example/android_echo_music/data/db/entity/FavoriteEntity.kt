package com.example.android_echo_music.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 收藏表：一行代表一首被收藏的歌曲，歌曲详情仍实时从 MediaStore 读取。
 */
@Entity(tableName = "favorite")
data class FavoriteEntity(
    @PrimaryKey val songId: Long,
    val createdAt: Long
)
