package com.example.socialbaby.ui.child.shorts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.example.socialbaby.domain.model.MediaItem
import com.example.socialbaby.ui.child.player.EmbeddedLinkPlayer
import com.example.socialbaby.ui.child.player.LocalVideoPlayer
import com.example.socialbaby.ui.child.player.YoutubePlayer
import com.example.socialbaby.ui.theme.KidCoral
import com.example.socialbaby.ui.theme.KidYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsFeedScreen(
    viewModel: ShortsFeedViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit
) {
    BackHandler { onNavigateBack() }
    val shorts = viewModel.shorts.collectAsLazyPagingItems()
    val pagerState = rememberPagerState(pageCount = { shorts.itemCount.coerceAtLeast(1) })
    // Global unmute for Shorts — fixes "unmute all then next paused, tap to start" (per-page muted caused next to be muted/paused)
    var globalMuted by remember { mutableStateOf(false) } // false = always unmuted as requested

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shorts", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        containerColor = Color.Black
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().background(Color.Black)) {
            if (shorts.itemCount == 0) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No shorts yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Ask parent to add videos", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            } else {
                VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 0) { idx ->
                    val item = shorts[idx]
                    // Only settled page plays and only one player alive (no overlap, fixes previous sound after tap)
                    val isCurrent = pagerState.currentPage == idx
                    if (item != null) {
                        ShortsInlinePage(
                            item = item,
                            isCurrentPage = isCurrent,
                            isMutedGlobal = globalMuted,
                            onToggleMuteGlobal = { globalMuted = !globalMuted },
                            onToggleFavorite = { viewModel.toggleFavorite(item.id) },
                            onWatchProgress = { id, pos, delta -> viewModel.saveWatchProgress(id, pos, delta) },
                            onWatchTime = { id, delta -> viewModel.recordWatchTime(id, delta) },
                            onTap = { onNavigateToPlayer(item.id) }
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(Color.DarkGray))
                    }
                }
                // page indicator
                Column(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(shorts.itemCount.coerceAtMost(8)) { i ->
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(if (pagerState.currentPage == i) 20.dp else 8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (pagerState.currentPage == i) KidYellow else Color.White.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortsInlinePage(item: MediaItem, isCurrentPage: Boolean, isMutedGlobal: Boolean, onToggleMuteGlobal: () -> Unit, onToggleFavorite: () -> Unit, onWatchProgress: (Long, Long, Long) -> Unit, onWatchTime: (Long, Long) -> Unit, onTap: () -> Unit) {
    var liked by remember(item.id) { mutableStateOf(item.isFavorite) }
    val muted = isMutedGlobal // use global so unmute all persists to next video (fixes next paused need tap)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // --- Inline video / image rendering (same page, auto-plays like YouTube Shorts / Reels) ---
        when (item) {
            is MediaItem.LocalVideo -> {
                LocalVideoPlayer(
                    uri = item.sourceUri,
                    startPositionMs = 0,
                    onPositionChanged = { pos, delta -> onWatchProgress(item.id, pos, delta) },
                    modifier = Modifier.fillMaxSize(),
                    autoPlay = true,
                    looping = true,
                    showControls = false,
                    isMuted = isMutedGlobal,
                    isCurrentPage = isCurrentPage
                )
            }
            is MediaItem.OnlineVideo -> {
                LocalVideoPlayer(
                    uri = item.externalUrl,
                    startPositionMs = 0,
                    onPositionChanged = { pos, delta -> onWatchProgress(item.id, pos, delta) },
                    modifier = Modifier.fillMaxSize(),
                    autoPlay = true,
                    looping = true,
                    showControls = false,
                    isMuted = isMutedGlobal,
                    isCurrentPage = isCurrentPage
                )
            }
            is MediaItem.YoutubeLink -> {
                YoutubePlayer(
                    videoId = item.platformVideoId,
                    externalUrl = item.externalUrl,
                    modifier = Modifier.fillMaxSize(),
                    autoPlay = isCurrentPage,
                    mute = if (isCurrentPage) isMutedGlobal else true,
                    loop = true,
                    onPositionChanged = { pos, delta -> onWatchProgress(item.id, pos, delta) },
                    onWatchTime = { delta -> onWatchTime(item.id, delta) }
                )
            }
            is MediaItem.GenericLink -> {
                EmbeddedLinkPlayer(videoId = item.externalUrl, platform = "GENERIC", externalUrl = item.externalUrl, modifier = Modifier.fillMaxSize(), autoPlay = isCurrentPage, isMuted = if (isCurrentPage) isMutedGlobal else true, isCurrentPage = isCurrentPage, onWatchTime = { delta -> onWatchTime(item.id, delta) })
            }
            is MediaItem.LocalImage -> {
                AsyncImage(model = item.sourceUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            is MediaItem.OnlineImage -> {
                AsyncImage(model = item.externalUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }

        // Gradient scrim for text readability (like Shorts/Reels)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f)))))

        // Tap overlay hint — tap anywhere to open full player (but video already playing inline)
        // We don't need center play button for inline autoplay; keep subtle tap area
        // Show only for images where no autoplay
        if (item is MediaItem.LocalImage || item is MediaItem.OnlineImage) {
            // For images, show not playing, but still allow tap
        }

        // Bottom info (title + swipe hint)
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp).padding(end = 64.dp)
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = KidYellow) {
                Text(
                    when (item) {
                        is MediaItem.YoutubeLink -> "YOUTUBE"
                        is MediaItem.LocalVideo -> "VIDEO"
                        is MediaItem.OnlineVideo -> "VIDEO"
                        is MediaItem.GenericLink -> "WEB"
                        is MediaItem.OnlineImage -> "PHOTO"
                        is MediaItem.LocalImage -> "PHOTO"
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2B2600)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 2)
            Text(if (isCurrentPage) "Playing • swipe for next" else "Swipe to play", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        }

        // Right action column (like Reels)
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp, bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { liked = !liked; onToggleFavorite() },
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(
                        if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        tint = if (liked) KidCoral else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text("${if (liked) 1 else 0}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            // Global mute toggle — affects app + embedded, next video auto-plays unmuted (fixes next paused need tap)
            if (item !is MediaItem.LocalImage && item !is MediaItem.OnlineImage) {
                IconButton(
                    onClick = { onToggleMuteGlobal() },
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(if (isMutedGlobal) Icons.Default.VolumeOff else Icons.Default.VolumeUp, null, tint = Color.White)
                }
            }
            // Fullscreen button (optional) - tap to go full player
            // Using same onTap
        }
    }
}

@Composable
private fun ThumbWithScrim(item: MediaItem, thumbnail: String?) {
    if (!thumbnail.isNullOrBlank()) {
        AsyncImage(model = thumbnail, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    } else {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(KidCoral, Color(0xFF1A1A1A)))))
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)))))
}
