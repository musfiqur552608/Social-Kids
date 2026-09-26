package com.freedu.socialbaby.ui.child.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File

/**
 * Instant poster frame shown while a player boots. The thumbnail is already
 * on disk / in Coil's memory cache from the list screen, so this paints
 * immediately — the child sees the video's picture instead of a black screen
 * with a spinner while DNS/TLS/player-init happen underneath.
 *
 * Accepts an absolute file path ("/data/.../thumb_x.jpg") or an http(s) URL.
 */
@Composable
fun PosterImage(posterUrl: String?, modifier: Modifier = Modifier) {
    if (posterUrl.isNullOrBlank()) return
    val model: Any = if (posterUrl.startsWith("/")) File(posterUrl) else posterUrl
    AsyncImage(
        model = model,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}
