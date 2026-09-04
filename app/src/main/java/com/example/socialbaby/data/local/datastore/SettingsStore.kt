package com.example.socialbaby.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "kidtube_settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dailyLimitKey = intPreferencesKey("daily_limit_minutes") // 0 = unlimited
    private val bedtimeEnabledKey = booleanPreferencesKey("bedtime_enabled")
    private val bedtimeStartKey = stringPreferencesKey("bedtime_start") // HH:mm
    private val bedtimeEndKey = stringPreferencesKey("bedtime_end")
    private val maxSessionKey = intPreferencesKey("max_session_minutes")
    private val shortsEnabledKey = booleanPreferencesKey("shorts_enabled")
    private val onlineLinksEnabledKey = booleanPreferencesKey("online_links_enabled")

    val dailyLimitFlow: Flow<Int> = context.settingsDataStore.data.map { it[dailyLimitKey] ?: 60 }
    val bedtimeEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { it[bedtimeEnabledKey] ?: false }
    val bedtimeStartFlow: Flow<String> = context.settingsDataStore.data.map { it[bedtimeStartKey] ?: "20:00" }
    val bedtimeEndFlow: Flow<String> = context.settingsDataStore.data.map { it[bedtimeEndKey] ?: "07:00" }
    val maxSessionFlow: Flow<Int> = context.settingsDataStore.data.map { it[maxSessionKey] ?: 30 }
    val shortsEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { it[shortsEnabledKey] ?: true }
    val onlineLinksEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { it[onlineLinksEnabledKey] ?: true }

    suspend fun setDailyLimit(minutes: Int) {
        context.settingsDataStore.edit { it[dailyLimitKey] = minutes }
    }

    suspend fun setBedtimeEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[bedtimeEnabledKey] = enabled }
    }

    suspend fun setBedtimeWindow(start: String, end: String) {
        context.settingsDataStore.edit {
            it[bedtimeStartKey] = start
            it[bedtimeEndKey] = end
        }
    }

    suspend fun setMaxSession(minutes: Int) {
        context.settingsDataStore.edit { it[maxSessionKey] = minutes }
    }

    suspend fun setShortsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[shortsEnabledKey] = enabled }
    }

    suspend fun setOnlineLinksEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[onlineLinksEnabledKey] = enabled }
    }
}

data class KidSettings(
    val dailyLimitMinutes: Int = 60,
    val bedtimeEnabled: Boolean = false,
    val bedtimeStart: String = "20:00",
    val bedtimeEnd: String = "07:00",
    val maxSessionMinutes: Int = 30,
    val shortsEnabled: Boolean = true,
    val onlineLinksEnabled: Boolean = true
)
