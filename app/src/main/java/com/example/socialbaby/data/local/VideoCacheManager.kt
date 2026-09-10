package com.example.socialbaby.data.local

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Disk cache for direct video links (parent-provided mp4/m3u8 files played
 * through ExoPlayer). Repeat plays start instantly from disk instead of
 * re-streaming. 500 MB LRU — oldest entries evicted automatically.
 *
 * NOTE: YouTube streams are never cached here (their ToS forbids
 * downloading/re-hosting). Only direct-link playback goes through this cache.
 */
object VideoCacheManager {

    private const val DIR_NAME = "video_cache"
    private const val MAX_BYTES = 500L * 1024 * 1024

    @Volatile
    private var cache: SimpleCache? = null

    /** Call once from Application.onCreate (cheap — only allocates on first use). */
    fun init(appContext: Context) {
        get(appContext)
    }

    @Synchronized
    fun get(appContext: Context): SimpleCache {
        return cache ?: SimpleCache(
            File(appContext.cacheDir, DIR_NAME),
            LeastRecentlyUsedCacheEvictor(MAX_BYTES),
            StandaloneDatabaseProvider(appContext)
        ).also { cache = it }
    }

    /** Wraps [upstream] so reads/writes flow through the disk cache. */
    fun cachedFactory(context: Context, upstream: DataSource.Factory): DataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(get(context.applicationContext))
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun sizeBytes(): Long = try {
        cache?.cacheSpace ?: 0L
    } catch (_: Exception) {
        0L
    }

    /** Clears all cached video data. Safe to call while idle; a playing video
     * keeps its in-memory buffers and re-fetches on next open. */
    @Synchronized
    fun clear(appContext: Context) {
        try {
            cache?.release()
        } catch (_: Exception) {
        }
        cache = null
        try {
            File(appContext.cacheDir, DIR_NAME).deleteRecursively()
        } catch (_: Exception) {
        }
    }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb < 10) String.format("%.1f MB", mb) else "${mb.toLong()} MB"
    }
}
