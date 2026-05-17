package com.nexus.android.ui.screens.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexus.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    channelId: String,
    channelName: String,
    onLeave: () -> Unit,
    vm: VoiceViewModel = hiltViewModel(),
) {
    val uiState      by vm.uiState.collectAsState()
    val participants by vm.participants.collectAsState()
    val context      = LocalContext.current

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.join(channelId) else onLeave()
    }
    LaunchedEffect(channelId) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
            vm.join(channelId)
        else
            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    DisposableEffect(Unit) { onDispose { vm.leave(channelId) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🔊 $channelName", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Voice Connected", color = NexusGreen, fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.leave(channelId); onLeave() }) {
                        Icon(Icons.Default.CallEnd, "Leave", tint = NexusRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusDarkMedium),
            )
        },
        containerColor = NexusDark,
        bottomBar = {
            Row(Modifier.fillMaxWidth().background(NexusDarkMedium).padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                FloatingActionButton(onClick = vm::toggleMute,
                    containerColor = if (uiState.muted) NexusRed else NexusDarkLight, modifier = Modifier.size(56.dp)) {
                    Icon(if (uiState.muted) Icons.Default.MicOff else Icons.Default.Mic, "Mute", tint = Color.White)
                }
                FloatingActionButton(onClick = vm::toggleDeafen,
                    containerColor = if (uiState.deafened) NexusRed else NexusDarkLight, modifier = Modifier.size(56.dp)) {
                    Icon(if (uiState.deafened) Icons.Default.HeadsetOff else Icons.Default.Headset, "Deafen", tint = Color.White)
                }
                FloatingActionButton(onClick = { vm.leave(channelId); onLeave() },
                    containerColor = NexusRed, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.CallEnd, "Leave", tint = Color.White)
                }
            }
        },
    ) { padding ->
        when {
            uiState.error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(uiState.error!!, color = NexusRed)
            }
            uiState.connecting -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NexusBlurple)
                    Spacer(Modifier.height(12.dp))
                    Text("Connecting to voice...", color = NexusTextMuted)
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                item {
                    Text("PARTICIPANTS — ${participants.size}", color = NexusTextMuted,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                }
                items(participants) { p ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(NexusBlurple), contentAlignment = Alignment.Center) {
                            Text(p.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(p, color = NexusTextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        if (uiState.speaking.contains(p))
                            Icon(Icons.Default.Mic, null, tint = NexusGreen, modifier = Modifier.size(16.dp))
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
