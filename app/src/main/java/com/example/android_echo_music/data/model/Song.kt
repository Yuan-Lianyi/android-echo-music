package com.example.android_echo_music.data.model

/**
 * 全组共享的歌曲数据契约（[B] 维护）。
 *
 * 字段与 MediaStore 查询列一一对应，不放 UI 状态字段（如 isPlaying）。
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val albumArtUri: String? = null,
    val folder: String = ""
)