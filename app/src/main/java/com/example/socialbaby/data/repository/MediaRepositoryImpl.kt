package com.example.socialbaby.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.socialbaby.data.local.dao.MediaItemDao
import com.example.socialbaby.data.local.dao.WatchLogDao
import com.example.socialbaby.data.local.entity.MediaType
import com.example.socialbaby.data.local.entity.WatchLogEntity
import com.example.socialbaby.data.mapper.toDomain
import com.example.socialbaby.data.mapper.toEntity
import com.example.socialbaby.domain.model.MediaItem
import com.example.socialbaby.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    private val dao: MediaItemDao,
    private val watchLogDao: WatchLogDao
) : MediaRepository {

    override fun observeAll(): Flow<List<MediaItem>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeByShelf(shelfId: Long): Flow<List<MediaItem>> = dao.observeByShelf(shelfId).map { list -> list.map { it.toDomain() } }

    override fun pagingAll(): Flow<PagingData<MediaItem>> = Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
        dao.pagingAll()
    }.flow.map { pagingData -> pagingData.map { it.toDomain() } }

    override fun pagingByShelf(shelfId: Long): Flow<PagingData<MediaItem>> = Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
        dao.pagingByShelf(shelfId)
    }.flow.map { pagingData -> pagingData.map { it.toDomain() } }

    override fun pagingByType(type: String): Flow<PagingData<MediaItem>> = Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
        dao.pagingByType(type)
    }.flow.map { pagingData -> pagingData.map { it.toDomain() } }

    override fun pagingShorts(): Flow<PagingData<MediaItem>> {
        return Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
            dao.pagingByTypes(listOf(MediaType.YOUTUBE_LINK.name, MediaType.LOCAL_VIDEO.name, MediaType.ONLINE_VIDEO.name, MediaType.GENERIC_LINK.name))
        }.flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    override fun pagingPhotos(): Flow<PagingData<MediaItem>> = Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
        dao.pagingByTypes(listOf(MediaType.LOCAL_IMAGE.name, MediaType.ONLINE_IMAGE.name))
    }.flow.map { pagingData -> pagingData.map { it.toDomain() } }

    override suspend fun getById(id: Long): MediaItem? = dao.getById(id)?.toDomain()

    override fun observeById(id: Long): Flow<MediaItem?> = dao.observeById(id).map { it?.toDomain() }

    override suspend fun insert(item: MediaItem): Long = dao.insert(item.toEntity())

    override suspend fun update(item: MediaItem) = dao.update(item.toEntity())

    override suspend fun delete(item: MediaItem) = dao.delete(item.toEntity())

    override suspend fun deleteById(id: Long) = dao.deleteById(id)

    override suspend fun saveWatchProgress(id: Long, positionMs: Long, deltaMs: Long) {
        dao.updateWatchProgress(id, positionMs, System.currentTimeMillis(), deltaMs)
        if (deltaMs > 0) {
            watchLogDao.insert(WatchLogEntity(mediaItemId = id, durationMs = deltaMs))
        }
    }

    override suspend fun toggleFavorite(id: Long) {
        val item = dao.getById(id) ?: return
        dao.setFavorite(id, !item.isFavorite)
    }

    override suspend fun moveToShelf(id: Long, shelfId: Long?) {
        dao.moveToShelf(id, shelfId)
    }
}
