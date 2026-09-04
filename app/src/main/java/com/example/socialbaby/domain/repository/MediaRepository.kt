package com.example.socialbaby.domain.repository

import androidx.paging.PagingData
import com.example.socialbaby.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun observeAll(): Flow<List<MediaItem>>
    fun observeByShelf(shelfId: Long): Flow<List<MediaItem>>
    fun pagingAll(): Flow<PagingData<MediaItem>>
    fun pagingByShelf(shelfId: Long): Flow<PagingData<MediaItem>>
    fun pagingByType(type: String): Flow<PagingData<MediaItem>>
    fun pagingShorts(): Flow<PagingData<MediaItem>>
    fun pagingPhotos(): Flow<PagingData<MediaItem>>
    suspend fun getById(id: Long): MediaItem?
    fun observeById(id: Long): Flow<MediaItem?>
    suspend fun insert(item: MediaItem): Long
    suspend fun update(item: MediaItem)
    suspend fun delete(item: MediaItem)
    suspend fun deleteById(id: Long)
    suspend fun saveWatchProgress(id: Long, positionMs: Long, deltaMs: Long)
    suspend fun toggleFavorite(id: Long)
    suspend fun moveToShelf(id: Long, shelfId: Long?)
}
