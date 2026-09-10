package com.example.socialbaby.ui.parent.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialbaby.data.local.VideoCacheManager
import com.example.socialbaby.domain.model.ParentSettings
import com.example.socialbaby.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ParentSettingsViewModel @Inject constructor(
    private val app: Application,
    private val repo: SettingsRepository
) : AndroidViewModel(app) {

    val settings: StateFlow<ParentSettings> = repo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ParentSettings())

    private val _videoCacheSize = MutableStateFlow(VideoCacheManager.formatSize(VideoCacheManager.sizeBytes()))
    val videoCacheSize: StateFlow<String> = _videoCacheSize

    init { refreshCacheSize() }

    fun refreshCacheSize() = viewModelScope.launch {
        _videoCacheSize.value = VideoCacheManager.formatSize(VideoCacheManager.sizeBytes())
    }

    fun clearVideoCache() = viewModelScope.launch {
        withContext(Dispatchers.IO) { VideoCacheManager.clear(app) }
        _videoCacheSize.value = VideoCacheManager.formatSize(0L)
    }

    fun setDailyLimit(v: Int) = viewModelScope.launch { repo.setDailyLimit(v) }
    fun setBedtimeEnabled(v: Boolean) = viewModelScope.launch { repo.setBedtimeEnabled(v) }
    fun setBedtimeWindow(start: String, end: String) = viewModelScope.launch { repo.setBedtimeWindow(start, end) }
    fun setMaxSession(v: Int) = viewModelScope.launch { repo.setMaxSession(v) }
    fun setShortsEnabled(v: Boolean) = viewModelScope.launch { repo.setShortsEnabled(v) }
    fun setOnlineEnabled(v: Boolean) = viewModelScope.launch { repo.setOnlineLinksEnabled(v) }
}
