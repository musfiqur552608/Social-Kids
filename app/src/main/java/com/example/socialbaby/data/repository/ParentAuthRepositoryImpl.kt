package com.example.socialbaby.data.repository

import com.example.socialbaby.data.local.datastore.ParentAuthStore
import com.example.socialbaby.domain.repository.ParentAuthRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ParentAuthRepositoryImpl @Inject constructor(
    private val store: ParentAuthStore
) : ParentAuthRepository {
    override val isPinSetFlow = store.isPinSetFlow
    override val biometricEnabledFlow = store.biometricEnabledFlow
    override suspend fun setPin(pin: String) = store.setPin(pin)
    override suspend fun verifyPin(pin: String): Boolean = store.checkPin(pin)
    override suspend fun isPinSet(): Boolean = store.isPinSetFlow.first()
    override suspend fun setBiometricEnabled(enabled: Boolean) = store.setBiometricEnabled(enabled)
    override suspend fun clearPin() = store.clearPin()
}
