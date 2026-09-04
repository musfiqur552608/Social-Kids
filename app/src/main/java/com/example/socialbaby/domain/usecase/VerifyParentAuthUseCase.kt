package com.example.socialbaby.domain.usecase

import com.example.socialbaby.domain.repository.ParentAuthRepository
import javax.inject.Inject

class VerifyParentAuthUseCase @Inject constructor(private val repo: ParentAuthRepository) {
    suspend operator fun invoke(pin: String): Boolean = repo.verifyPin(pin)
    suspend fun isPinSet(): Boolean = repo.isPinSet()
}
