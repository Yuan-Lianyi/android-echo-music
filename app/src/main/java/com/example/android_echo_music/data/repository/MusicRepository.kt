package com.example.android_echo_music.data.repository

import com.example.android_echo_music.data.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * 媒体库数据契约（[B] 维护）。
 *
 * 约定：实现方负责权限判断与 MediaStore 查询，UI 只拿 [Song] 列表，不接触 Cursor。
 */
interface MusicRepository {

    /** 观察全部歌曲；数据变化时自动推送新列表。 */
    fun observeSongs(): Flow<List<Song>>

    /** 触发一次扫描（首次全量 / 后续增量），返回本次扫描到的歌曲数。 */
    suspend fun refresh(): Int

    /** 按歌名 / 艺术家 / 专辑模糊搜索。 */
    fun search(keyword: String): Flow<List<Song>>
}