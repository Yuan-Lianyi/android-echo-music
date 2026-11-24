package com.example.android_echo_music.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 歌单与歌曲的多对多关联表。
 *
 * songId 指向系统 MediaStore 中的音频 ID；MediaStore 不是 Room 表，因此不能对它建外键。
 * 删除歌单时，关联记录通过外键级联删除。
 */
@Entity(
    tableName = "playlist_song",
    primaryKeys = ["playlistId", "songId"],
    indices = [
        Index(value = ["playlistId", "orderIndex"], unique = true),
        Index(value = ["songId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: Long,
    val orderIndex: Int
)
