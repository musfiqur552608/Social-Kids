package com.example.socialbaby.ui.parent.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.socialbaby.domain.model.MediaItem
import com.example.socialbaby.domain.model.Shelf
import com.example.socialbaby.domain.model.typeName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    viewModel: ParentDashboardViewModel = hiltViewModel(),
    onBackToChild: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToShelf: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onPreviewItem: (Long) -> Unit
) {
    val media by viewModel.allMedia.collectAsState()
    val shelves by viewModel.shelves.collectAsState()
    val cs = MaterialTheme.colorScheme
    var editingItem by remember { mutableStateOf<MediaItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parent Dashboard", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBackToChild) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.primary, titleContentColor = cs.onPrimary)
            )
        },
        containerColor = cs.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = cs.primary,
                contentColor = cs.onPrimary,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add", fontWeight = FontWeight.Bold) }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
            // Stats with gradient feel
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(title = "${media.size}", subtitle = "Items curated", modifier = Modifier.weight(1f), container = cs.primaryContainer, content = cs.onPrimaryContainer, icon = Icons.Default.VideoLibrary)
                StatCard(title = "${media.count { it.isFavorite }}", subtitle = "Favorites", modifier = Modifier.weight(1f), container = cs.secondaryContainer, content = cs.onSecondaryContainer, icon = Icons.Default.Favorite)
                StatCard(title = "${media.count { it is MediaItem.LocalVideo || it is MediaItem.LocalImage || it is MediaItem.OnlineVideo || it is MediaItem.OnlineImage }}", subtitle = "Local & Online", modifier = Modifier.weight(1f), container = cs.tertiaryContainer, content = cs.onTertiaryContainer, icon = Icons.Default.CloudDone)
            }
            Spacer(Modifier.height(16.dp))
            // Actions - more playful, easier
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Quick Actions", fontWeight = FontWeight.Bold, color = cs.onSurface, fontSize = 13.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionButton("Add Content", Icons.Default.AddCircle, cs.primary, cs.onPrimary, Modifier.weight(1f), onNavigateToAdd)
                        ActionButton("Record", Icons.Default.Videocam, cs.secondary, cs.onSecondary, Modifier.weight(1f), onNavigateToAdd)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionButton("Shelves", Icons.Default.FolderSpecial, cs.secondaryContainer, cs.onSecondaryContainer, Modifier.weight(1f), onNavigateToShelf)
                        ActionButton("Settings", Icons.Default.Tune, cs.surfaceVariant, cs.onSurfaceVariant, Modifier.weight(1f), onNavigateToSettings)
                        ActionButton("History", Icons.Default.History, cs.tertiaryContainer, cs.onTertiaryContainer, Modifier.weight(1f), onNavigateToHistory)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("All Content", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = cs.onBackground)
                    Text("Tap ✏️ to edit • ▶ to preview", fontSize = 13.sp, color = cs.onSurfaceVariant)
                }
                if (media.isNotEmpty()) {
                    AssistChip(onClick = {}, label = { Text("${media.size} items") }, leadingIcon = { Icon(Icons.Default.GridView, null, modifier = Modifier.size(16.dp)) })
                }
            }
            Spacer(Modifier.height(8.dp))

            if (media.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)).background(cs.surfaceContainer), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.VideoLibrary, null, modifier = Modifier.size(56.dp), tint = cs.outline)
                        Spacer(Modifier.height(8.dp))
                        Text("No content yet", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp), color = cs.onSurface, fontSize = 18.sp)
                        Text("Add videos, photos or links for your child.\nTry Record for instant capture!", color = cs.onSurfaceVariant, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onNavigateToAdd, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary)) {
                            Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add First Item")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(media.size) { idx ->
                        val item = media[idx]
                        ParentMediaCard(
                            item = item,
                            onPreview = { onPreviewItem(item.id) },
                            onDelete = { viewModel.deleteItem(item) },
                            onToggleFav = { viewModel.toggleFavorite(item.id) },
                            onEdit = { editingItem = item }
                        )
                    }
                }
            }
        }
    }

    // Edit dialog
    editingItem?.let { item ->
        EditMediaDialog(
            item = item,
            shelves = shelves,
            onDismiss = { editingItem = null },
            onSave = { updatedTitle, newShelfId ->
                val updated = item.copyWith(title = updatedTitle, shelfId = newShelfId)
                viewModel.updateItem(updated)
                editingItem = null
            }
        )
    }
}

