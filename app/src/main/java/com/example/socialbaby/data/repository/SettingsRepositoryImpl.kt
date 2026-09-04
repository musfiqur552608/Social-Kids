package com.example.socialbaby.data.repository

import com.example.socialbaby.data.local.datastore.SettingsStore
import com.example.socialbaby.domain.model.ParentSettings
import com.example.socialbaby.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val store: SettingsStore
) : SettingsRepository {
    override val settingsFlow: Flow<ParentSettings> = combine(
        combine(store.dailyLimitFlow, store.bedtimeEnabledFlow, store.bedtimeStartFlow) { a, b, c -> Triple(a, b, c) },
        combine(store.bedtimeEndFlow, store.maxSessionFlow, store.shortsEnabledFlow) { a, b, c -> Triple(a, b, c) },
        store.onlineLinksEnabledFlow
    ) { first, second, online ->
        val (daily, bedEnabled, start) = first
        val (end, maxSess, shorts) = second
        ParentSettings(
            dailyLimitMinutes = daily,
            bedtimeEnabled = bedEnabled,
            bedtimeStart = start,
            bedtimeEnd = end,
            maxSessionMinutes = maxSess,
            shortsEnabled = shorts,
            onlineLinksEnabled = online
        )
    }

    override suspend fun setDailyLimit(minutes: Int) = store.setDailyLimit(minutes)
    override suspend fun setBedtimeEnabled(enabled: Boolean) = store.setBedtimeEnabled(enabled)
    override suspend fun setBedtimeWindow(start: String, end: String) = store.setBedtimeWindow(start, end)
    override suspend fun setMaxSession(minutes: Int) = store.setMaxSession(minutes)
    override suspend fun setShortsEnabled(enabled: Boolean) = store.setShortsEnabled(enabled)
    override suspend fun setOnlineLinksEnabled(enabled: Boolean) = store.setOnlineLinksEnabled(enabled)
}
