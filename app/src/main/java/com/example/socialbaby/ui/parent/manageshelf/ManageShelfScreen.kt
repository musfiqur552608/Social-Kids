package com.example.socialbaby.ui.parent.manageshelf

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
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
fun ManageShelfScreen(
    viewModel: ManageShelfViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val shelves by viewModel.shelves.collectAsState()
    var newName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shelves & Playlists", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.primary, titleContentColor = cs.onPrimary)
            )
        },
        containerColor = cs.background
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Create New Shelf", fontWeight = FontWeight.Bold, color = cs.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text("e.g. Bedtime, Car Rides") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = { viewModel.addShelf(newName); newName = "" },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary)
                        ) { Text("Add", fontWeight = FontWeight.Bold) }
                    }
                    Text("Default shelves (Long Videos, Shorts, Photos) are auto-created on first launch.", fontSize = 11.sp, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Your Shelves", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = cs.onBackground)
            Spacer(Modifier.height(8.dp))
            if (shelves.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No shelves yet", color = cs.onSurfaceVariant) }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    items(shelves) { shelf ->
                        Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer), elevation = CardDefaults.cardElevation(1.dp)) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Folder, null, tint = cs.secondary)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(shelf.name, fontWeight = FontWeight.Bold, color = cs.onSurface)
                                        Text(shelf.kind.name, fontSize = 11.sp, color = cs.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteShelf(shelf) }) { Icon(Icons.Default.Delete, null, tint = cs.onSurfaceVariant) }
                            }
                        }
                    }
                }
            }
        }
    }
}
