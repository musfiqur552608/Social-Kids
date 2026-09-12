package com.example.socialbaby.data.backup

import com.example.socialbaby.domain.model.MediaItem
import com.example.socialbaby.domain.model.Shelf
import com.example.socialbaby.domain.repository.MediaRepository
import com.example.socialbaby.domain.repository.ShelfRepository
import kotlinx.coroutines.flow.first

/**
 * CSV backup of the parent-curated content catalog (opens in Excel/Sheets).
 *
 * Columns: title,type,url,videoId,shelf,thumbnail,favorite,dateAdded
 *  - type: LOCAL_VIDEO, LOCAL_IMAGE, YOUTUBE_LINK, ONLINE_VIDEO, ONLINE_IMAGE, GENERIC_LINK
 *  - url: local content URI or online URL
 *  - videoId: YouTube video id (for YOUTUBE_LINK rows)
 *  - shelf: shelf NAME (matched by name on import, created if missing)
 *
 * Watch history and settings are intentionally NOT included — this backs up
 * the added content only. Local content:// URIs are stored as-is; on a
 * different device they may no longer resolve (system permission grants don't
 * transfer) and those rows play as unavailable until re-added.
 */
object MediaCsvBackup {

    private const val HEADER = "title,type,url,videoId,shelf,thumbnail,favorite,dateAdded"

    data class ImportResult(val imported: Int, val skipped: Int)

    suspend fun exportCsv(
        mediaRepo: MediaRepository,
        shelfRepo: ShelfRepository
    ): String {
        val items = mediaRepo.observeAll().first()
        val shelves = try { shelfRepo.getAll() } catch (_: Exception) { emptyList() }
        val shelfNames = shelves.associate { it.id to it.name }
        val sb = StringBuilder(HEADER).append('\n')
        for (item in items) {
            val url = urlOf(item)
            val videoId = (item as? MediaItem.YoutubeLink)?.platformVideoId ?: ""
            sb.append(csvRow(
                item.title,
                typeNameOf(item),
                url,
                videoId,
                shelfNames[item.shelfId] ?: "",
                item.thumbnailPath ?: "",
                if (item.isFavorite) "1" else "0",
                item.dateAdded.toString()
            )).append('\n')
        }
        return sb.toString()
    }

