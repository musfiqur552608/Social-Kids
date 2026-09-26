package com.freedu.socialbaby.domain.usecase

import com.freedu.socialbaby.domain.repository.MediaRepository
import javax.inject.Inject

class SaveWatchProgressUseCase @Inject constructor(private val repo: MediaRepository) {
    suspend operator fun invoke(mediaId: Long, positionMs: Long, deltaMs: Long) {
        repo.saveWatchProgress(mediaId, positionMs, deltaMs)
    }
}
