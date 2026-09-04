package com.example.socialbaby.domain.repository

import kotlinx.coroutines.flow.Flow

interface ParentAuthRepository {
    val isPinSetFlow: Flow<Boolean>
    val biometricEnabledFlow: Flow<Boolean>
    suspend fun setPin(pin: String)
    suspend fun verifyPin(pin: String): Boolean
    suspend fun isPinSet(): Boolean
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun clearPin()
}
