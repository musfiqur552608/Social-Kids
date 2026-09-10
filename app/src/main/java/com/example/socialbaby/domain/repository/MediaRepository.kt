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
    /** Same filter as pagingShorts but as a plain list flow (for infinite-loop Shorts). */
    fun observeShorts(): Flow<List<MediaItem>>
    fun pagingPhotos(): Flow<PagingData<MediaItem>>
    /** Title search; when [shelfId] is null searches everything. */
    fun pagingSearch(query: String, shelfId: Long?): Flow<PagingData<MediaItem>>
    suspend fun getById(id: Long): MediaItem?
    fun observeById(id: Long): Flow<MediaItem?>
    suspend fun insert(item: MediaItem): Long
    suspend fun update(item: MediaItem)
    suspend fun delete(item: MediaItem)
    suspend fun deleteById(id: Long)
    suspend fun saveWatchProgress(id: Long, positionMs: Long, deltaMs: Long)
    /** Records watched time WITHOUT touching resume position (for WebView
     * playback where no position is knowable). Never pass 0/stale positions
     * into saveWatchProgress — it would wipe the resume point. */
    suspend fun recordWatchTime(id: Long, deltaMs: Long)
    suspend fun toggleFavorite(id: Long)
    suspend fun moveToShelf(id: Long, shelfId: Long?)
}
