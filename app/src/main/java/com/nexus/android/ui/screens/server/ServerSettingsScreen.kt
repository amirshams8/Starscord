package com.nexus.android.ui.screens.server

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nexus.android.data.api.models.*
import com.nexus.android.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

private enum class SubScreen { None, Overview, Members, Roles, Invites, Channels }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsScreen(
    guildId: String,
    onBack: () -> Unit,
    vm: ServerSettingsViewModel = hiltViewModel(),
) {
    val guild    by vm.guild.collectAsState()
    val members  by vm.members.collectAsState()
    val roles    by vm.roles.collectAsState()
    val invites  by vm.invites.collectAsState()
    val channels by vm.channels.collectAsState()
    val uiState  by vm.uiState.collectAsState()
    var sub      by remember { mutableStateOf(SubScreen.None) }

    LaunchedEffect(guildId) { vm.load(guildId) }

    when (sub) {
        SubScreen.Overview  -> OverviewSubScreen(guild, uiState, { name -> vm.saveGuildName(guildId, name) }) { sub = SubScreen.None }
        SubScreen.Members   -> MembersSubScreen(members, roles) { sub = SubScreen.None }
        SubScreen.Roles     -> RolesSubScreen(roles) { sub = SubScreen.None }
        SubScreen.Invites   -> InvitesSubScreen(invites, vm::deleteInvite) { sub = SubScreen.None }
        SubScreen.Channels  -> ChannelsSubScreen(channels) { sub = SubScreen.None }
        SubScreen.None      -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Server Settings", fontWeight = FontWeight.Bold) },
                        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.Close, "Close") } },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusDarkMedium),
                    )
                },
                containerColor = NexusDark,
            ) { padding ->
                if (uiState.loading) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NexusBlurple)
                    }
                } else {
                    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                        // Header
                        Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(80.dp).clip(CircleShape).background(NexusBlurple), contentAlignment = Alignment.Center) {
                                if (guild?.icon != null)
                                    AsyncImage(model = guild!!.icon, contentDescription = null, modifier = Modifier.fillMaxSize())
                                else
                                    Text(guild?.name?.take(2)?.uppercase() ?: "?", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(guild?.name ?: "", color = NexusTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        SectionHeader("Settings")
                        SettingsGroup {
                            SettingsRow(Icons.Default.Info,               "Overview")    { sub = SubScreen.Overview }
                            SettingsRow(Icons.Default.Shield,             "Moderation")  { }
                            SettingsRow(Icons.Default.Assignment,         "Audit Log")   { }
                            SettingsRow(Icons.Default.Tag,                "Channels")    { sub = SubScreen.Channels }
                            SettingsRow(Icons.Default.Extension,          "Integrations"){ }
                            SettingsRow(Icons.Default.EmojiEmotions,      "Emoji")       { }
                            SettingsRow(Icons.Default.StickyNote2,        "Stickers")    { }
                            SettingsRow(Icons.Default.Security,           "Security")    { }
                        }

                        Spacer(Modifier.height(16.dp))
                        SectionHeader("Community")
                        SettingsGroup {
                            SettingsRow(Icons.Default.Groups, "Enable Community") { }
                        }

                        Spacer(Modifier.height(16.dp))
                        SectionHeader("User Management")
                        SettingsGroup {
                            SettingsRow(Icons.Default.People,              "Members", badge = "${members.size}") { sub = SubScreen.Members }
                            SettingsRow(Icons.Default.AdminPanelSettings,  "Roles",   badge = "${roles.size}")   { sub = SubScreen.Roles }
                            SettingsRow(Icons.Default.Link,                "Invites", badge = "${invites.size}") { sub = SubScreen.Invites }
                            SettingsRow(Icons.Default.Gavel,               "Bans")    { }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title.uppercase(), color = NexusTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp))
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = NexusDarkMedium, modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, badge: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = NexusTextMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = NexusTextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (badge != null) { Text(badge, color = NexusTextMuted, fontSize = 13.sp); Spacer(Modifier.width(8.dp)) }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NexusTextMuted, modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(color = NexusOutline.copy(alpha = 0.4f), modifier = Modifier.padding(start = 52.dp))
}


// ── Overview ──────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewSubScreen(
    guild: GuildResponse?, uiState: ServerSettingsUiState,
    onSave: (String) -> Unit, onBack: () -> Unit,
) {
    var name by remember(guild?.name) { mutableStateOf(guild?.name ?: "") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Overview", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    TextButton(onClick = { onSave(name) }, enabled = !uiState.saving && name.isNotBlank()) {
                        if (uiState.saving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = NexusBlurple)
                        else Text("Save", color = NexusBlurple, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusDarkMedium),
            )
        },
        containerColor = NexusDark,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(16.dp))
            SectionHeader("Server Name")
            Surface(color = NexusDarkMedium, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = NexusBlurple, unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor   = NexusDarkLight, unfocusedContainerColor = NexusDarkLight,
                    ),
                )
            }
            Spacer(Modifier.height(24.dp))
            SectionHeader("Server Boost")
            Surface(color = NexusDarkMedium, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = NitroMid, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Level ${guild?.boostTier ?: 0}", color = NexusTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${guild?.boostCount ?: 0} boosts", color = NexusTextMuted, fontSize = 12.sp)
                    }
                }
            }
            uiState.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = NexusRed, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}


