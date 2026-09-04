package com.example.socialbaby.ui.child.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.socialbaby.domain.model.MediaItem
import com.example.socialbaby.domain.model.Shelf
import com.example.socialbaby.domain.repository.MediaRepository
import com.example.socialbaby.domain.repository.ShelfRepository
import com.example.socialbaby.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepo: MediaRepository,
    private val shelfRepo: ShelfRepository,
    settingsRepo: SettingsRepository
) : ViewModel() {

    val shelves: Flow<List<Shelf>> = shelfRepo.observeAll()

    private val selectedShelf = MutableStateFlow<Long?>(null)

    fun selectShelf(id: Long?) { selectedShelf.value = id }

    val selectedShelfId: StateFlow<Long?> = selectedShelf

    val pagedMedia: Flow<PagingData<MediaItem>> = selectedShelf.flatMapLatest { shelfId ->
        if (shelfId == null) mediaRepo.pagingAll() else mediaRepo.pagingByShelf(shelfId)
    }.cachedIn(viewModelScope)

    // For grid without paging fallback (simple list)
    val allMediaFlow = mediaRepo.observeAll()
}
