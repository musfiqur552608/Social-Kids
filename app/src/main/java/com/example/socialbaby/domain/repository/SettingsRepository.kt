package com.example.socialbaby.domain.repository

import com.example.socialbaby.domain.model.ParentSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<ParentSettings>
    suspend fun setDailyLimit(minutes: Int)
    suspend fun setBedtimeEnabled(enabled: Boolean)
    suspend fun setBedtimeWindow(start: String, end: String)
    suspend fun setMaxSession(minutes: Int)
    suspend fun setShortsEnabled(enabled: Boolean)
    suspend fun setOnlineLinksEnabled(enabled: Boolean)
}