// ── Members ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MembersSubScreen(members: List<MemberResponse>, roles: List<RoleResponse>, onBack: () -> Unit) {
    var query   by remember { mutableStateOf("") }
    val roleMap = remember(roles) { roles.associateBy { it.id } }
    val filtered = members.filter {
        query.isBlank() || it.user?.username?.contains(query, true) == true || it.nickname?.contains(query, true) == true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Members", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusDarkMedium),
            )
        },
        containerColor = NexusDark,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Search Members", color = NexusTextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = NexusTextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NexusBlurple, unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = NexusDarkMedium, unfocusedContainerColor = NexusDarkMedium,
                ),
            )
            LazyColumn(Modifier.fillMaxSize()) {
                items(filtered, key = { it.userId }) { member ->
                    MemberListRow(member, roleMap)
                    HorizontalDivider(color = NexusOutline.copy(alpha = 0.3f), modifier = Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}

@Composable
private fun MemberListRow(member: MemberResponse, roleMap: Map<String, RoleResponse>) {
    val displayName = member.nickname ?: member.user?.username ?: member.userId
    val statusColor = when (member.user?.status) {
        "online" -> NexusGreen; "idle" -> NexusYellow; "dnd" -> NexusRed; else -> NexusTextMuted
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(44.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(NexusBlurple), contentAlignment = Alignment.Center) {
                if (member.user?.avatar != null)
                    AsyncImage(model = member.user.avatar, contentDescription = null, modifier = Modifier.fillMaxSize())
                else
                    Text(displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Box(Modifier.size(12.dp).align(Alignment.BottomEnd).clip(CircleShape).background(NexusDark).padding(2.dp)) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(statusColor))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(displayName, color = NexusTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            member.user?.let { Text("${it.username}#${it.discriminator}", color = NexusTextMuted, fontSize = 12.sp) }
            if (member.roles.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    member.roles.take(4).forEach { roleId ->
                        val role = roleMap[roleId]
                        val rc   = if ((role?.color ?: 0) != 0) Color(0xFF000000 or role!!.color.toLong()) else NexusOutline
                        Surface(shape = RoundedCornerShape(4.dp), color = rc.copy(alpha = 0.2f)) {
                            Row(Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(rc))
                                Spacer(Modifier.width(4.dp))
                                Text(role?.name ?: roleId, color = NexusTextPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                    if (member.roles.size > 4) Text("+${member.roles.size - 4}", color = NexusTextMuted, fontSize = 11.sp)
                }
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = NexusTextMuted, modifier = Modifier.size(16.dp))
    }
}


// ── Roles ─────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RolesSubScreen(roles: List<RoleResponse>, onBack: () -> Unit) {
    var query    by remember { mutableStateOf("") }
    val everyone = roles.firstOrNull { it.name == "@everyone" }
    val rest     = roles.filter { it.name != "@everyone" && (query.isBlank() || it.name.contains(query, true)) }.sortedByDescending { it.position }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Roles", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusDarkMedium),
            )
        },
        containerColor = NexusDark,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Search", color = NexusTextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = NexusTextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NexusBlurple, unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = NexusDarkMedium, unfocusedContainerColor = NexusDarkMedium,
                ),
            )
            Text("Use roles to group your server members and assign permissions.",
                color = NexusTextMuted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, bottom = 8.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                everyone?.let { ev ->
                    item {
                        RoleListRow(ev)
                        HorizontalDivider(color = NexusOutline.copy(alpha = 0.3f), modifier = Modifier.padding(start = 56.dp))
                    }
                }
                if (rest.isNotEmpty()) {
                    item {
                        Text("ROLES — ${rest.size}", color = NexusTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                    items(rest, key = { it.id }) { role ->
                        RoleListRow(role)
                        HorizontalDivider(color = NexusOutline.copy(alpha = 0.3f), modifier = Modifier.padding(start = 56.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleListRow(role: RoleResponse) {
    val rc = if (role.color != 0) Color(0xFF000000 or role.color.toLong()) else NexusTextMuted
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(rc.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Shield, null, tint = rc, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(role.name, color = NexusTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("${role.memberCount} Members", color = NexusTextMuted, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = NexusTextMuted, modifier = Modifier.size(16.dp))
    }
}


// ── Invites ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvitesSubScreen(invites: List<GuildInviteResponse>, onDelete: (String) -> Unit, onBack: () -> Unit) {
    val isoFmt = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invites", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusDarkMedium),
            )
        },
        containerColor = NexusDark,
    ) { padding ->
        if (invites.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No active invites", color = NexusTextMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(invites, key = { it.code }) { invite ->
                    InviteCard(invite, isoFmt) { onDelete(invite.code) }
                }
            }
        }
    }
}

@Composable
private fun InviteCard(invite: GuildInviteResponse, isoFmt: SimpleDateFormat, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val expiryText = remember(invite.expiresAt) {
        if (invite.expiresAt == null) "Never expires"
        else try {
            val diff = (isoFmt.parse(invite.expiresAt)?.time ?: 0L) - System.currentTimeMillis()
            if (diff <= 0) "Expired"
            else "%02d:%02d:%02d".format(diff / 3_600_000, (diff % 3_600_000) / 60_000, (diff % 60_000) / 1_000)
                .let { "Expires in: $it" }
        } catch (_: Exception) { "Unknown" }
    }
    Surface(shape = RoundedCornerShape(12.dp), color = NexusDarkMedium, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(invite.code, color = NexusTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val ch     = invite.channel
                val prefix = if (ch?.type == "voice") "🔊" else "#"
                Text("$prefix ${ch?.name ?: invite.channelId}", color = NexusTextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = NexusGreen, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(expiryText, color = NexusGreen, fontSize = 12.sp)
                }
                invite.creator?.let { u ->
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(20.dp).clip(CircleShape).background(NexusBlurple), contentAlignment = Alignment.Center) {
                            Text(u.username.take(1).uppercase(), color = Color.White, fontSize = 10.sp)
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(u.username, color = NexusTextMuted, fontSize = 12.sp)
                        Spacer(Modifier.width(12.dp))
                        Text("Uses: ${invite.uses}", color = NexusTextPrimary, fontSize = 12.sp)
                    }
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null, tint = NexusTextMuted) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Revoke Invite", color = NexusRed) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = NexusRed) },
                    )
                }
            }
        }
    }
}


