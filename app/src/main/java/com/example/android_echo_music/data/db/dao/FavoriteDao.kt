package com.example.android_echo_music.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.android_echo_music.data.db.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE songId = :songId")
    suspend fun delete(songId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE songId = :songId)")
    fun observeIsFavorite(songId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE songId = :songId)")
    suspend fun isFavorite(songId: Long): Boolean

    @Query("SELECT songId FROM favorite ORDER BY createdAt DESC")
    fun observeFavoriteSongIds(): Flow<List<Long>>
}
