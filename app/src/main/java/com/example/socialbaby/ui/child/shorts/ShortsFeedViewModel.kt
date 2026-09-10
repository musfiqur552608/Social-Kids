package com.example.socialbaby.ui.child.shorts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialbaby.domain.model.MediaItem
import com.example.socialbaby.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShortsFeedViewModel @Inject constructor(
    private val mediaRepo: MediaRepository
) : ViewModel() {

    /**
     * Infinite-loop feed order. The screen renders a virtual endless pager as
     * order[page % order.size]; every completed lap re-shuffles so kids never
     * see the same sequence twice in a row.
     */
    private val _order = MutableStateFlow<List<MediaItem>>(emptyList())
    val order: StateFlow<List<MediaItem>> = _order.asStateFlow()

    private var lastBase: List<MediaItem> = emptyList()
    private var lastIds: Set<Long> = emptySet()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun setQuery(q: String) { _query.value = q }

    init {
        viewModelScope.launch {
            mediaRepo.observeShorts().collect { base -> mergeBase(base) }
        }
    }

    private fun mergeBase(base: List<MediaItem>) {
        lastBase = base
        val ids = base.map { it.id }.toSet()
        if (ids != lastIds) {
            // Items added/removed (e.g. parent editing) → fresh random order.
            lastIds = ids
            _order.value = if (base.size > 1) base.shuffled() else base
        } else {
            // Same items (e.g. favorite toggled) → refresh content in place,
            // keep positions so the playing video never jumps.
            val fresh = base.associateBy { it.id }
            _order.value = _order.value.mapNotNull { fresh[it.id] }
        }
    }

    /**
     * Called when the pager crosses into a new lap. Re-shuffles the upcoming
     * sequence but keeps the currently-playing item at [currentSlot] so there
     * is no visual jump — the next videos are simply a fresh random order.
     */
    fun onLapCompleted(currentItemId: Long, currentSlot: Int) {
        val base = lastBase
        if (base.size <= 1) return
        val shuffled = base.shuffled().toMutableList()
        val target = currentSlot % shuffled.size
        val curIdx = shuffled.indexOfFirst { it.id == currentItemId }
        if (curIdx >= 0 && curIdx != target) {
            val tmp = shuffled[target]
            shuffled[target] = shuffled[curIdx]
            shuffled[curIdx] = tmp
        }
        _order.value = shuffled
    }

    fun saveWatchProgress(id: Long, positionMs: Long, deltaMs: Long) {
        viewModelScope.launch { mediaRepo.saveWatchProgress(id, positionMs, deltaMs) }
    }

    fun recordWatchTime(id: Long, deltaMs: Long) {
        viewModelScope.launch { mediaRepo.recordWatchTime(id, deltaMs) }
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch { mediaRepo.toggleFavorite(id) }
    }
}
