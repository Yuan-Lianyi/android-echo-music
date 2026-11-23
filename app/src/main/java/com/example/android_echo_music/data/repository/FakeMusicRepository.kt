package com.example.android_echo_music.data.repository

import androidx.core.net.toUri
import com.example.android_echo_music.BuildConfig
import com.example.android_echo_music.R
import com.example.android_echo_music.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * W1 假数据实现（[B] 在 W2 用 MediaStore + Room 的 `MediaStoreMusicRepository` 替换）。
 *
 * 所有歌曲都指向内置的 `res/raw/demo_tone.wav`，保证「列表 → 播放页 → 出声」链路可演示；
 * 因此时长统一按示例音频（8 秒）填写，避免列表时长与播放页进度不一致。
 */
class FakeMusicRepository : MusicRepository {

    // 直接引用 R.raw，避免打包后资源被裁剪；ExoPlayer 的 RawResourceDataSource 支持该 URI
    private val demoUri: String =
        "android.resource://${BuildConfig.APPLICATION_ID}/${R.raw.demo_tone}".toUri().toString()

    private val songs: List<Song> = listOf(
        Song(1L, "Demo Tone", "Echo Music", "示例音频", 8_000L, demoUri, folder = "/Music/Demo"),
        Song(2L, "夜行列车", "林间乐队", "城市回声", 8_000L, demoUri, folder = "/Music/Demo"),
        Song(3L, "海边的信号", "林间乐队", "城市回声", 8_000L, demoUri, folder = "/Music/Demo"),
        Song(4L, "无人的楼梯", "纸飞机", "纸飞机", 8_000L, demoUri, folder = "/Music/Demo"),
        Song(5L, "夏日回声", "纸飞机", "纸飞机", 8_000L, demoUri, folder = "/Music/Demo"),
        Song(6L, "Slow Motion", "North Light", "Slow Motion", 8_000L, demoUri, folder = "/Music/Demo"),
        Song(7L, "雨停之前", "陈小满", "雨停之前", 8_000L, demoUri, folder = "/Music/Demo"),
        Song(8L, "Midnight Walk", "North Light", "Slow Motion", 8_000L, demoUri, folder = "/Music/Demo")
    )

    override fun observeSongs(): Flow<List<Song>> = flowOf(songs)

    override suspend fun refresh(): Int = songs.size

    override fun search(keyword: String): Flow<List<Song>> {
        val query = keyword.trim()
        if (query.isEmpty()) return flowOf(emptyList())
        return flowOf(
            songs.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                    song.artist.contains(query, ignoreCase = true) ||
                    song.album.contains(query, ignoreCase = true)
            }
        )
    }
}