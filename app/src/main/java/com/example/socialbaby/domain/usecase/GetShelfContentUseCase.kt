package com.example.socialbaby.domain.usecase

import androidx.paging.PagingData
import com.example.socialbaby.domain.model.MediaItem
import com.example.socialbaby.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetShelfContentUseCase @Inject constructor(private val repo: MediaRepository) {
    operator fun invoke(shelfId: Long?): Flow<PagingData<MediaItem>> =
        if (shelfId == null) repo.pagingAll() else repo.pagingByShelf(shelfId)
}
