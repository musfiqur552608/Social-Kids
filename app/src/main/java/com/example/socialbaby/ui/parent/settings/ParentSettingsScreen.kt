package com.example.socialbaby.ui.parent.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSettingsScreen(
    viewModel: ParentSettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val cs = MaterialTheme.colorScheme

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
                        Column { Text("Shorts vertical feed", fontWeight = FontWeight.Bold, color = cs.onSurface); Text("TikTok-like swipe", fontSize = 12.sp, color = cs.onSurfaceVariant) }
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
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Safety note", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = cs.onSurface)
                        Text("Direct links use native player; YouTube/TikTok use embed only while watching. No analytics, no ads, no account. Works 100% offline for local media.", fontSize = 12.sp, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
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
