package com.example.socialbaby

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.socialbaby.data.local.VideoCacheManager
import com.example.socialbaby.worker.ThumbnailCacheCleanupWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class KidTubeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        VideoCacheManager.init(this)
        prewarmWebView()
        scheduleWorkers()
    }

    /** Warm WebView engine + dns in background so first embedded video loads faster.
     *  Runs after app cold start; takes <100ms, never blocks the UI thread. */
    private fun prewarmWebView() {
        val appContext = this
        Thread {
            try {
                android.webkit.WebStorage.getInstance()
                android.webkit.WebView(appContext).apply {
                    settings.javaScriptEnabled = true
                    loadUrl("about:blank", mapOf("User-Agent" to "Mozilla/5.0"))
                    postDelayed({
                        try { destroy() } catch (_: Exception) {}
                    }, 300)
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun scheduleWorkers() {
        val cleanup = PeriodicWorkRequestBuilder<ThumbnailCacheCleanupWorker>(7, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "thumb_cleanup", ExistingPeriodicWorkPolicy.KEEP, cleanup
        )
    }
}
