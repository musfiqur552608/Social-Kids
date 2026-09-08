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

    /** One row in history = one continuous viewing (ticks grouped, not flooded). */
    data class HistoryEntry(
        val mediaItemId: Long,
        val media: MediaItem?,
        val watchedAt: Long,
        val totalDurationMs: Long,
        val segments: Int
    )

    private val _entries = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val entries: StateFlow<List<HistoryEntry>> = _entries

    init {
        viewModelScope.launch {
            // Wide window so grouping sees full sessions, not a clipped tail.
            watchLogDao.observeRecent(500).collect { list ->
                _entries.value = buildSessions(list)
            }
        }
    }

    /**
     * Groups raw 2-second progress ticks into viewing sessions: consecutive
     * ticks for the same video with gaps under [SESSION_GAP_MS] become a single
     * entry with summed duration. Newest session first.
     */
    private suspend fun buildSessions(list: List<WatchLogEntity>): List<HistoryEntry> {
        data class Acc(var endAt: Long, var total: Long, var segments: Int)

        val accs = mutableListOf<Triple<Long, Long, Acc>>() // mediaId, startAt, acc
        for (log in list.sortedBy { it.watchedAt }) {
            val last = accs.lastOrNull()
            if (last != null && last.first == log.mediaItemId &&
                log.watchedAt - last.third.endAt <= SESSION_GAP_MS
            ) {
                last.third.endAt = log.watchedAt
                last.third.total += log.durationMs
                last.third.segments++
            } else {
                accs.add(Triple(log.mediaItemId, log.watchedAt, Acc(log.watchedAt, log.durationMs, 1)))
            }
        }
        val mediaCache = mutableMapOf<Long, MediaItem?>()
        return accs.sortedByDescending { it.third.endAt }.map { (mediaId, startAt, acc) ->
            val media = mediaCache.getOrPut(mediaId) { mediaRepo.getById(mediaId) }
            HistoryEntry(
                mediaItemId = mediaId,
                media = media,
                watchedAt = startAt,
                totalDurationMs = acc.total,
                segments = acc.segments
            )
        }
    }

    companion object {
        private const val SESSION_GAP_MS = 5 * 60 * 1000L
    }

    fun clearAll() {
        viewModelScope.launch { watchLogDao.clearAll() }
    }
}
