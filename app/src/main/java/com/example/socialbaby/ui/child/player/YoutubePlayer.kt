package com.example.socialbaby.ui.child.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.util.Log
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@Composable
fun YoutubePlayer(
    videoId: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    mute: Boolean = false,
    loop: Boolean = false,
    externalUrl: String? = null,
    onReady: (() -> Unit)? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var loading by remember { mutableStateOf(true) }
    var youTubePlayer by remember { mutableStateOf<YouTubePlayer?>(null) }
    // Stripped full-page mode: for videos whose owner disabled embedding (error
    // 150/101 — your recurring "153"). No embed can play those, so we load the
    // original page with all YouTube chrome hidden and every exit blocked.
    var useStrippedPage by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    // YouTube video IDs are always exactly 11 chars. Old saved items can hold a
    // hashcode or truncated ID — those can never play; say so clearly instead
    // of showing a cryptic player error.
    val cleanId = videoId.trim()
    val isValidId = cleanId.matches(Regex("[A-Za-z0-9_-]{11}"))
    // App-side mute state — single source of truth so YouTube's own mute button isn't confusing.
    // Start muted=false as requested, but YouTube forces muted autoplay until user gesture,
    // so we show our own big unmute overlay on first frame.
    var appMuted by remember(videoId, mute) { mutableStateOf(mute) }
    var hasUserUnmuted by remember(videoId) { mutableStateOf(false) }
    // Set when PLAYING arrives. Some YouTube failures (152 in-frame screens)
    // never fire onError — without this the player would sit on the error
    // screen forever with no fallback. See the watchdog below.
    var reachedPlaying by remember { mutableStateOf(false) }
    // Second player path: our fixed WebView embed (loadDataWithBaseURL with a
    // real youtube.com base + referrerpolicy — the documented 152/153 cure).
    // Used when the library player stalls silently (in-frame 152 screens fire
    // no onError, so without this the video would sit broken forever).
    var fallbackToEmbed by remember { mutableStateOf(false) }

    fun applyMute(p: YouTubePlayer, wantMuted: Boolean) {
        try {
            if (wantMuted) p.mute() else { p.unMute(); p.setVolume(100) }
        } catch (_: Exception) {}
    }

    // Handle mute without reloading
    LaunchedEffect(mute) {
        appMuted = mute
        youTubePlayer?.let { applyMute(it, mute) }
    }

    LaunchedEffect(autoPlay) {
        youTubePlayer?.let {
            if (autoPlay) {
                it.play()
                applyMute(it, appMuted)
            } else {
                it.pause()
            }
        }
    }

    // Keep unmuted persistent — fixes "after unmute it mute after some second" (YouTube re-mutes on loop)
    LaunchedEffect(autoPlay, appMuted) {
        while (autoPlay && !appMuted) {
            kotlinx.coroutines.delay(1600)
            youTubePlayer?.let { p ->
                try { p.unMute(); p.setVolume(100) } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(videoId) {
        reachedPlaying = false
        fallbackToEmbed = false
        // When videoId changes and player is ready, load new video
        youTubePlayer?.let {
            loading = true
            useStrippedPage = false
            errorText = null
            hasUserUnmuted = false
            if (autoPlay) it.loadVideo(videoId, 0f) else it.cueVideo(videoId, 0f)
            applyMute(it, appMuted)
        }
    }

    // Watchdog: if autoplay was requested but PLAYING never arrives, switch to
    // the fixed embed. Slow networks get 15s. Off-screen Shorts pages
    // (autoPlay=false) are excluded.
    LaunchedEffect(videoId, autoPlay) {
        if (!autoPlay) return@LaunchedEffect
        kotlinx.coroutines.delay(15000)
        if (!reachedPlaying && errorText == null && !useStrippedPage && !fallbackToEmbed) {
            Log.w("KidTubePlayer", "YT watchdog: no PLAYING in 15s, switching to embed id=$videoId")
            fallbackToEmbed = true
        }
    }

    // Bad ID (old hashcode items, truncated paste) — explain, don't play.
    if (!isValidId) {
        Box(modifier.background(Color.Black).padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "This link couldn't be read",
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Ask a parent to delete it and paste the YouTube link again " +
                        "(Share → Copy link).",
                    color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    if (useStrippedPage && externalUrl != null) {
        // Owner disabled embedding: play the original page with YouTube chrome
        // stripped and every exit blocked. Child stays in the app.
        EmbeddedLinkPlayer(
            videoId = videoId,
            platform = "YOUTUBE_FULL",
            externalUrl = externalUrl,
            modifier = modifier,
            autoPlay = autoPlay,
            isMuted = mute,
            isCurrentPage = true
        )
        return
    }

    if (fallbackToEmbed) {
        // Library player stalled with no callback (silent in-frame 152): play
        // the same video in our fixed WebView embed (proper Referer). If that
        // embed's own network load fails, its Retry overlay takes over.
        EmbeddedLinkPlayer(
            videoId = videoId,
            platform = "YOUTUBE",
            externalUrl = externalUrl,
            modifier = modifier,
            autoPlay = autoPlay,
            isMuted = mute,
            isCurrentPage = true
        )
        return
    }

    Box(modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                YouTubePlayerView(ctx).apply {
                    enableAutomaticInitialization = false
                    lifecycleOwner.lifecycle.addObserver(this)
                    // origin MUST be a trusted domain. Without it the library falls
                    // back to https://<packageName>, which YouTube rejects with
                    // "Error 152" (missing/invalid Referer). Library docs recommend
                    // https://www.youtube.com — it is also the WebView base URL.
                    val iframeOptions = IFramePlayerOptions.Builder()
                        .controls(1)
                        .rel(0)
                        .ivLoadPolicy(3)
                        .ccLoadPolicy(0)
                        .modestBranding(1)
                        .origin("https://www.youtube.com")
                        .build()
                    initialize(object : AbstractYouTubePlayerListener() {
                        override fun onReady(player: YouTubePlayer) {
                            youTubePlayer = player
                            loading = false
                            Log.d("KidTubePlayer", "YT ready id=$videoId autoplay=$autoPlay")
                            onReady?.invoke()
                            // Set mute BEFORE load for reliable autoplay (fixes tap to unmute confusion).
                            // YouTube forces muted autoplay on mobile — we request unmuted, then
                            // re-assert after load; our overlay button is the single UX for mute.
                            if (appMuted) player.mute() else player.unMute()
                            if (autoPlay) player.loadVideo(videoId, 0f) else player.cueVideo(videoId, 0f)
                            // Ensure unmuted after load (YouTube may reset mute on load)
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(400)
                                if (appMuted) player.mute() else { player.unMute(); player.setVolume(100) }
                            }
                        }
                        override fun onStateChange(player: YouTubePlayer, state: PlayerConstants.PlayerState) {
                            Log.d("KidTubePlayer", "YT state=$state id=$videoId")
                            if (state == PlayerConstants.PlayerState.PLAYING) {
                                loading = false
                                reachedPlaying = true
                            }
                            if (state == PlayerConstants.PlayerState.ENDED && loop) {
                                player.seekTo(0f)
                                player.play()
                            }
                        }
                        override fun onError(player: YouTubePlayer, error: PlayerConstants.PlayerError) {
                            loading = false
                            Log.w("KidTubePlayer", "YT error=$error id=$videoId")
                            when (error) {
                                // Deleted / private video, or bad request: nothing can play it.
                                PlayerConstants.PlayerError.VIDEO_NOT_FOUND,
                                PlayerConstants.PlayerError.INVALID_PARAMETER_IN_REQUEST -> {
                                    errorText = "Video not found or private — ask a parent to pick another video."
                                }
                                // Owner disabled embedding, region/age block, or any other
                                // playback refusal (your recurring "152" screen): NO embed
                                // player can play these — not the IFrame library, not the
                                // nocookie embed (it just renders the same in-frame error
                                // with no recovery). Use the stripped in-app page instead:
                                // same video, YouTube chrome hidden, exits blocked.
                                else -> {
                                    if (externalUrl != null) useStrippedPage = true
                                    else errorText = "YouTube couldn't start this video — check internet and retry."
                                }
                            }
                        }
                    }, iframeOptions)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Block taps on YouTube title/channel (top strip) from leaving the app.
        // Transparent click-consumer keeps playback inside KidTube.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .width(400.dp)
                .height(60.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* consume: stay in app */ }
        )
        // Bottom-right 110x44dp: blocks YouTube watermark logo taps
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 48.dp)
                .width(110.dp)
                .height(44.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* consume: stay in app */ }
        )

        // App-side unmute overlay — single clear UX instead of YouTube's small mute button.
        // YouTube forces muted autoplay on mobile; first user tap unmutes with sound.
        if (appMuted && !loading && errorText == null && !useStrippedPage) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.62f))
                    .clickable {
                        hasUserUnmuted = true
                        appMuted = false
                        try {
                            youTubePlayer?.unMute()
                            youTubePlayer?.setVolume(100)
                            youTubePlayer?.play()
                        } catch (_: Exception) {}
                    }
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VolumeOff,
                        contentDescription = "Unmute",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Tap to unmute",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            }
        }

        // Small mute toggle once unmuted (bottom-start, kid-friendly 48dp)
        if (!appMuted && !loading && errorText == null && !useStrippedPage) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable {
                        appMuted = true
                        try { youTubePlayer?.mute() } catch (_: Exception) {}
                    },
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        if (loading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        errorText?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("YouTube can't play", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(msg, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(videoId, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
