package com.example.socialbaby.ui.child.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.socialbaby.domain.model.MediaItem
import com.example.socialbaby.ui.theme.KidCoral
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    mediaId: Long,
    viewModel: PlayerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }
    LaunchedEffect(mediaId) { viewModel.load(mediaId) }
    val item by viewModel.item.collectAsState()
    val cs = MaterialTheme.colorScheme

    var exitHoldProgress by remember { mutableStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }

    LaunchedEffect(isHolding) {
        while (isHolding) {
            delay(16)
            exitHoldProgress = (exitHoldProgress + 0.02f).coerceAtMost(1f)
            if (exitHoldProgress >= 1f) {
                onNavigateBack()
                break
            }
        }
        if (!isHolding) exitHoldProgress = 0f
    }

    if (item == null) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = cs.primary)
        }
        return
    }

    val media = item!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(media.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, color = cs.onSurface)
                        Text(
                            when (media) {
                                is MediaItem.LocalVideo -> "Local video • in-app"
                                is MediaItem.LocalImage -> "Photo • in-app"
                                is MediaItem.OnlineVideo -> "Online video • in-app"
                                is MediaItem.OnlineImage -> "Online image • in-app"
                                is MediaItem.GenericLink -> "Web • in-app"
                                is MediaItem.YoutubeLink -> "YouTube (embed)"
                                is MediaItem.TikTokLink -> "TikTok (embed)"
                                is MediaItem.FacebookLink -> "Facebook (embed)"
                            },
                            fontSize = 11.sp, color = cs.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isHolding) cs.secondary else cs.surfaceVariant)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isHolding = true
                                        tryAwaitRelease()
                                        isHolding = false
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (exitHoldProgress > 0) {
                            CircularProgressIndicator(progress = { exitHoldProgress }, modifier = Modifier.size(48.dp), strokeWidth = 4.dp, color = cs.secondary, trackColor = Color.Transparent)
                        }
                        Icon(Icons.Default.ArrowBack, null, tint = cs.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(if (media.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (media.isFavorite) cs.secondary else cs.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.surface, titleContentColor = cs.onSurface)
            )
        },
        containerColor = Color.Black
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().background(Color.Black)) {
            when (media) {
                is MediaItem.LocalVideo -> {
                    LocalVideoPlayer(
                        uri = media.sourceUri,
                        startPositionMs = media.lastWatchedPositionMs,
                        onPositionChanged = { pos, delta -> viewModel.saveProgress(pos, delta) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is MediaItem.OnlineVideo -> {
                    // Direct in-app playback via ExoPlayer (no embed)
                    LocalVideoPlayer(
                        uri = media.externalUrl,
                        startPositionMs = media.lastWatchedPositionMs,
                        onPositionChanged = { pos, delta -> viewModel.saveProgress(pos, delta) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is MediaItem.LocalImage -> {
                    AsyncImage(
                        model = media.sourceUri,
                        contentDescription = media.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                is MediaItem.OnlineImage -> {
                    AsyncImage(
                        model = media.externalUrl,
                        contentDescription = media.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                is MediaItem.YoutubeLink -> {
                    // Dedicated YouTube player — far more reliable than WebView iframe_api (fixes embedded any video)
                    YoutubePlayer(videoId = media.platformVideoId, externalUrl = media.externalUrl, modifier = Modifier.fillMaxSize(), autoPlay = true, mute = false, loop = false)
                }
                is MediaItem.TikTokLink -> {
                    EmbeddedLinkPlayer(videoId = media.platformVideoId, platform = "TIKTOK", externalUrl = media.externalUrl, modifier = Modifier.fillMaxSize())
                }
                is MediaItem.FacebookLink -> {
                    EmbeddedLinkPlayer(videoId = media.platformVideoId, platform = "FACEBOOK", externalUrl = media.externalUrl, modifier = Modifier.fillMaxSize())
                }
                is MediaItem.GenericLink -> {
                    // Any platform link (your own site) — loads directly in secure WebView, no external app
                    EmbeddedLinkPlayer(videoId = media.externalUrl, platform = "GENERIC", externalUrl = media.externalUrl, modifier = Modifier.fillMaxSize())
                }
            }

            // Hold-to-exit hint
            if (isHolding) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Text("Keep holding to exit...", color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Bottom hint for images
            if (media is MediaItem.LocalImage || media is MediaItem.OnlineImage) {
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.4f)).padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Pinch to zoom • Hold back button to exit", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}
