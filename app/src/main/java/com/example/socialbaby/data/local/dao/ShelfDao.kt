package com.example.socialbaby.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.socialbaby.data.local.entity.ShelfEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShelfDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shelf: ShelfEntity): Long

    @Update
    suspend fun update(shelf: ShelfEntity)

    @Delete
    suspend fun delete(shelf: ShelfEntity)

    @Query("DELETE FROM shelves WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM shelves ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<ShelfEntity>>

    @Query("SELECT * FROM shelves WHERE id = :id")
    suspend fun getById(id: Long): ShelfEntity?

    @Query("SELECT * FROM shelves ORDER BY sortOrder ASC")
    suspend fun getAll(): List<ShelfEntity>

    @Query("SELECT COUNT(*) FROM shelves")
    suspend fun count(): Int
}
