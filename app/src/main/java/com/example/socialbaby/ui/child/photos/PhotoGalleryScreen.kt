package com.example.socialbaby.ui.child.photos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Photo
import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.example.socialbaby.domain.model.MediaItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhotoGalleryScreen(
    viewModel: PhotoGalleryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit
) {
    BackHandler { onNavigateBack() }
    val cs = MaterialTheme.colorScheme
    val photos = viewModel.photos.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photos", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.surface, titleContentColor = cs.onSurface)
            )
        },
        containerColor = cs.background
    ) { pad ->
        if (photos.itemCount == 0) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Photo, null, modifier = Modifier.size(64.dp), tint = cs.outline)
                    Spacer(Modifier.height(8.dp))
                    Text("No photos yet", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = cs.onBackground)
                    Text("Ask parent to add pictures", color = cs.onSurfaceVariant)
                }
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { photos.itemCount })
            HorizontalPager(state = pagerState, modifier = Modifier.padding(pad).fillMaxSize()) { idx ->
                val item = photos[idx]
                Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    if (item != null) {
                        val model: Any? = when (item) {
                            is MediaItem.LocalImage -> item.sourceUri
                            is MediaItem.OnlineImage -> item.externalUrl
                            else -> item.thumbnailPath
                        }
                        AsyncImage(
                            model = model,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Swipe for next • ${idx + 1} / ${photos.itemCount}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
