package com.example.socialbaby.ui.parent.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchHistoryScreen(
    viewModel: WatchHistoryViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val logs by viewModel.logs.collectAsState()
    val mediaMap by viewModel.mediaMap.collectAsState()
    val fmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watch History", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = { viewModel.clearAll() }) { Icon(Icons.Default.Delete, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.primary, titleContentColor = cs.onPrimary)
            )
        },
        containerColor = cs.background
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Today's watch time", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = cs.onSurfaceVariant)
                        val total = logs.filter { isToday(it.watchedAt) }.sumOf { it.durationMs }
                        Text(formatDuration(total), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = cs.onSurface)
                    }
                    Icon(Icons.Default.History, null, modifier = Modifier.size(36.dp), tint = cs.outline)
                }
            }
            Spacer(Modifier.height(16.dp))
            if (logs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, null, modifier = Modifier.size(48.dp), tint = cs.outline)
                        Text("No watch history yet", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp), color = cs.onSurface)
                        Text("History appears after the child watches videos", fontSize = 12.sp, color = cs.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    items(logs) { log ->
                        val title = mediaMap[log.mediaItemId]?.title ?: "Unknown item #${log.mediaItemId}"
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, color = cs.onSurface)
                                    Text(fmt.format(Date(log.watchedAt)), fontSize = 12.sp, color = cs.onSurfaceVariant)
                                }
                                Surface(shape = RoundedCornerShape(10.dp), color = cs.primary) {
                                    Text(formatDuration(log.durationMs), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cs.onPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isToday(ts: Long): Boolean {
    val now = System.currentTimeMillis()
    val day = 24L * 60 * 60 * 1000
    return now - ts < day && java.util.Calendar.getInstance().apply { timeInMillis = ts }.get(java.util.Calendar.DAY_OF_YEAR) == java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
}
private fun formatDuration(ms: Long): String {
    val s = ms / 1000
    val m = s / 60
    val sec = s % 60
    return if (m > 0) "${m}m ${sec}s" else "${sec}s"
}
