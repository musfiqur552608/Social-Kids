package com.example.socialbaby.ui.child.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.example.socialbaby.domain.model.MediaItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToParent: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToShorts: () -> Unit,
    onNavigateToPhotos: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val shelves by viewModel.shelves.collectAsState(initial = emptyList())
    val selectedId by viewModel.selectedShelfId.collectAsState()
    val paging = viewModel.pagedMedia.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cs.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = cs.onPrimary, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("Social Kids", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = cs.onBackground)
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = onNavigateToParent,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = cs.primary, contentColor = cs.onPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Parents", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = cs.surface,
                    titleContentColor = cs.onSurface
                )
            )
        },
        containerColor = cs.background
    ) { padding ->

        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Playful welcome banner - more attractive
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cs.primaryContainer),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(cs.primaryContainer, cs.secondaryContainer.copy(alpha = 0.6f), cs.tertiaryContainer.copy(alpha = 0.5f))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(cs.primary),
                            contentAlignment = Alignment.Center
                        ) { Text("👶", fontSize = 30.sp) }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Welcome to Social Kids!", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = cs.onPrimaryContainer)
                            Text("Your parent-curated world • safe • offline • fun", fontSize = 12.sp, color = cs.onPrimaryContainer.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.Default.PlayCircle, null, tint = cs.primary, modifier = Modifier.size(28.dp))
                    }
                }
            }

            // Quick nav - Shorts / Photos like TikTok style
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BigNavCard(
                    title = "Shorts",
                    subtitle = "Swipe videos",
                    container = cs.secondary,
                    content = cs.onSecondary,
                    icon = Icons.Default.Movie,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToShorts
                )
                BigNavCard(
                    title = "Photos",
                    subtitle = "Swipe pictures",
                    container = cs.tertiary,
                    content = cs.onTertiary,
                    icon = Icons.Default.Photo,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToPhotos
                )
            }

            // Shelf chips - FIXED FILTERING
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedId == null,
                        onClick = { viewModel.selectShelf(null) },
                        label = { Text("All", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cs.primary,
                            selectedLabelColor = cs.onPrimary
                        )
                    )
                }
                items(shelves) { shelf ->
                    FilterChip(
                        selected = selectedId == shelf.id,
                        onClick = { viewModel.selectShelf(shelf.id) },
                        label = { Text(shelf.name, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cs.secondaryContainer,
                            selectedLabelColor = cs.onSecondaryContainer
                        )
                    )
                }
            }

            // Content grid - responsive (adaptive) for all mobile screens: phones 2-col, tablets 3-4, foldables adaptive
            when {
                paging.itemCount > 0 -> {
                    // BoxWithConstraints lets grid adapt to width (phones/tablets/foldables/landscape)
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val minCard = 160.dp
                        val cols = (maxWidth / minCard).toInt().coerceIn(2, 4)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(cols),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            contentPadding = PaddingValues(bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(paging.itemCount) { idx ->
                                paging[idx]?.let { item ->
                                    MediaCard(item = item, onClick = { onNavigateToPlayer(item.id) })
                                }
                            }
                        }
                    }
                }
                paging.loadState.refresh is androidx.paging.LoadState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = cs.primary)
                    }
                }
                selectedId != null -> {
                    // filtered shelf empty
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Icon(Icons.Default.VideoLibrary, null, modifier = Modifier.size(64.dp), tint = cs.outline)
                            Spacer(Modifier.height(12.dp))
                            Text("Empty shelf", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = cs.onBackground)
                            Text(
                                "\"${shelves.find { it.id == selectedId }?.name ?: ""}\" has no items. Tap All or ask parent to add.",
                                color = cs.onSurfaceVariant,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = { viewModel.selectShelf(null) }) { Text("Show All") }
                        }
                    }
                }
                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.VideoLibrary, null, modifier = Modifier.size(64.dp), tint = cs.secondary)
                            Spacer(Modifier.height(12.dp))
                            Text("No videos yet", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = cs.onBackground)
                            Text("Ask a parent to add some!", color = cs.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateToParent,
                                colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary),
                                shape = RoundedCornerShape(20.dp)
                            ) { Text("Go to Parent Mode", fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BigNavCard(
    title: String,
    subtitle: String,
    container: Color,
    content: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(110.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(Modifier.fillMaxSize().padding(14.dp)) {
            Icon(icon, null, modifier = Modifier.size(36.dp).align(Alignment.TopEnd), tint = content.copy(alpha = 0.85f))
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = content)
                Text(subtitle, fontSize = 13.sp, color = content.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun MediaCard(item: MediaItem, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(cs.surfaceVariant)
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Thumbnail — handles local, online & platform links with proper preview
                val tp = item.thumbnailPath
                val thumbModel: Any? = when {
                    !tp.isNullOrBlank() -> tp
                    item is MediaItem.LocalImage -> item.sourceUri
                    item is MediaItem.OnlineImage -> item.externalUrl
                    item is MediaItem.GenericLink -> null // generic web has no image thumb, show icon
                    // OnlineVideo with no thumb → show icon placeholder (don't try to load mp4 as image)
                    else -> null
                }

                if (thumbModel != null) {
                    AsyncImage(
                        model = thumbModel,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        // coil will gracefully fail for video file uris; we keep overlay
                    )
                    // subtle scrim for text contrast
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.08f)))
                } else {
                    Icon(
                        when (item) {
                            is MediaItem.LocalImage -> Icons.Default.Photo
                            is MediaItem.TikTokLink, is MediaItem.YoutubeLink, is MediaItem.FacebookLink -> Icons.Default.PlayCircle
                            else -> Icons.Default.Movie
                        },
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = cs.secondary
                    )
                }
                // Type badge - use primary / secondary tokens
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = cs.primary
                ) {
                    Text(
                        when (item) {
                            is MediaItem.LocalVideo -> "VIDEO"
                            is MediaItem.LocalImage -> "PHOTO"
                            is MediaItem.OnlineVideo -> "VIDEO"
                            is MediaItem.OnlineImage -> "PHOTO"
                            is MediaItem.GenericLink -> "WEB"
                            is MediaItem.YoutubeLink -> "YOUTUBE"
                            is MediaItem.TikTokLink -> "TIKTOK"
                            is MediaItem.FacebookLink -> "FACEBOOK"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = cs.onPrimary
                    )
                }
                // Play overlay for videos/links (not for pure images)
                if (item !is MediaItem.LocalImage && item !is MediaItem.OnlineImage) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.PlayCircle, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                }

                if (item.isFavorite) {
                    Icon(Icons.Default.Favorite, null, tint = cs.secondary, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp))
                }
            }
            Column(Modifier.padding(12.dp).fillMaxWidth()) {
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = cs.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
                Spacer(Modifier.height(4.dp))
                if (item.lastWatchedPositionMs > 0) {
                    LinearProgressIndicator(
                        progress = { 0.3f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = cs.primary,
                        trackColor = cs.outlineVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Resume", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = cs.primary)
                } else {
                    Text(
                        when (item) {
                            is MediaItem.LocalVideo, is MediaItem.YoutubeLink, is MediaItem.TikTokLink, is MediaItem.FacebookLink, is MediaItem.OnlineVideo, is MediaItem.GenericLink -> "Tap to play"
                            is MediaItem.LocalImage, is MediaItem.OnlineImage -> "Tap to view"
                        },
                        fontSize = 12.sp, color = cs.onSurfaceVariant
                    )
                }
            }
        }
    }
}
