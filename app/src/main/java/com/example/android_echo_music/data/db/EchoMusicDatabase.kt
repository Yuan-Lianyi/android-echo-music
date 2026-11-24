package com.example.android_echo_music.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.android_echo_music.data.db.dao.FavoriteDao
import com.example.android_echo_music.data.db.dao.PlayHistoryDao
import com.example.android_echo_music.data.db.dao.PlaylistDao
import com.example.android_echo_music.data.db.entity.FavoriteEntity
import com.example.android_echo_music.data.db.entity.PlayHistoryEntity
import com.example.android_echo_music.data.db.entity.PlaylistEntity
import com.example.android_echo_music.data.db.entity.PlaylistSongEntity

/**
 * 应用唯一的 Room 数据库。
 *
 * 这里只保存用户产生的数据（歌单、歌单关联、收藏、播放历史）；
 * 歌曲元数据和音频文件本身不入库，实时从 MediaStore 读取。
 */
@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        FavoriteEntity::class,
        PlayHistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class EchoMusicDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao

    abstract fun favoriteDao(): FavoriteDao

    abstract fun playHistoryDao(): PlayHistoryDao

    companion object {
        private const val DATABASE_NAME = "echo_music.db"

        @Volatile
        private var instance: EchoMusicDatabase? = null

        fun getInstance(context: Context): EchoMusicDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EchoMusicDatabase::class.java,
                    DATABASE_NAME
                ).build().also { database ->
                    instance = database
                }
            }
        }
    }
}
