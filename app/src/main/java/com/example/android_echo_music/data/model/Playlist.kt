package com.example.android_echo_music.data.model

/**
 * 歌单领域模型（全组共享契约）。
 *
 * [songCount] 是查询时统计出的派生字段，不写入 playlist 表。
 */
data class Playlist(
    val id: Long,
    val name: String,
    val coverUri: String? = null,
    val createdAt: Long,
    val songCount: Int = 0
)
