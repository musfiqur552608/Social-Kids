package com.example.socialbaby.ui.child.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialbaby.domain.model.MediaItem
import com.example.socialbaby.domain.repository.MediaRepository
import com.example.socialbaby.domain.usecase.SaveWatchProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val mediaRepo: MediaRepository,
    private val saveProgress: SaveWatchProgressUseCase
) : ViewModel() {

    private val _item = MutableStateFlow<MediaItem?>(null)
    val item: StateFlow<MediaItem?> = _item

    fun load(id: Long) {
        viewModelScope.launch {
            mediaRepo.observeById(id).collect { _item.value = it }
        }
    }

    fun saveProgress(positionMs: Long, deltaMs: Long) {
        val id = _item.value?.id ?: return
        viewModelScope.launch {
            saveProgress(id, positionMs, deltaMs)
        }
    }

    fun recordWatchTime(deltaMs: Long) {
        val id = _item.value?.id ?: return
        viewModelScope.launch {
            mediaRepo.recordWatchTime(id, deltaMs)
        }
    }

    fun toggleFavorite() {
        val id = _item.value?.id ?: return
        viewModelScope.launch { mediaRepo.toggleFavorite(id) }
    }
}
