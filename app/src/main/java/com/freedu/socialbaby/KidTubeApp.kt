package com.freedu.socialbaby

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.freedu.socialbaby.data.local.VideoCacheManager
import com.freedu.socialbaby.worker.ThumbnailCacheCleanupWorker
import dagger.hilt.android.HiltAndroidApp
import java.net.InetAddress
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class KidTubeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        VideoCacheManager.init(this)
        prewarmWebView()
        scheduleWorkers()
    }

    /**
     * Two-part warmup so the FIRST embedded video starts fast:
     *
     * 1) DNS cache — background getAllByName for the image/video hosts. Helps
     *    Coil thumbnails + oEmbed immediately (JVM resolver cache is shared).
     * 2) Chromium socket pool — a throwaway WebView loads a page of
     *    <link rel="preconnect"> hints. Chromium opens DNS+TCP+TLS to the
     *    YouTube hosts up-front; those sockets are pooled per network context
     *    and REUSED by the player's WebView, cutting the first-video handshake
     *    (DNS + TCP + TLS ≈ 1–3 s on mobile) to near zero.
     *
     * NOTE: WebView must be touched on a Looper thread. The old version used a
     * raw Thread, every WebView call threw, and catch{} hid it — nothing was
     * ever warmed.
     */
    private fun prewarmWebView() {
        Thread {
            val hosts = arrayOf(
                "www.youtube.com", "i.ytimg.com", "img.youtube.com",
                "www.youtube-nocookie.com", "ytimg.com"
            )
            for (h in hosts) {
                try { InetAddress.getAllByName(h) } catch (_: Exception) {}
            }
        }.start()

        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.postDelayed({
            try {
                val preconnectHtml = """
                    <!DOCTYPE html><html><head>
                    <link rel="preconnect" href="https://www.youtube.com">
                    <link rel="preconnect" href="https://www.youtube-nocookie.com" crossorigin>
                    <link rel="preconnect" href="https://i.ytimg.com">
                    <link rel="preconnect" href="https://www.google.com">
                    <link rel="dns-prefetch" href="https://www.youtube-nocookie.com">
                    <link rel="dns-prefetch" href="https://ytimg.com">
                    </head><body></body></html>
                """.trimIndent()
                val warm = WebView(applicationContext).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    loadDataWithBaseURL("https://www.youtube.com", preconnectHtml, "text/html", "utf-8", null)
                }
                // Keep the pooled sockets alive through the typical
                // "open app → tap a video" window, then release.
                mainHandler.postDelayed({
                    try { warm.destroy() } catch (_: Exception) {}
                }, 30_000)
            } catch (_: Exception) {}
        }, 400)
    }

    private fun scheduleWorkers() {
        val cleanup = PeriodicWorkRequestBuilder<ThumbnailCacheCleanupWorker>(7, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "thumb_cleanup", ExistingPeriodicWorkPolicy.KEEP, cleanup
        )
    }
}
