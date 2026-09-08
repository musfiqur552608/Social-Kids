package com.example.socialbaby.ui.child.player

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun LocalVideoPlayer(
    uri: String,
    startPositionMs: Long = 0,
    onPositionChanged: (positionMs: Long, deltaMs: Long) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    looping: Boolean = false,
    showControls: Boolean = true,
    isMuted: Boolean = false,
    isCurrentPage: Boolean = true
) {
    val context = LocalContext.current
    var lastPos by remember { mutableStateOf(startPositionMs) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var errorCode by remember { mutableStateOf<Int?>(null) }
    var isReady by remember { mutableStateOf(false) }
    var retryKey by remember { mutableIntStateOf(0) }
    var fallbackToWebView by remember { mutableStateOf(false) }
    LaunchedEffect(uri) { fallbackToWebView = false; errorMsg = null; errorCode = null }

    // Robust player creation — handles content://, file:// and https:// with proper HTTP headers for 2004 fix
    val player = remember(uri, retryKey) {
        errorMsg = null
        errorCode = null
        isReady = false
        val httpFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 13; SocialKids) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                repeatMode = if (looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                volume = if (isMuted) 0f else 1f

                // Build MediaItem with explicit mime type inference
                val parsedUri = try { Uri.parse(uri) } catch (_: Exception) { null }
                val builder = MediaItem.Builder().setUri(parsedUri ?: Uri.parse(uri))
                // Hint mime for common cases to avoid container parsing error 153
                val lower = uri.lowercase()
                when {
                    lower.endsWith(".m3u8") -> builder.setMimeType(MimeTypes.APPLICATION_M3U8)
                    lower.endsWith(".mpd") -> builder.setMimeType(MimeTypes.APPLICATION_MPD)
                    lower.endsWith(".mp4") -> builder.setMimeType(MimeTypes.VIDEO_MP4)
                    lower.endsWith(".mkv") -> builder.setMimeType(MimeTypes.VIDEO_MATROSKA)
                    lower.endsWith(".webm") -> builder.setMimeType(MimeTypes.VIDEO_WEBM)
                }
                val mediaItem = builder.build()
                setMediaItem(mediaItem)
                // Prepare first, then seek
                addListener(object : Player.Listener {
                    override fun onPlayerError(err: PlaybackException) {
                        errorCode = err.errorCode
                        val name = try { err.errorCodeName } catch (_: Exception) { "UNKNOWN" }
                        val msg = err.message?.takeIf { it.isNotBlank() } ?: name
                        // Try to extract HTTP response code for 2004
                        val httpCode = try {
                            val cause = err.cause
                            if (cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) cause.responseCode else null
                        } catch (_: Exception) { null }
                        val friendly = when {
                            httpCode != null -> "HTTP $httpCode — link returned ${if (httpCode == 404) "404 Not Found" else if (httpCode == 403) "403 Forbidden" else "error $httpCode"}. The URL may have expired, requires login, or is not a direct video file. Try the sample MP4 or re-add as Generic link."
                            err.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Network 2004: Bad HTTP status (4xx/5xx). URL may be a web page, not a direct mp4. If it's a YouTube link, add it as YouTube type (not Online Video). For your own site, ensure the link ends with .mp4 or use Generic Web."
                            err.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "File not found — may have been moved or permission lost. Please re-add the video."
                            err.errorCode == PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> "Permission lost — please re-add the video from parent mode."
                            err.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
                            err.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "Video file is corrupted or unsupported format. If this was a pasted link, it may be an HTML page, not a direct video — try adding as Generic Link instead of Online Video."
                            err.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                            err.errorCode == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> "Codec not supported on this device."
                            err.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                            err.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Network error — check internet for online videos."
                            err.errorCode == 152 || err.errorCode == 153 || err.errorCode == 154 -> "Configuration ${err.errorCode}: container/codec mismatch or HTML page parsed as video. If you pasted a YouTube/watch page, add it as YouTube/Generic, not Online Video. Try sample MP4."
                            else -> null
                        }
                        errorMsg = buildString {
                            if (friendly != null) append(friendly).append("\n\n")
                            append(msg)
                            append(" [${err.errorCode} $name")
                            if (httpCode != null) append(" http:$httpCode")
                            append("]")
                        }
                    }
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            isReady = true
                            errorMsg = null
                            errorCode = null
                        }
                    }
                })
                prepare()
                seekTo(startPositionMs.coerceAtLeast(0))
                playWhenReady = autoPlay && isCurrentPage
            }
    }

    LaunchedEffect(isMuted) { player.volume = if (isMuted) 0f else 1f }
    LaunchedEffect(isCurrentPage, autoPlay) {
        player.playWhenReady = autoPlay && isCurrentPage
        if (isCurrentPage) {
            if (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING) player.play()
        } else {
            player.pause()
        }
    }
    LaunchedEffect(looping) {
        player.repeatMode = if (looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    // Poll position every 2s to save progress
    LaunchedEffect(player, isCurrentPage) {
        while (true) {
            delay(2000)
            if (isReady && isCurrentPage && player.isPlaying) {
                val pos = try { player.currentPosition } catch (_: Exception) { 0L }
                val delta = (pos - lastPos).coerceAtLeast(0)
                if (delta in 1..9_000) {
                    onPositionChanged(pos, delta)
                    lastPos = pos
                }
            }
        }
    }

    DisposableEffect(player) {
        onDispose {
            try {
                val pos = if (player.isCommandAvailable(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)) player.currentPosition else 0L
                val delta = (pos - lastPos).coerceAtLeast(0)
                if (delta in 1..9_000) onPositionChanged(pos, delta)
            } catch (_: Exception) {}
            player.release()
        }
    }

    // If ExoPlayer fails with 403/404 for a https link that is actually a web page (not direct mp4), fallback to WebView
    if (fallbackToWebView && (uri.startsWith("http://") || uri.startsWith("https://"))) {
        EmbeddedLinkPlayer(videoId = uri, platform = "GENERIC", externalUrl = uri, modifier = modifier)
        return
    }

    Box(modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = showControls
                    controllerShowTimeoutMs = if (showControls) 3000 else 0
                    setShowBuffering(if (showControls) PlayerView.SHOW_BUFFERING_WHEN_PLAYING else PlayerView.SHOW_BUFFERING_NEVER)
                    if (!showControls) hideController()
                    setKeepContentOnPlayerReset(true)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { pv ->
                pv.player = player
                pv.useController = showControls
                // Immediate pause/mute for off-screen Shorts (prevents previous sound overlap on swipe/tap)
                pv.player?.let { p ->
                    p.volume = if (isMuted) 0f else 1f
                    p.playWhenReady = autoPlay && isCurrentPage
                    if (isCurrentPage && autoPlay) {
                        if (!p.isPlaying) p.play()
                    } else {
                        p.pause()
                    }
                }
            }
        )

        errorMsg?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Video can't play",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        msg,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        uri.take(90),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { errorMsg = null; errorCode = null; fallbackToWebView = false; retryKey++ },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) { Text("Retry") }
                    // For HTTP 403/404 (often HTML page pasted as Online Video), offer WebView fallback — fixes "any video" generic links
                    val isHttpError = errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS || msg.contains("403") || msg.contains("404") || msg.contains("HTTP")
                    if (isHttpError && (uri.startsWith("http://") || uri.startsWith("https://"))) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { fallbackToWebView = true; errorMsg = null },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) { Text("Open as Web Page", fontSize = 13.sp) }
                    }
                    if (msg.contains("Permission") || msg.contains("not found") || errorCode == 2002 || errorCode == 2003) {
                        Spacer(Modifier.height(8.dp))
                        Text("Tip: Re-add this video from Parent → Add Content → Pick Video", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // Small loading indicator when not ready and no error
        if (!isReady && errorMsg == null) {
            androidx.compose.material3.CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