// ── Channels ──────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelsSubScreen(channels: List<ChannelResponse>, onBack: () -> Unit) {
    val categories    = channels.filter { it.type == "category" }.sortedBy { it.position }
    val uncategorized = channels.filter { it.type != "category" && it.parentId == null }.sortedBy { it.position }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Channels", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusDarkMedium),
            )
        },
        containerColor = NexusDark,
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(uncategorized) { ch -> ChannelSettingsRow(ch) }
            categories.forEach { cat ->
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(cat.name.uppercase(), color = NexusTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = {}) { Text("Edit", color = NexusBlurple, fontSize = 12.sp) }
                    }
                }
                items(channels.filter { it.parentId == cat.id }.sortedBy { it.position }) { ch -> ChannelSettingsRow(ch) }
            }
        }
    }
}

@Composable
private fun ChannelSettingsRow(channel: ChannelResponse) {
    val prefix = when (channel.type) { "voice" -> "🔊"; "announcement" -> "📢"; "forum" -> "💬"; else -> "#" }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(prefix, color = NexusTextMuted, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text(channel.name, color = NexusTextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = NexusTextMuted, modifier = Modifier.size(16.dp))
    }
    HorizontalDivider(color = NexusOutline.copy(alpha = 0.3f), modifier = Modifier.padding(start = 40.dp))
}
