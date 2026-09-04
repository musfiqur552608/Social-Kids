package com.example.socialbaby.ui.child.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
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
    var fallbackToWebView by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Handle mute without reloading
    LaunchedEffect(mute) {
        youTubePlayer?.let { if (mute) it.mute() else it.unMute() }
    }

    LaunchedEffect(videoId) {
        // When videoId changes and player is ready, load new video
        youTubePlayer?.let {
            loading = true
            fallbackToWebView = false
            errorText = null
            if (autoPlay) it.loadVideo(videoId, 0f) else it.cueVideo(videoId, 0f)
            if (mute) it.mute() else it.unMute()
        }
    }

    if (fallbackToWebView && externalUrl != null) {
        // Fallback: embedding disabled (150/101/153) — load original YouTube page in WebView so video still visible
        EmbeddedLinkPlayer(videoId = videoId, platform = "GENERIC", externalUrl = externalUrl, modifier = modifier)
        return
    }

    Box(modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                YouTubePlayerView(ctx).apply {
                    enableAutomaticInitialization = false
                    lifecycleOwner.lifecycle.addObserver(this)
                    initialize(object : AbstractYouTubePlayerListener() {
                        override fun onReady(player: YouTubePlayer) {
                            youTubePlayer = player
                            loading = false
                            onReady?.invoke()
                            if (mute) player.mute() else player.unMute()
                            if (autoPlay) player.loadVideo(videoId, 0f) else player.cueVideo(videoId, 0f)
                        }
                        override fun onStateChange(player: YouTubePlayer, state: PlayerConstants.PlayerState) {
                            if (state == PlayerConstants.PlayerState.PLAYING) loading = false
                            if (state == PlayerConstants.PlayerState.ENDED && loop) {
                                player.seekTo(0f)
                                player.play()
                            }
                        }
                        override fun onError(player: YouTubePlayer, error: PlayerConstants.PlayerError) {
                            loading = false
                            // Any YouTube error (unknown 152/153/154, 150/101 embedding disabled, HTML5) → fallback to WebView with original watch page so video still visible
                            // This fixes "youtube show unknown" for private/age-restricted/embed-disabled videos
                            if (externalUrl != null) {
                                // For unknown/any error, prefer WebView fallback over showing error
                                val shouldFallback = error == PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER ||
                                        error == PlayerConstants.PlayerError.VIDEO_NOT_FOUND ||
                                        error == PlayerConstants.PlayerError.INVALID_PARAMETER_IN_REQUEST ||
                                        error == PlayerConstants.PlayerError.HTML_5_PLAYER ||
                                        error == PlayerConstants.PlayerError.UNKNOWN ||
                                        error.name.contains("152") || error.name.contains("153") || error.name.contains("154") || error.name.contains("150") || error.name.contains("101")
                                if (shouldFallback) {
                                    fallbackToWebView = true
                                    return
                                }
                            }
                            errorText = when (error) {
                                PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER -> "Embedding disabled (150/101/152-154) — try opening original"
                                PlayerConstants.PlayerError.VIDEO_NOT_FOUND -> "Video not found or private"
                                PlayerConstants.PlayerError.UNKNOWN -> "YouTube unknown error — fallback to web page"
                                else -> "YouTube error: $error"
                            }
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

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