private fun MediaItem.copyWith(title: String, shelfId: Long?): MediaItem = when (this) {
    is MediaItem.LocalVideo -> copy(title = title, shelfId = shelfId)
    is MediaItem.LocalImage -> copy(title = title, shelfId = shelfId)
    is MediaItem.YoutubeLink -> copy(title = title, shelfId = shelfId)
    is MediaItem.TikTokLink -> copy(title = title, shelfId = shelfId)
    is MediaItem.FacebookLink -> copy(title = title, shelfId = shelfId)
    is MediaItem.OnlineVideo -> copy(title = title, shelfId = shelfId)
    is MediaItem.OnlineImage -> copy(title = title, shelfId = shelfId)
    is MediaItem.GenericLink -> copy(title = title, shelfId = shelfId)
}

@Composable
private fun StatCard(title: String, subtitle: String, modifier: Modifier, container: Color, content: Color, icon: ImageVector) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = container, contentColor = content), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, null, modifier = Modifier.size(22.dp), tint = content)
            Spacer(Modifier.height(6.dp))
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Text(subtitle, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = content.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun ActionButton(title: String, icon: ImageVector, container: Color, content: Color, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = container, contentColor = content)
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun ParentMediaCard(item: MediaItem, onPreview: () -> Unit, onDelete: () -> Unit, onToggleFav: () -> Unit, onEdit: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val previewModel: Any? = when (item) {
                    is MediaItem.LocalImage -> item.sourceUri
                    is MediaItem.OnlineImage -> item.externalUrl
                    else -> item.thumbnailPath
                }
                if (previewModel != null) {
                    AsyncImage(model = previewModel, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.Movie, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(36.dp))
                }
                if (item !is MediaItem.LocalImage && item !is MediaItem.OnlineImage) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                }
                Surface(modifier = Modifier.align(Alignment.TopStart).padding(6.dp), shape = RoundedCornerShape(8.dp), color = cs.primary) {
                    Text(
                        when (item) {
                            is MediaItem.LocalVideo -> "LOCAL"
                            is MediaItem.LocalImage -> "PHOTO"
                            is MediaItem.OnlineVideo -> "ONLINE"
                            is MediaItem.OnlineImage -> "IMAGE"
                            is MediaItem.GenericLink -> "WEB"
                            is MediaItem.YoutubeLink -> "YT"
                            is MediaItem.TikTokLink -> "TK"
                            is MediaItem.FacebookLink -> "FB"
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 9.sp, fontWeight = FontWeight.Bold, color = cs.onPrimary
                    )
                }
                // Favorite star
                if (item.isFavorite) {
                    Icon(Icons.Default.Favorite, null, tint = cs.secondary, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, minLines = 2, color = cs.onSurface)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row {
                    IconButton(onClick = onToggleFav, modifier = Modifier.size(30.dp)) {
                        Icon(if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (item.isFavorite) cs.secondary else cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, null, tint = cs.primary, modifier = Modifier.size(18.dp))
                    }
                }
                Row {
                    IconButton(onClick = onPreview, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp), tint = cs.onSurface) }
                    IconButton(onClick = { showDelete = true }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.DeleteOutline, null, tint = cs.error, modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Remove item?") },
            text = { Text("Delete \"${item.title}\" from Social Kids? This only removes it from the app, not your device.") },
            confirmButton = { TextButton(onClick = { onDelete(); showDelete = false }) { Text("Remove", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMediaDialog(item: MediaItem, shelves: List<Shelf>, onDismiss: () -> Unit, onSave: (String, Long?) -> Unit) {
    var title by remember(item) { mutableStateOf(item.title) }
    var selectedShelf by remember(item) { mutableStateOf(item.shelfId) }
    var expanded by remember { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Item", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Preview
                Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(12.dp)).background(cs.surfaceVariant), contentAlignment = Alignment.Center) {
                    val preview: Any? = when (item) {
                        is MediaItem.LocalImage -> item.sourceUri
                        is MediaItem.OnlineImage -> item.externalUrl
                        else -> item.thumbnailPath
                    }
                    if (preview != null) {
                        AsyncImage(model = preview, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Image, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(32.dp))
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Title, null) },
                    singleLine = true
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = shelves.find { it.id == selectedShelf }?.name ?: "No shelf (All)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Shelf") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Folder, null) }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("No shelf") }, onClick = { selectedShelf = null; expanded = false })
                        shelves.forEach { shelf ->
                            DropdownMenuItem(text = { Text(shelf.name) }, onClick = { selectedShelf = shelf.id; expanded = false })
                        }
                    }
                }
                Text("ID: ${item.id} • ${item.typeName()}", fontSize = 11.sp, color = cs.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank()) onSave(title.trim(), selectedShelf) }, shape = RoundedCornerShape(12.dp)) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = cs.surfaceContainer
    )
}
