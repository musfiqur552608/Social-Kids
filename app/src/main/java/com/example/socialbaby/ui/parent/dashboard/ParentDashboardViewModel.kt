package com.example.socialbaby.ui.parent.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialbaby.domain.model.MediaItem
import com.example.socialbaby.domain.model.Shelf
import com.example.socialbaby.domain.repository.MediaRepository
import com.example.socialbaby.domain.repository.ShelfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentDashboardViewModel @Inject constructor(
    private val mediaRepo: MediaRepository,
    private val shelfRepo: ShelfRepository
) : ViewModel() {

    val allMedia: StateFlow<List<MediaItem>> = mediaRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shelves: StateFlow<List<Shelf>> = shelfRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteItem(item: MediaItem) {
        viewModelScope.launch { mediaRepo.delete(item) }
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch { mediaRepo.toggleFavorite(id) }
    }

    fun updateItem(updated: MediaItem) {
        viewModelScope.launch {
            mediaRepo.update(updated)
        }
    }

    fun moveItemToShelf(id: Long, shelfId: Long?) {
        viewModelScope.launch { mediaRepo.moveToShelf(id, shelfId) }
    }
}
