package com.nexus.android.ui.screens.server

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import com.nexus.android.data.api.models.MemberResponse
import com.nexus.android.data.api.models.RoleResponse
import com.nexus.android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelMembersScreen(
    channelId: String,
    guildId: String,
    onBack: () -> Unit,
    vm: ServerSettingsViewModel = hiltViewModel(),
) {
    val members  by vm.members.collectAsState()
    val roles    by vm.roles.collectAsState()
    val channels by vm.channels.collectAsState()
    var query    by remember { mutableStateOf("") }

    LaunchedEffect(guildId) { vm.load(guildId) }

    val channelName = channels.firstOrNull { it.id == channelId }?.name ?: channelId
    val roleMap     = remember(roles) { roles.associateBy { it.id } }

    val filtered = members.filter {
        query.isBlank() ||
        it.user?.username?.contains(query, true) == true ||
        it.nickname?.contains(query, true) == true
    }

    val online  = filtered.filter { it.user?.status != null && it.user.status != "offline" }
    val offline = filtered.filter { it.user?.status == null || it.user.status == "offline" }

    fun topRoleName(m: MemberResponse): String =
        m.roles.mapNotNull { roleMap[it] }.maxByOrNull { it.position }?.name ?: "Members"

    val onlineGrouped = online.groupBy { topRoleName(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("#$channelName", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${online.size} Online · ${offline.size} Offline", color = NexusTextMuted, fontSize = 11.sp)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusDarkMedium),
            )
        },
        containerColor = NexusDark,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Search members", color = NexusTextMuted) },
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
                onlineGrouped.forEach { (roleName, groupMembers) ->
                    item {
                        Text("${roleName.uppercase()} — ${groupMembers.size}",
                            color = NexusTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                    items(groupMembers, key = { "on_${it.userId}" }) { m ->
                        ChannelMemberRow(m, roleMap)
                    }
                }
                if (offline.isNotEmpty()) {
                    item {
                        Text("OFFLINE — ${offline.size}",
                            color = NexusTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                    items(offline, key = { "off_${it.userId}" }) { m ->
                        ChannelMemberRow(m, roleMap, dimmed = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelMemberRow(member: MemberResponse, roleMap: Map<String, RoleResponse>, dimmed: Boolean = false) {
    val displayName = member.nickname ?: member.user?.username ?: member.userId
    val statusColor = when (member.user?.status) {
        "online" -> NexusGreen; "idle" -> NexusYellow; "dnd" -> NexusRed; else -> NexusTextMuted
    }
    // FIX: safe let chain — no !! operator; role lookup from map is nullable so must use let
    val topRoleColor = member.roles
        .mapNotNull { roleMap[it] }
        .maxByOrNull { it.position }
        ?.let { role -> if (role.color != 0) Color(0xFF000000 or role.color.toLong()) else null }

    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp)) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(NexusBlurple.copy(alpha = if (dimmed) 0.5f else 1f)),
                contentAlignment = Alignment.Center,
            ) {
                if (member.user?.avatar != null)
                    AsyncImage(model = member.user.avatar, contentDescription = null, modifier = Modifier.fillMaxSize())
                else
                    Text(displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.size(12.dp).align(Alignment.BottomEnd).clip(CircleShape).background(NexusDark).padding(2.dp)) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(statusColor))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(displayName, color = topRoleColor ?: if (dimmed) NexusTextMuted else NexusTextPrimary,
                fontWeight = FontWeight.Medium, fontSize = 14.sp)
            member.user?.customStatus?.let { Text(it, color = NexusTextMuted, fontSize = 12.sp, maxLines = 1) }
        }
    }
}
