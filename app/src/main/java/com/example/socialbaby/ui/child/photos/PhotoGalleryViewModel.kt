package com.example.socialbaby.ui.child.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.socialbaby.domain.model.MediaItem
import com.example.socialbaby.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class PhotoGalleryViewModel @Inject constructor(
    mediaRepo: MediaRepository
) : ViewModel() {
    val photos: Flow<PagingData<MediaItem>> = mediaRepo.pagingPhotos().cachedIn(viewModelScope)
}
