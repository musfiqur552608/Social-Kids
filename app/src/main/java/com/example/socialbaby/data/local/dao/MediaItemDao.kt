package com.example.socialbaby.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.socialbaby.data.local.entity.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MediaItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaItemEntity>)

    @Update
    suspend fun update(item: MediaItemEntity)

    @Delete
    suspend fun delete(item: MediaItemEntity)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getById(id: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE id = :id")
    fun observeById(id: Long): Flow<MediaItemEntity?>

    @Query("SELECT * FROM media_items ORDER BY dateAdded DESC")
    fun observeAll(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE shelfId = :shelfId ORDER BY sortOrder ASC, dateAdded DESC")
    fun observeByShelf(shelfId: Long): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE type IN (:types) ORDER BY dateAdded DESC")
    fun observeByTypes(types: List<String>): Flow<List<MediaItemEntity>>

    // Paging sources
    @Query("SELECT * FROM media_items ORDER BY dateAdded DESC")
    fun pagingAll(): PagingSource<Int, MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE shelfId = :shelfId ORDER BY sortOrder ASC, dateAdded DESC")
    fun pagingByShelf(shelfId: Long): PagingSource<Int, MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE type = :type ORDER BY dateAdded DESC")
    fun pagingByType(type: String): PagingSource<Int, MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun pagingFavorites(): PagingSource<Int, MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE type IN (:types) ORDER BY dateAdded DESC")
    fun pagingByTypes(types: List<String>): PagingSource<Int, MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE title LIKE '%' || :query || '%' ESCAPE '\\' ORDER BY dateAdded DESC")
    fun pagingSearch(query: String): PagingSource<Int, MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE shelfId = :shelfId AND title LIKE '%' || :query || '%' ESCAPE '\\' ORDER BY sortOrder ASC, dateAdded DESC")
    fun pagingSearchInShelf(query: String, shelfId: Long): PagingSource<Int, MediaItemEntity>

    @Query("UPDATE media_items SET lastWatchedPositionMs = :position, lastWatchedAt = :at, totalWatchTimeMs = totalWatchTimeMs + :delta WHERE id = :id")
    suspend fun updateWatchProgress(id: Long, position: Long, at: Long, delta: Long)

    @Query("UPDATE media_items SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean)

    @Query("UPDATE media_items SET shelfId = :shelfId WHERE id = :id")
    suspend fun moveToShelf(id: Long, shelfId: Long?)

    // One-time cleanup after removing TikTok/Facebook support: drop rows whose
    // types can never play again. Also drops old YouTube rows with unplayable
    // IDs (hashcodes from the first parser version — valid IDs are 11 chars).
    @Query("DELETE FROM media_items WHERE type IN ('TIKTOK_LINK','FACEBOOK_LINK')")
    suspend fun deleteUnsupportedTypes(): Int

    @Query("DELETE FROM media_items WHERE type = 'YOUTUBE_LINK' AND (platformVideoId IS NULL OR length(platformVideoId) != 11)")
    suspend fun deleteInvalidYoutubeLinks(): Int

    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun count(): Int
}
