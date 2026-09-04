package com.example.socialbaby.ui.parent.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialbaby.domain.model.ParentSettings
import com.example.socialbaby.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentSettingsViewModel @Inject constructor(
    private val repo: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<ParentSettings> = repo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ParentSettings())

    fun setDailyLimit(v: Int) = viewModelScope.launch { repo.setDailyLimit(v) }
    fun setBedtimeEnabled(v: Boolean) = viewModelScope.launch { repo.setBedtimeEnabled(v) }
    fun setBedtimeWindow(start: String, end: String) = viewModelScope.launch { repo.setBedtimeWindow(start, end) }
    fun setMaxSession(v: Int) = viewModelScope.launch { repo.setMaxSession(v) }
    fun setShortsEnabled(v: Boolean) = viewModelScope.launch { repo.setShortsEnabled(v) }
    fun setOnlineEnabled(v: Boolean) = viewModelScope.launch { repo.setOnlineLinksEnabled(v) }
}