    suspend fun importCsv(
        text: String,
        mediaRepo: MediaRepository,
        shelfRepo: ShelfRepository
    ): ImportResult {
        val rows = parseCsv(text)
        if (rows.isEmpty()) return ImportResult(0, 0)
        val header = rows.first().map { it.trim().lowercase() }
        // Tolerate missing/reordered header: fall back to positional columns.
        val hasHeader = header.contains("title") && header.contains("type") && header.contains("url")
        val data = if (hasHeader) rows.drop(1) else rows
        val idx = if (hasHeader) {
            mapOf(
                "title" to header.indexOf("title"),
                "type" to header.indexOf("type"),
                "url" to header.indexOf("url"),
                "videoid" to header.indexOf("videoid"),
                "shelf" to header.indexOf("shelf"),
                "thumbnail" to header.indexOf("thumbnail"),
                "favorite" to header.indexOf("favorite"),
                "dateadded" to header.indexOf("dateadded")
            )
        } else {
            mapOf("title" to 0, "type" to 1, "url" to 2, "videoid" to 3, "shelf" to 4, "thumbnail" to 5, "favorite" to 6, "dateadded" to 7)
        }
        fun rowGet(row: List<String>, key: String): String {
            val i = idx[key] ?: -1
            return if (i in row.indices) row[i].trim() else ""
        }

        val shelfCache = shelfRepo.getAll()
            .associateBy { it.name.trim().lowercase() }
            .toMutableMap()
        // Idempotency: rows already in the catalog (same type + link) are
        // skipped, so repeat restores can never create duplicates.
        val seen = mediaRepo.observeAll().first()
            .map { typeNameOf(it) to urlOf(it) }
            .toMutableSet()
        var imported = 0
        var skipped = 0
        for (row in data) {
            try {
                val title = rowGet(row, "title")
                val type = rowGet(row, "type").uppercase()
                val url = rowGet(row, "url")
                if (title.isBlank() || url.isBlank()) { skipped++; continue }
                val videoId = rowGet(row, "videoid")
                if (type == "YOUTUBE_LINK" && videoId.isBlank()) { skipped++; continue }
                if ((type to url) in seen) { skipped++; continue }
                val shelfName = rowGet(row, "shelf")
                val shelfId: Long? = if (shelfName.isBlank()) {
                    null
                } else {
                    shelfCache.getOrPut(shelfName.lowercase()) {
                        val id = shelfRepo.insert(Shelf(name = shelfName))
                        Shelf(id = id, name = shelfName)
                    }.id
                }
                val thumbnail = rowGet(row, "thumbnail").ifBlank { null }
                val favorite = rowGet(row, "favorite") == "1"
                val dateAdded = rowGet(row, "dateadded").toLongOrNull() ?: System.currentTimeMillis()
                val item: MediaItem = when (type) {
                    "LOCAL_VIDEO" -> MediaItem.LocalVideo(title = title, thumbnailPath = thumbnail, shelfId = shelfId, dateAdded = dateAdded, isFavorite = favorite, sourceUri = url)
                    "LOCAL_IMAGE" -> MediaItem.LocalImage(title = title, thumbnailPath = thumbnail, shelfId = shelfId, dateAdded = dateAdded, isFavorite = favorite, sourceUri = url)
                    "YOUTUBE_LINK" -> MediaItem.YoutubeLink(title = title, thumbnailPath = thumbnail, shelfId = shelfId, dateAdded = dateAdded, isFavorite = favorite, externalUrl = url, platformVideoId = videoId)
                    "ONLINE_VIDEO" -> MediaItem.OnlineVideo(title = title, thumbnailPath = thumbnail, shelfId = shelfId, dateAdded = dateAdded, isFavorite = favorite, externalUrl = url)
                    "ONLINE_IMAGE" -> MediaItem.OnlineImage(title = title, thumbnailPath = thumbnail, shelfId = shelfId, dateAdded = dateAdded, isFavorite = favorite, externalUrl = url)
                    "GENERIC_LINK" -> MediaItem.GenericLink(title = title, thumbnailPath = thumbnail, shelfId = shelfId, dateAdded = dateAdded, isFavorite = favorite, externalUrl = url)
                    else -> { skipped++; continue }
                }
                mediaRepo.insert(item)
                seen.add(type to url)
                imported++
            } catch (_: Exception) {
                skipped++
            }
        }
        return ImportResult(imported, skipped)
    }

    private fun urlOf(item: MediaItem): String = when (item) {
        is MediaItem.LocalVideo -> item.sourceUri
        is MediaItem.LocalImage -> item.sourceUri
        is MediaItem.YoutubeLink -> item.externalUrl
        is MediaItem.OnlineVideo -> item.externalUrl
        is MediaItem.OnlineImage -> item.externalUrl
        is MediaItem.GenericLink -> item.externalUrl
    }

    private fun typeNameOf(item: MediaItem): String = when (item) {
        is MediaItem.LocalVideo -> "LOCAL_VIDEO"
        is MediaItem.LocalImage -> "LOCAL_IMAGE"
        is MediaItem.YoutubeLink -> "YOUTUBE_LINK"
        is MediaItem.OnlineVideo -> "ONLINE_VIDEO"
        is MediaItem.OnlineImage -> "ONLINE_IMAGE"
        is MediaItem.GenericLink -> "GENERIC_LINK"
    }

    private fun csvRow(vararg fields: String): String = fields.joinToString(",") { escape(it) }

    private fun escape(field: String): String {
        return if (field.contains(',') || field.contains('"') || field.contains('\n') || field.contains('\r')) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
    }

    /** Minimal RFC-4180 reader: quoted fields, "" escapes, CRLF/newlines in fields. */
    fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                    field.append('"'); i += 2
                }
                c == '"' -> { inQuotes = !inQuotes; i++ }
                !inQuotes && c == ',' -> { row.add(field.toString()); field.clear(); i++ }
                !inQuotes && (c == '\n' || c == '\r') -> {
                    row.add(field.toString()); field.clear()
                    if (row.size > 1 || row[0].isNotBlank()) rows.add(row.toList())
                    row.clear()
                    i++
                    if (c == '\r' && i < text.length && text[i] == '\n') i++
                }
                else -> { field.append(c); i++ }
            }
        }
        row.add(field.toString())
        if (row.size > 1 || row[0].isNotBlank()) rows.add(row.toList())
        return rows
    }
}
