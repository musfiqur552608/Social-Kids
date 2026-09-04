package com.example.socialbaby.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ThumbnailCacheCleanupWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            val cache = applicationContext.cacheDir
            val now = System.currentTimeMillis()
            val sevenDays = 7L * 24 * 60 * 60 * 1000
            cache.listFiles()?.forEach { file ->
                if (file.name.startsWith("thumb_") && now - file.lastModified() > sevenDays) {
                    file.delete()
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
