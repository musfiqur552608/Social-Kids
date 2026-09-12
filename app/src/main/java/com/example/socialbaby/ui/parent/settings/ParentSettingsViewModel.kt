package com.example.socialbaby.ui.parent.settings

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialbaby.data.backup.MediaCsvBackup
import com.example.socialbaby.data.local.VideoCacheManager
import com.example.socialbaby.domain.model.ParentSettings
import com.example.socialbaby.domain.repository.MediaRepository
import com.example.socialbaby.domain.repository.SettingsRepository
import com.example.socialbaby.domain.repository.ShelfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ParentSettingsViewModel @Inject constructor(
    private val app: Application,
    private val repo: SettingsRepository,
    private val mediaRepo: MediaRepository,
    private val shelfRepo: ShelfRepository
) : AndroidViewModel(app) {

    val settings: StateFlow<ParentSettings> = repo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ParentSettings())

    private val _videoCacheSize = MutableStateFlow(VideoCacheManager.formatSize(VideoCacheManager.sizeBytes()))
    val videoCacheSize: StateFlow<String> = _videoCacheSize

    init { refreshCacheSize() }

    fun refreshCacheSize() = viewModelScope.launch {
        _videoCacheSize.value = VideoCacheManager.formatSize(VideoCacheManager.sizeBytes())
    }

    fun clearVideoCache() = viewModelScope.launch {
        withContext(Dispatchers.IO) { VideoCacheManager.clear(app) }
        _videoCacheSize.value = VideoCacheManager.formatSize(0L)
    }

    // ---- Backup & Restore (CSV catalog of added content) ----

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus

    fun backupFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        return "social-kids-backup-$stamp.csv"
    }

    /** Builds the CSV text for Export / Drive share. Null + status on failure. */
    suspend fun buildBackupCsv(): String? = withContext(Dispatchers.IO) {
        try {
            MediaCsvBackup.exportCsv(mediaRepo, shelfRepo)
        } catch (e: Exception) {
            _backupStatus.value = "Backup failed: ${e.message}"
            null
        }
    }

    fun writeBackupTo(uri: Uri, csv: String) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            try {
                app.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
                _backupStatus.value = "Backup saved."
            } catch (e: Exception) {
                _backupStatus.value = "Save failed: ${e.message}"
            }
        }
    }

    /** Stages the CSV as a shared file and returns its content Uri for ACTION_SEND (Drive). */
    suspend fun prepareShareUri(csv: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val dir = File(app.cacheDir, "shared").apply { mkdirs() }
            dir.listFiles()?.forEach { try { it.delete() } catch (_: Exception) {} }
            val file = File(dir, backupFileName())
            file.writeText(csv, Charsets.UTF_8)
            FileProvider.getUriForFile(app, "com.example.socialbaby.fileprovider", file)
        } catch (e: Exception) {
            _backupStatus.value = "Share failed: ${e.message}"
            null
        }
    }

    fun importBackupFrom(uri: Uri) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            try {
                val text = app.contentResolver.openInputStream(uri)?.use { `in` ->
                    `in`.readBytes().toString(Charsets.UTF_8)
                }.orEmpty()
                if (text.isBlank()) {
                    _backupStatus.value = "Import failed: file is empty."
                    return@withContext
                }
                val result = MediaCsvBackup.importCsv(text, mediaRepo, shelfRepo)
                _backupStatus.value = if (result.skipped == 0) {
                    "Imported ${result.imported} item${if (result.imported == 1) "" else "s"}."
                } else {
                    "Imported ${result.imported}, skipped ${result.skipped} (bad rows or missing links)."
                }
            } catch (e: Exception) {
                _backupStatus.value = "Import failed: ${e.message}"
            }
        }
    }

    fun clearBackupStatus() { _backupStatus.value = null }


    fun setDailyLimit(v: Int) = viewModelScope.launch { repo.setDailyLimit(v) }
    fun setBedtimeEnabled(v: Boolean) = viewModelScope.launch { repo.setBedtimeEnabled(v) }
    fun setBedtimeWindow(start: String, end: String) = viewModelScope.launch { repo.setBedtimeWindow(start, end) }
    fun setMaxSession(v: Int) = viewModelScope.launch { repo.setMaxSession(v) }
    fun setShortsEnabled(v: Boolean) = viewModelScope.launch { repo.setShortsEnabled(v) }
    fun setOnlineEnabled(v: Boolean) = viewModelScope.launch { repo.setOnlineLinksEnabled(v) }
}
