package com.nexus.android.ui.screens.dm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nexus.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DmListScreen(
    onOpenDm: (String) -> Unit,
    onBack: () -> Unit,
    vm: DmViewModel = hiltViewModel(),
) {
    val dmChannels by vm.dmChannels.collectAsState()
    val uiState    by vm.uiState.collectAsState()
    var showNewDm  by remember { mutableStateOf(false) }
    var newDmUser  by remember { mutableStateOf("") }

    if (showNewDm) {
        AlertDialog(
            onDismissRequest = { showNewDm = false; newDmUser = "" },
            title = { Text("New Message") },
            text = {
                OutlinedTextField(
                    value = newDmUser, onValueChange = { newDmUser = it },
                    placeholder = { Text("User ID", color = NexusTextMuted) }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NexusBlurple, unfocusedBorderColor = NexusOutline),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newDmUser.isNotBlank()) {
                        vm.openDm(newDmUser.trim()) { onOpenDm(it) }
                        showNewDm = false; newDmUser = ""
                    }
                }) { Text("Open DM", color = NexusBlurple) }
            },
            dismissButton = { TextButton(onClick = { showNewDm = false; newDmUser = "" }) { Text("Cancel") } },
            containerColor = NexusDarkMedium,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Direct Messages", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { showNewDm = true }) { Icon(Icons.Default.Edit, "New DM", tint = NexusTextMuted) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusDarkMedium),
            )
        },
        containerColor = NexusDark,
    ) { padding ->
        if (uiState.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NexusBlurple)
            }
        } else if (dmChannels.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Forum, null, tint = NexusTextMuted, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No direct messages yet", color = NexusTextMuted)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showNewDm = true }) { Text("Start a conversation", color = NexusBlurple) }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(dmChannels, key = { it.id }) { ch ->
                    val other = ch.dmParticipants?.firstOrNull()?.user
                    val name  = other?.username ?: "Unknown"
                    val statusColor = when (other?.status) {
                        "online" -> NexusGreen; "idle" -> NexusYellow; "dnd" -> NexusRed; else -> NexusTextMuted
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenDm(ch.id) }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(44.dp)) {
                            Box(Modifier.size(44.dp).clip(CircleShape).background(NexusBlurple), contentAlignment = Alignment.Center) {
                                if (other?.avatar != null)
                                    AsyncImage(model = other.avatar, contentDescription = null, modifier = Modifier.fillMaxSize())
                                else
                                    Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Box(Modifier.size(13.dp).align(Alignment.BottomEnd).clip(CircleShape).background(NexusDark).padding(2.dp)) {
                                Box(Modifier.fillMaxSize().clip(CircleShape).background(statusColor))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, color = NexusTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            // Fix: explicit String type so Kotlin resolves Text overload unambiguously
                            val status: String? = other?.customStatus
                            if (status != null) {
                                Text(status, color = NexusTextMuted, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = NexusTextMuted, modifier = Modifier.size(16.dp))
                    }
                    HorizontalDivider(color = NexusOutline.copy(alpha = 0.3f), modifier = Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}
