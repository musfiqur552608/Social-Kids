package com.freedu.socialbaby.ui.child.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.freedu.socialbaby.domain.model.MediaItem
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

/**
 * One warm YouTube player for the whole app.
 *
 * The 3-4s a child waits after tapping a tile is almost entirely player boot
 * (WebView + youtube iframe_api + DASH manifest) happening AFTER the click.
 * This object moves that work before the click:
 *
 *  - install(activity) at app start boots the IFrame player immediately and
 *    parks the view in a 1x1 FrameLayout on the activity decor (attached to
 *    the window so Chromium actually renders/buffers, but invisible).
 *  - preload(videoId) fires on TOUCH-DOWN of a Home tile: mutes and starts
 *    loadVideo right away — the stream is already buffering while Compose
 *    navigates to PlayerScreen.
 *  - acquire()/release() hand the SAME view to PlayerScreen's AndroidView.
 *    On attach the video is usually already PLAYING → picture is there
 *    instantly. On release it goes back to the 1x1 host, paused + muted.
 *
 * The view observes the ACTIVITY lifecycle only (library releases itself on
 * ON_DESTROY) — never a NavBackStackEntry, which would kill the pool on pop.
 */
object WarmYoutubePlayer {
    private const val TAG = "KidTubePlayer"
    private const val IDLE_PAUSE_MS = 4_000L
    private val ID_PATTERN = Regex("[A-Za-z0-9_-]{11}")

    private var _view: YouTubePlayerView? = null
    var player: YouTubePlayer? = null
        private set
    val readyState: MutableState<Boolean> = mutableStateOf(false)
    var currentVideoId: String? = null
        private set
    var lastState: PlayerConstants.PlayerState? = null
        private set
    var lastError: PlayerConstants.PlayerError? = null
        private set

    private var host: FrameLayout? = null
    private var observedLifecycle: Lifecycle? = null
    private var acquired = false
    private var pendingId: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var idlePause: Runnable? = null

    private fun iframeOptions(): IFramePlayerOptions =
        IFramePlayerOptions.Builder()
            .controls(1)
            .rel(0)
            .ivLoadPolicy(3)
            .ccLoadPolicy(0)
            .modestBranding(1)
            .origin("https://www.youtube.com")
            .build()

    private val poolListener = object : AbstractYouTubePlayerListener() {
        override fun onReady(p: YouTubePlayer) {
            player = p
            readyState.value = true
            val pending = pendingId
            pendingId = null
            if (pending != null) {
                // Tile was pressed before the player finished booting — load now.
                currentVideoId = pending
                lastState = null
                try {
                    p.mute()
                    p.loadVideo(pending, 0f)
                } catch (_: Exception) {}
                scheduleIdlePause()
            }
            Log.d(TAG, "warm player ready pending=$pending")
        }

        override fun onStateChange(p: YouTubePlayer, state: PlayerConstants.PlayerState) {
            lastState = state
        }

        override fun onError(p: YouTubePlayer, error: PlayerConstants.PlayerError) {
            Log.w(TAG, "warm player error=$error id=$currentVideoId")
            lastError = error
        }
    }

    /** Boot the shared player. Called once from MainActivity.onCreate. */
    fun install(activity: Activity) {
        val old = _view
        if (old != null) {
            val oldActivity = old.context.unwrapActivity()
            if (oldActivity != null && oldActivity.isDestroyed) {
                // Activity recreated (rotation/theme): the library already
                // released the old WebView on ON_DESTROY — drop it, start fresh.
                try { old.release() } catch (_: Exception) {}
                _view = null
                player = null
                readyState.value = false
                currentVideoId = null
                lastState = null
                lastError = null
                pendingId = null
                observedLifecycle = null
                acquired = false
            }
        }
        ensureHost(activity)
        val v = ensureView(activity)
        val lc = (activity as? LifecycleOwner)?.lifecycle
        if (lc != null && observedLifecycle !== lc) {
            observedLifecycle = lc
            try { lc.addObserver(v) } catch (_: Exception) {}
        }
        if (!acquired) {
            val parent = v.parent as? ViewGroup
            if (parent !== host) {
                parent?.removeView(v)
                host?.addView(v)
            }
        }
    }

    /**
     * Start fetching [videoId] NOW (muted, invisible). Safe to call on every
     * touch-down — it replaces any previous preload. Ignored while a screen
     * holds the view.
     */
    fun preload(videoId: String) {
        val id = videoId.trim()
        if (!id.matches(ID_PATTERN)) return
        if (acquired) return
        lastError = null
        pendingId = id
        val p = player ?: return // applied by poolListener.onReady
        pendingId = null
        currentVideoId = id
        lastState = null
        try {
            p.mute()
            p.loadVideo(id, 0f)
        } catch (_: Exception) {
            Log.w(TAG, "preload loadVideo failed id=$id")
        }
        scheduleIdlePause()
    }

    fun preloadItem(item: MediaItem) {
        if (item is MediaItem.YoutubeLink) preload(item.platformVideoId)
    }

    /** Abort an orphaned preload (touch turned into a scroll). */
    fun cancelPreload() {
        if (acquired) return
        cancelIdlePause()
        pendingId = null
        try { player?.pause() } catch (_: Exception) {}
    }

    /** Take the view out of the 1x1 host for display inside a screen. */
    fun acquire(context: Context): YouTubePlayerView {
        cancelIdlePause()
        val v = ensureView(context)
        val lc = (context.unwrapActivity() as? LifecycleOwner)?.lifecycle
        if (lc != null && observedLifecycle !== lc) {
            observedLifecycle = lc
            try { lc.addObserver(v) } catch (_: Exception) {}
        }
        (v.parent as? ViewGroup)?.removeView(v)
        acquired = true
        return v
    }

    /** Screen is done: stop audio, park the view back in the invisible host. */
    fun release(v: YouTubePlayerView) {
        if (v !== _view) return
        acquired = false
        cancelIdlePause()
        lastError = null
        try { player?.pause() } catch (_: Exception) {}
        try { player?.mute() } catch (_: Exception) {}
        (v.parent as? ViewGroup)?.removeView(v)
        host?.let { h -> if (v.parent == null) h.addView(v) }
    }

    fun consumeLastError(): PlayerConstants.PlayerError? {
        val e = lastError
        lastError = null
        return e
    }

    private fun ensureView(context: Context): YouTubePlayerView {
        _view?.let { return it }
        val v = YouTubePlayerView(context).apply {
            enableAutomaticInitialization = false
            initialize(poolListener, iframeOptions())
        }
        _view = v
        return v
    }

    /** 1x1 container on the activity decor — keeps Chromium window-visible. */
    private fun ensureHost(activity: Activity) {
        val decor = activity.window?.decorView as? ViewGroup ?: return
        val existing = host
        val existingParent = existing?.parent as? ViewGroup
        if (existing != null && existingParent === decor) return
        if (existing != null) existingParent?.removeView(existing)
        val h = FrameLayout(activity)
        host = h
        decor.addView(h, FrameLayout.LayoutParams(1, 1))
    }

    /** Pause an orphaned preload if the child never opens the screen. */
    private fun scheduleIdlePause() {
        cancelIdlePause()
        val r = Runnable {
            idlePause = null
            if (!acquired) {
                try { player?.pause() } catch (_: Exception) {}
            }
        }
        idlePause = r
        mainHandler.postDelayed(r, IDLE_PAUSE_MS)
    }

    private fun cancelIdlePause() {
        idlePause?.let { mainHandler.removeCallbacks(it) }
        idlePause = null
    }

    private fun Context.unwrapActivity(): Activity? {
        var c: Context? = this
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }
}
