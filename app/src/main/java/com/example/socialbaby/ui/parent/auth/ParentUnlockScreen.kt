package com.example.socialbaby.ui.parent.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentUnlockScreen(
    viewModel: ParentUnlockViewModel = hiltViewModel(),
    onUnlocked: () -> Unit,
    onBackToChild: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val pin by viewModel.pin.collectAsState()
    val isPinSet by viewModel.isPinSet.collectAsState()
    val error by viewModel.error.collectAsState()
    val unlocked by viewModel.unlocked.collectAsState()

    LaunchedEffect(unlocked) {
        if (unlocked) {
            onUnlocked()
            viewModel.resetUnlocked()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isPinSet == false) "Set Parent PIN" else "Parent Unlock", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackToChild) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.surface, titleContentColor = cs.onSurface)
            )
        },
        containerColor = cs.background
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(cs.primary),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Lock, null, modifier = Modifier.size(40.dp), tint = cs.onPrimary) }

            Spacer(Modifier.height(16.dp))
            Text(
                if (isPinSet == false) "Create a 4-6 digit PIN" else "Enter your PIN",
                fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = cs.onBackground
            )
            Text(
                if (isPinSet == false) "This protects Parent Mode from kids." else "Required to manage content & settings.",
                color = cs.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(28.dp))

            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(6) { idx ->
                    Box(
                        modifier = Modifier.size(18.dp).clip(CircleShape)
                            .background(if (idx < pin.length) cs.onBackground else cs.outlineVariant)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(pin.ifEmpty { "• • • •" }, fontSize = 18.sp, letterSpacing = 8.sp, fontWeight = FontWeight.Bold, color = cs.onBackground)

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = cs.errorContainer) {
                    Text(error!!, color = cs.onErrorContainer, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Keypad
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "⌫")
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                keys.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { key ->
                            Button(
                                onClick = {
                                    when (key) {
                                        "C" -> viewModel.onClear()
                                        "⌫" -> viewModel.onBackspace()
                                        else -> viewModel.onPinDigit(key)
                                    }
                                },
                                modifier = Modifier.weight(1f).height(62.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when (key) {
                                        "C" -> cs.secondaryContainer
                                        else -> cs.surfaceContainer
                                    },
                                    contentColor = cs.onSurface
                                ),
                                elevation = ButtonDefaults.buttonElevation(2.dp)
                            ) {
                                if (key == "⌫") Icon(Icons.Default.Backspace, null) else Text(key, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { viewModel.onConfirm() },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary)
            ) {
                Text(if (isPinSet == false) "Set PIN & Enter" else "Unlock", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }

            TextButton(onClick = onBackToChild) { Text("Back to Kid Mode", fontWeight = FontWeight.Bold) }
        }
    }
}
