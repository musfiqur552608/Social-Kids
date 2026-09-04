package com.example.socialbaby

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.socialbaby.worker.ThumbnailCacheCleanupWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class KidTubeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleWorkers()
    }

    private fun scheduleWorkers() {
        val cleanup = PeriodicWorkRequestBuilder<ThumbnailCacheCleanupWorker>(7, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "thumb_cleanup", ExistingPeriodicWorkPolicy.KEEP, cleanup
        )
    }
}
