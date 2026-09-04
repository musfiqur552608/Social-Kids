package com.example.socialbaby.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore(name = "parent_auth")

@Singleton
class ParentAuthStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val pinHashKey = stringPreferencesKey("pin_hash")
    private val biometricEnabledKey = booleanPreferencesKey("biometric_enabled")
    private val isPinSetKey = booleanPreferencesKey("is_pin_set")

    val pinHashFlow: Flow<String?> = context.authDataStore.data.map { it[pinHashKey] }
    val biometricEnabledFlow: Flow<Boolean> = context.authDataStore.data.map { it[biometricEnabledKey] ?: false }
    val isPinSetFlow: Flow<Boolean> = context.authDataStore.data.map { it[isPinSetKey] ?: false }

    suspend fun setPin(pin: String) {
        val hash = hashPin(pin)
        context.authDataStore.edit { prefs ->
            prefs[pinHashKey] = hash
            prefs[isPinSetKey] = true
        }
    }

    suspend fun getPinHash(): String? {
        return context.authDataStore.data.first()[pinHashKey]
    }

    suspend fun checkPin(pin: String): Boolean {
        val hash = getPinHash() ?: return false
        return hashPin(pin) == hash
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.authDataStore.edit { it[biometricEnabledKey] = enabled }
    }

    suspend fun clearPin() {
        context.authDataStore.edit {
            it.remove(pinHashKey)
            it[isPinSetKey] = false
        }
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
