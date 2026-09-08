package com.example.socialbaby.ui.parent.addcontent

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialbaby.domain.model.Shelf
import com.example.socialbaby.domain.repository.ShelfRepository
import com.example.socialbaby.domain.usecase.AddLocalMediaUseCase
import com.example.socialbaby.domain.usecase.AddOnlineLinkUseCase
import com.example.socialbaby.util.UrlParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddContentViewModel @Inject constructor(
    private val addLocal: AddLocalMediaUseCase,
    private val addOnline: AddOnlineLinkUseCase,
    shelfRepo: ShelfRepository
) : ViewModel() {

    val shelves: StateFlow<List<Shelf>> = MutableStateFlow(emptyList())

    private val _link = MutableStateFlow("")
    val link: StateFlow<String> = _link

    private val _selectedShelf = MutableStateFlow<Long?>(null)
    val selectedShelf: StateFlow<Long?> = _selectedShelf

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _shelfList = MutableStateFlow<List<Shelf>>(emptyList())
    val shelfList: StateFlow<List<Shelf>> = _shelfList

    init {
        viewModelScope.launch {
            shelfRepo.observeAll().collect { _shelfList.value = it }
        }
    }

    fun onLinkChange(v: String) { _link.value = v; _message.value = null }
    fun onShelfSelected(id: Long?) { _selectedShelf.value = id }

    fun addLink() {
        val url = _link.value.trim()
        if (!UrlParser.isSupported(url)) {
            _message.value = "Unsupported link. TikTok and Facebook links aren't supported — use a YouTube link or a direct video/image link."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = addOnline(url, _selectedShelf.value)
            _isLoading.value = false
            if (result.isSuccess) {
                _message.value = "Added!"
                _link.value = ""
            } else {
                _message.value = "Failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun addLocalUri(uri: Uri, mime: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                addLocal(uri, mime, _selectedShelf.value)
                _message.value = "Local media added!"
            } catch (e: Exception) {
                _message.value = "Failed: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun clearMessage() { _message.value = null }
}
