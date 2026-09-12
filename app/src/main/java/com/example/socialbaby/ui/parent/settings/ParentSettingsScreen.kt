package com.example.socialbaby.ui.parent.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSettingsScreen(
    viewModel: ParentSettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val videoCacheSize by viewModel.videoCacheSize.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stagedCsv by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && stagedCsv != null) viewModel.writeBackupTo(uri, stagedCsv!!)
        stagedCsv = null
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importBackupFrom(uri)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parental Controls", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.primary, titleContentColor = cs.onPrimary)
            )
        },
        containerColor = cs.background
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                SettingsCard(title = "Daily Time Limit") {
                    Text("Max minutes per day child can watch. 0 = unlimited", fontSize = 13.sp, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    val options = listOf(15, 30, 60, 90, 120, 0)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        options.forEach { v ->
                            FilterChip(
                                selected = settings.dailyLimitMinutes == v,
                                onClick = { viewModel.setDailyLimit(v) },
                                label = { Text(if (v == 0) "∞" else "$v m", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = cs.primary, selectedLabelColor = cs.onPrimary)
                            )
                        }
                    }
                    Slider(
                        value = settings.dailyLimitMinutes.toFloat(),
                        onValueChange = { viewModel.setDailyLimit(it.toInt()) },
                        valueRange = 0f..180f,
                        steps = 11
                    )
                    Text("${settings.dailyLimitMinutes} minutes", fontWeight = FontWeight.Bold, color = cs.onSurface)
                }
            }
            item {
                SettingsCard(title = "Bedtime Lock") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Enable bedtime window", color = cs.onSurface)
                        Switch(checked = settings.bedtimeEnabled, onCheckedChange = { viewModel.setBedtimeEnabled(it) })
                    }
                    if (settings.bedtimeEnabled) {
                        Spacer(Modifier.height(8.dp))
                        Text("Window: ${settings.bedtimeStart} → ${settings.bedtimeEnd}", fontSize = 13.sp, color = cs.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(value = settings.bedtimeStart, onValueChange = { viewModel.setBedtimeWindow(it, settings.bedtimeEnd) }, label = { Text("Start") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = settings.bedtimeEnd, onValueChange = { viewModel.setBedtimeWindow(settings.bedtimeStart, it) }, label = { Text("End") }, modifier = Modifier.weight(1f))
                        }
                        Text("During bedtime, Child Mode shows a friendly 'time's up' screen.", fontSize = 11.sp, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            item {
                SettingsCard(title = "Max Session Length") {
                    Text("Auto-pause after this many minutes (gentle reminder).", fontSize = 13.sp, color = cs.onSurfaceVariant)
                    Slider(value = settings.maxSessionMinutes.toFloat(), onValueChange = { viewModel.setMaxSession(it.toInt()) }, valueRange = 5f..60f, steps = 10)
                    Text("${settings.maxSessionMinutes} minutes", fontWeight = FontWeight.Bold, color = cs.onSurface)
                }
            }
            item {
                SettingsCard(title = "Features") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text("Shorts vertical feed", fontWeight = FontWeight.Bold, color = cs.onSurface); Text("Swipe videos vertically", fontSize = 12.sp, color = cs.onSurfaceVariant) }
                        Switch(checked = settings.shortsEnabled, onCheckedChange = { viewModel.setShortsEnabled(it) })
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text("Online Links", fontWeight = FontWeight.Bold, color = cs.onSurface); Text("If off, only local videos work (100% offline)", fontSize = 12.sp, color = cs.onSurfaceVariant) }
                        Switch(checked = settings.onlineLinksEnabled, onCheckedChange = { viewModel.setOnlineEnabled(it) })
                    }
                }
            }
            item {
                SettingsCard(title = "Video Cache") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Disk cache for direct-link videos", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = cs.onSurface)
                            Text("Repeat plays start instantly from storage", fontSize = 12.sp, color = cs.onSurfaceVariant)
                            Text(videoCacheSize, fontSize = 12.sp, color = cs.outline, modifier = Modifier.padding(top = 2.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = { viewModel.clearVideoCache(); viewModel.refreshCacheSize() },
                            colors = ButtonDefaults.textButtonColors(contentColor = cs.error)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
            item {
                SettingsCard(title = "Backup & Restore") {
                    Text(
                        "Catalog of added content as CSV (opens in Excel/Sheets). Shelves are matched by name on import.",
                        fontSize = 13.sp, color = cs.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    val csv = viewModel.buildBackupCsv()
                                    if (csv != null) {
                                        stagedCsv = csv
                                        exportLauncher.launch(viewModel.backupFileName())
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    val csv = viewModel.buildBackupCsv()
                                    if (csv != null) {
                                        val uri = viewModel.prepareShareUri(csv)
                                        if (uri != null) {
                                            val send = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/csv"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(send, "Save to Drive"))
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Drive", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = {
                                importLauncher.launch(arrayOf("text/csv", "text/*", "application/vnd.ms-excel"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Restore, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Import", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (backupStatus != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(backupStatus!!, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cs.primary)
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Safety note", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = cs.onSurface)
                        Text("Direct links use native player; YouTube uses the official embed only while watching. No analytics, no ads, no account. Works 100% offline for local media.", fontSize = 12.sp, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = cs.onSurface)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
