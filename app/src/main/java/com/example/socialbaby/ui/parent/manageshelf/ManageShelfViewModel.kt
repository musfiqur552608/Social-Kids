package com.example.socialbaby.ui.parent.manageshelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialbaby.domain.model.Shelf
import com.example.socialbaby.domain.model.ShelfKind
import com.example.socialbaby.domain.repository.ShelfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageShelfViewModel @Inject constructor(
    private val repo: ShelfRepository
) : ViewModel() {

    val shelves: StateFlow<List<Shelf>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addShelf(name: String, kind: ShelfKind = ShelfKind.SHELF) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.insert(Shelf(name = name.trim(), kind = kind, sortOrder = shelves.value.size))
        }
    }

    fun deleteShelf(shelf: Shelf) {
        viewModelScope.launch { repo.delete(shelf) }
    }

    fun renameShelf(shelf: Shelf, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { repo.update(shelf.copy(name = newName.trim())) }
    }
}
