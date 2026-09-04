package com.example.socialbaby.ui.parent.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialbaby.data.local.dao.WatchLogDao
import com.example.socialbaby.data.local.entity.WatchLogEntity
import com.example.socialbaby.domain.model.MediaItem
import com.example.socialbaby.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchHistoryViewModel @Inject constructor(
    private val watchLogDao: WatchLogDao,
    private val mediaRepo: MediaRepository
) : ViewModel() {

    private val _logs = MutableStateFlow<List<WatchLogEntity>>(emptyList())
    val logs: StateFlow<List<WatchLogEntity>> = _logs

    private val _mediaMap = MutableStateFlow<Map<Long, MediaItem?>>(emptyMap())
    val mediaMap: StateFlow<Map<Long, MediaItem?>> = _mediaMap

    init {
        viewModelScope.launch {
            watchLogDao.observeRecent(100).collect { list ->
                _logs.value = list
                val map = mutableMapOf<Long, MediaItem?>()
                list.forEach { log ->
                    if (!map.containsKey(log.mediaItemId)) {
                        map[log.mediaItemId] = mediaRepo.getById(log.mediaItemId)
                    }
                }
                _mediaMap.value = map
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch { watchLogDao.clearAll() }
    }
}
