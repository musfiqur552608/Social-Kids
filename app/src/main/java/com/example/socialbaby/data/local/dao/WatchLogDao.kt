package com.example.socialbaby.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.socialbaby.data.local.entity.WatchLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WatchLogEntity): Long

    @Query("SELECT * FROM watch_logs ORDER BY watchedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<WatchLogEntity>>

    @Query("SELECT * FROM watch_logs WHERE watchedAt >= :since ORDER BY watchedAt DESC")
    fun observeSince(since: Long): Flow<List<WatchLogEntity>>

    @Query("SELECT SUM(durationMs) FROM watch_logs WHERE watchedAt >= :since")
    suspend fun totalWatchSince(since: Long): Long?

    @Query("SELECT SUM(durationMs) FROM watch_logs WHERE watchedAt >= :since")
    fun observeTotalWatchSince(since: Long): Flow<Long?>

    @Query("DELETE FROM watch_logs WHERE watchedAt < :before")
    suspend fun deleteBefore(before: Long)

    @Query("DELETE FROM watch_logs")
    suspend fun clearAll()

    @Query("SELECT * FROM watch_logs ORDER BY watchedAt DESC")
    suspend fun getAll(): List<WatchLogEntity>
}
