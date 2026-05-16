package com.nexus.android.ui.screens.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nexus.android.data.api.models.ChannelResponse
import com.nexus.android.data.api.models.GuildResponse
import com.nexus.android.ui.theme.*

@Composable
fun HomeScreen(
    onOpenChannel: (String, String) -> Unit,
    onOpenVoice: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenServerSettings: (String) -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val guilds        by vm.guilds.collectAsState()
    val selectedGuild by vm.selectedGuild.collectAsState()
    val channels      by vm.channels.collectAsState()
    val uiState       by vm.uiState.collectAsState()
    val context       = LocalContext.current

    if (uiState.showCreateGuildDialog)
        CreateGuildDialog(loading = uiState.createGuildLoading, error = uiState.error,
            onDismiss = vm::hideCreateGuildDialog, onCreate = vm::createGuild)
    if (uiState.showJoinGuildDialog)
        JoinGuildDialog(loading = uiState.loading, error = uiState.error,
            onDismiss = vm::hideJoinGuildDialog, onJoin = vm::joinGuildByInvite)
    if (uiState.showCreateChannelDialog)
        CreateChannelDialog(loading = uiState.createChannelLoading, error = uiState.error,
            onDismiss = vm::hideCreateChannelDialog, onCreate = { name, type -> vm.createChannel(name, type) })
    if (uiState.showInviteDialog && uiState.inviteCode != null)
        InviteDialog(code = uiState.inviteCode!!, onDismiss = vm::hideInviteDialog, onCopy = { code ->
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Invite Link", "nexus.gg/$code"))
        })

    Row(modifier = Modifier.fillMaxSize().background(NexusDark)) {
        ServerRail(
            guilds       = guilds,
            selectedId   = selectedGuild?.id,
            onSelect     = vm::selectGuild,
            onAddServer  = vm::showCreateGuildDialog,
            onJoinServer = vm::showJoinGuildDialog,
            onOpenProfile = onOpenProfile,
        )
        if (selectedGuild != null) {
            ChannelSidebar(
                guild           = selectedGuild!!,
                channels        = channels,
                onSelect        = { ch -> if (ch.type == "voice") onOpenVoice(ch.id) else onOpenChannel(ch.id, selectedGuild!!.id) },
                onLeaveGuild    = { vm.leaveGuild(selectedGuild!!.id) },
                onCreateChannel = vm::showCreateChannelDialog,
                onInvite        = { vm.generateInvite(it) },
                onOpenSettings  = onOpenSettings,
                onOpenServerSettings = { onOpenServerSettings(selectedGuild!!.id) },
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No server selected", color = NexusTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + to create or join one", color = NexusTextMuted, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ServerRail(
    guilds: List<GuildResponse>, selectedId: String?,
    onSelect: (GuildResponse) -> Unit, onAddServer: () -> Unit,
    onJoinServer: () -> Unit, onOpenProfile: () -> Unit,
) {
    var showAddMenu by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.width(72.dp).fillMaxHeight().background(NexusDark).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(NexusBlurple).clickable(onClick = onOpenProfile),
            contentAlignment = Alignment.Center,
        ) { Text("N", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) }

        Divider(modifier = Modifier.width(32.dp).padding(vertical = 8.dp), color = NexusOutline)

        LazyColumn(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(guilds, key = { it.id }) { guild ->
                val selected = guild.id == selectedId
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(if (selected) RoundedCornerShape(16.dp) else CircleShape)
                        .background(if (selected) NexusBlurple else NexusDarkMedium)
                        .clickable { onSelect(guild) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (guild.icon != null)
                        AsyncImage(model = guild.icon, contentDescription = guild.name, modifier = Modifier.fillMaxSize())
                    else
                        Text(guild.name.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        Divider(modifier = Modifier.width(32.dp).padding(vertical = 8.dp), color = NexusOutline)

        Box {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(NexusDarkMedium).clickable { showAddMenu = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Server", tint = NexusGreen, modifier = Modifier.size(28.dp))
            }
            DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                DropdownMenuItem(text = { Text("Create a Server") }, onClick = { showAddMenu = false; onAddServer() },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = NexusBlurple) })
                DropdownMenuItem(text = { Text("Join a Server") }, onClick = { showAddMenu = false; onJoinServer() },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = NexusGreen) })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelSidebar(
    guild: GuildResponse, channels: List<ChannelResponse>,
    onSelect: (ChannelResponse) -> Unit, onLeaveGuild: () -> Unit,
    onCreateChannel: () -> Unit, onInvite: (channelId: String) -> Unit,
    onOpenSettings: () -> Unit, onOpenServerSettings: () -> Unit,
) {
    var showGuildMenu    by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Leave '${guild.name}'?") },
            text  = { Text("You won't be able to rejoin without an invite.") },
            confirmButton = { TextButton(onClick = { showLeaveConfirm = false; onLeaveGuild() }) { Text("Leave", color = NexusRed) } },
            dismissButton = { TextButton(onClick = { showLeaveConfirm = false }) { Text("Cancel") } },
        )
    }

    Column(modifier = Modifier.width(240.dp).fillMaxHeight().background(NexusDarkMedium)) {
        Box {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showGuildMenu = true }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(guild.name, fontWeight = FontWeight.Bold, color = NexusTextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text("▾", color = NexusTextMuted, fontSize = 12.sp)
            }
            DropdownMenu(expanded = showGuildMenu, onDismissRequest = { showGuildMenu = false }) {
                DropdownMenuItem(text = { Text("Invite People") },
                    onClick = { showGuildMenu = false; channels.firstOrNull { it.type == "text" }?.let { onInvite(it.id) } },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = NexusGreen) })
                DropdownMenuItem(text = { Text("Create Channel") },
                    onClick = { showGuildMenu = false; onCreateChannel() },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = NexusBlurple) })
                DropdownMenuItem(text = { Text("Server Settings") },
                    onClick = { showGuildMenu = false; onOpenServerSettings() },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = NexusTextMuted) })
                Divider(color = NexusOutline)
                DropdownMenuItem(text = { Text("Leave Server", color = NexusRed) },
                    onClick = { showGuildMenu = false; showLeaveConfirm = true },
                    leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = NexusRed) })
            }
        }

        Divider(color = NexusOutline)

        val categories    = channels.filter { it.type == "category" }.sortedBy { it.position }
        val uncategorized = channels.filter { it.type != "category" && it.parentId == null }

        LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            items(uncategorized) { ch -> ChannelRow(ch, { onSelect(ch) }, { onInvite(ch.id) }) }
            categories.forEach { cat ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, bottom = 4.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(cat.name.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = NexusTextMuted, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.Add, contentDescription = "Add channel", tint = NexusTextMuted,
                            modifier = Modifier.size(16.dp).clickable { onCreateChannel() })
                    }
                }
                items(channels.filter { it.parentId == cat.id }.sortedBy { it.position }) { ch ->
                    ChannelRow(ch, { onSelect(ch) }, { onInvite(ch.id) })
                }
            }
        }

        Divider(color = NexusOutline)

        Row(
            modifier = Modifier.fillMaxWidth().background(NexusDark).padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(NexusBlurple),
                contentAlignment = Alignment.Center) {
                Text("A", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Text("Online", color = NexusGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = NexusTextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelRow(channel: ChannelResponse, onClick: () -> Unit, onInvite: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val prefix = when (channel.type) { "voice" -> "🔊"; "announcement" -> "📢"; "forum" -> "💬"; else -> "#" }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = { showMenu = true })
                .padding(horizontal = 8.dp, vertical = 1.dp)
                .clip(RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(prefix, color = NexusTextMuted, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(channel.name, color = NexusTextMuted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            if (channel.type == "text") {
                DropdownMenuItem(text = { Text("Copy Invite Link") }, onClick = { showMenu = false; onInvite() },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) })
            }
        }
    }
}

@Composable
fun CreateGuildDialog(loading: Boolean, error: String?, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = NexusDarkMedium, tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Create a Server", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NexusTextPrimary, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = NexusTextMuted) }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("SERVER NAME") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), isError = error != null)
                error?.let { Text(it, color = NexusRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
                Spacer(Modifier.height(20.dp))
                Button(onClick = { onCreate(name) }, enabled = !loading && name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NexusBlurple)) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Create", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun JoinGuildDialog(loading: Boolean, error: String?, onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = NexusDarkMedium, tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Join a Server", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NexusTextPrimary, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = NexusTextMuted) }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("INVITE LINK OR CODE") },
                    placeholder = { Text("nexus.gg/ABCD1234", color = NexusTextMuted) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), isError = error != null)
                error?.let { Text(it, color = NexusRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
                Spacer(Modifier.height(20.dp))
                Button(onClick = { onJoin(code) }, enabled = !loading && code.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NexusBlurple)) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Join Server", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CreateChannelDialog(loading: Boolean, error: String?, onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name         by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("text") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = NexusDarkMedium, tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Create Channel", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NexusTextPrimary, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = NexusTextMuted) }
                }
                Spacer(Modifier.height(16.dp))
                Text("CHANNEL TYPE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NexusTextMuted)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("text" to "# Text", "voice" to "🔊 Voice").forEach { (type, label) ->
                        val selected = selectedType == type
                        Surface(shape = RoundedCornerShape(8.dp),
                            color = if (selected) NexusBlurple else NexusDarkLight,
                            modifier = Modifier.clickable { selectedType = type }.weight(1f)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                                Text(label, color = if (selected) Color.White else NexusTextMuted,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("CHANNEL NAME") },
                    placeholder = { Text(if (selectedType == "voice") "General" else "new-channel", color = NexusTextMuted) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), isError = error != null)
                error?.let { Text(it, color = NexusRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
                Spacer(Modifier.height(20.dp))
                Button(onClick = { onCreate(name, selectedType) }, enabled = !loading && name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NexusBlurple)) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Create Channel", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun InviteDialog(code: String, onDismiss: () -> Unit, onCopy: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = NexusDarkMedium, tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Invite People", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NexusTextPrimary)
                Spacer(Modifier.height(16.dp))
                Text("Share this link to invite others:", color = NexusTextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = NexusDarkLight, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("nexus.gg/$code", color = NexusTextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onCopy(code) }) { Text("Copy", color = NexusBlurple, fontWeight = FontWeight.Bold) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Expires in 24 hours.", color = NexusTextMuted, fontSize = 11.sp)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Done", color = NexusBlurple) }
            }
        }
    }
}
