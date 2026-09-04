package com.example.socialbaby.domain.usecase

import com.example.socialbaby.domain.repository.MediaRepository
import javax.inject.Inject

class SaveWatchProgressUseCase @Inject constructor(private val repo: MediaRepository) {
    suspend operator fun invoke(mediaId: Long, positionMs: Long, deltaMs: Long) {
        repo.saveWatchProgress(mediaId, positionMs, deltaMs)
    }
}
