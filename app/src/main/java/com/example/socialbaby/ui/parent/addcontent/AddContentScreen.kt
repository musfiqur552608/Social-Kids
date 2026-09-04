package com.example.socialbaby.ui.parent.addcontent

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContentScreen(
    viewModel: AddContentViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val link by viewModel.link.collectAsState()
    val shelves by viewModel.shelfList.collectAsState()
    val selected by viewModel.selectedShelf.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()

    // FIX: Use OpenDocument for persistable URI permission (GetContent does NOT grant persistable, causes "configuration error" after restart)
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.addLocalUri(uri, "video/*")
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.addLocalUri(uri, "image/*")
    }
    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            viewModel.addLocalUri(uri, null)
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    // --- Easier record options: capture video/photo directly ---
    val captureVideoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val uri = res.data?.data
            if (uri != null) viewModel.addLocalUri(uri, "video/*")
        }
    }
    val capturePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val uri = res.data?.data
            if (uri != null) {
                viewModel.addLocalUri(uri, "image/*")
            } else {
                val bmp = res.data?.extras?.get("data") as? android.graphics.Bitmap
                if (bmp != null) {
                    try {
                        val file = java.io.File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                        java.io.FileOutputStream(file).use { out ->
                            bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        val fileUri = Uri.fromFile(file)
                        viewModel.addLocalUri(fileUri, "image/*")
                    } catch (e: Exception) { }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Content", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.primary, titleContentColor = cs.onPrimary)
            )
        },
        containerColor = cs.background
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhoneAndroid, null, tint = cs.primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("From Device", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = cs.onSurface)
                        }
                        Text("Photos & videos stored on this device play 100% offline. Thumbnail preview auto-generated.", fontSize = 13.sp, color = cs.onSurfaceVariant)
                        Spacer(Modifier.height(14.dp))
                        // Easier record + pick grid
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                                        captureVideoLauncher.launch(intent)
                                    },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Videocam, null, modifier = Modifier.size(22.dp))
                                        Text("Record Video", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Button(
                                    onClick = {
                                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                        capturePhotoLauncher.launch(intent)
                                    },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = cs.secondary, contentColor = cs.onSecondary)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(22.dp))
                                        Text("Take Photo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                FilledTonalButton(
                                    onClick = { pickVideo.launch(arrayOf("video/*")) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) { Icon(Icons.Default.VideoLibrary, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Pick Video") }
                                FilledTonalButton(
                                    onClick = { pickImage.launch(arrayOf("image/*")) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) { Icon(Icons.Default.Photo, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Pick Image") }
                            }
                            OutlinedButton(
                                onClick = { openDoc.launch(arrayOf("video/*", "image/*")) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) { Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Browse files (any)") }
                            Text("💡 Tip: Record is easiest — one tap to capture, auto-saved with preview", fontSize = 11.sp, color = cs.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("From Link — Plays Inside Social Kids", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = cs.onSurface)
                        Text("Paste any direct video (mp4, m3u8, webm) or image (jpg, png) link — it will play 100% in-app via our player (no YouTube/TikTok redirect). YouTube/TikTok/Facebook links still work via embed.", fontSize = 13.sp, color = cs.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = link,
                            onValueChange = { viewModel.onLinkChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("https://example.com/video.mp4  or  https://picsum.photos/800/600  or  https://youtube.com/watch?v=...") },
                            shape = RoundedCornerShape(14.dp),
                            leadingIcon = { Icon(Icons.Default.Link, null) },
                            supportingText = {
                                val parsed = remember(link) { try { com.example.socialbaby.util.UrlParser.parse(link) } catch (_: Exception) { null } }
                                if (link.isNotBlank()) {
                                    if (parsed != null) Text("✓ Detected: ${parsed.platform} • id: ${parsed.videoId.take(18)}", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                                    else Text("✗ Not recognized — try full https:// link", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(onClick = { viewModel.onLinkChange("https://storage.googleapis.com/exoplayer-test-media-1/mp4/android-screens-10s.mp4") }, label = { Text("Try sample MP4") })
                            AssistChip(onClick = { viewModel.onLinkChange("https://picsum.photos/800/600") }, label = { Text("Try sample image") })
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.addLink() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary),
                            enabled = !isLoading && link.isNotBlank()
                        ) {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = cs.onPrimary)
                            else { Icon(Icons.Default.AddLink, null); Spacer(Modifier.width(8.dp)); Text("Add Link (in-app playback)", fontWeight = FontWeight.Bold) }
                        }
                        Text("Preview auto-fetched • thumbnail cached once • then 100% in-app", fontSize = 11.sp, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Organize", fontWeight = FontWeight.Bold, color = cs.onSurface)
                        Text("Choose a shelf for the next item you add.", fontSize = 13.sp, color = cs.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                            OutlinedTextField(
                                value = shelves.find { it.id == selected }?.name ?: "No shelf (default)",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(text = { Text("No shelf") }, onClick = { viewModel.onShelfSelected(null); expanded = false })
                                shelves.forEach { shelf ->
                                    DropdownMenuItem(text = { Text(shelf.name) }, onClick = { viewModel.onShelfSelected(shelf.id); expanded = false })
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cs.primaryContainer)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, null, tint = cs.onPrimaryContainer)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Legal & Safe", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = cs.onPrimaryContainer)
                            Text("Direct links play with native ExoPlayer/Coil — no download of YouTube files. YouTube embed only uses IFrame Player API. All cache is local.", fontSize = 12.sp, color = cs.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }

        message?.let { msg ->
            LaunchedEffect(msg) { kotlinx.coroutines.delay(2500); viewModel.clearMessage() }
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
                val isErr = msg.contains("Failed") || msg.contains("Unsupported")
                Surface(shape = RoundedCornerShape(12.dp), color = if (isErr) cs.errorContainer else cs.secondaryContainer, shadowElevation = 4.dp) {
                    Text(msg, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = if (isErr) cs.onErrorContainer else cs.onSecondaryContainer, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
