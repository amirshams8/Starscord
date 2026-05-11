package com.nexus.android.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.nexus.android.data.api.models.ChannelResponse
import com.nexus.android.data.api.models.GuildResponse
import com.nexus.android.ui.theme.*

@Composable
fun HomeScreen(onOpenChannel: (String, String) -> Unit, onOpenVoice: (String) -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val guilds         by vm.guilds.collectAsState()
    val selectedGuild  by vm.selectedGuild.collectAsState()
    val channels       by vm.channels.collectAsState()

    Row(modifier = Modifier.fillMaxSize().background(NexusDark)) {
        ServerRail(guilds = guilds, selectedId = selectedGuild?.id, onSelect = vm::selectGuild)
        if (selectedGuild != null) {
            ChannelSidebar(
                guild    = selectedGuild!!,
                channels = channels,
                onSelect = { ch ->
                    if (ch.type == "voice") onOpenVoice(ch.id)
                    else onOpenChannel(ch.id, selectedGuild!!.id)
                }
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a server", color = NexusTextMuted)
            }
        }
    }
}

@Composable
fun ServerRail(guilds: List<GuildResponse>, selectedId: String?, onSelect: (GuildResponse) -> Unit) {
    Column(
        modifier = Modifier.width(72.dp).fillMaxHeight().background(NexusDark).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(NexusBlurple), contentAlignment = Alignment.Center) {
            Text("N", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Divider(modifier = Modifier.width(32.dp).padding(vertical = 8.dp), color = NexusOutline)
        guilds.forEach { guild ->
            val selected = guild.id == selectedId
            Box(
                modifier = Modifier.padding(bottom = 8.dp).size(48.dp)
                    .clip(if (selected) RoundedCornerShape(16.dp) else CircleShape)
                    .background(if (selected) NexusBlurple else NexusDarkMedium)
                    .clickable { onSelect(guild) },
                contentAlignment = Alignment.Center,
            ) {
                if (guild.icon != null) AsyncImage(model = guild.icon, contentDescription = guild.name, modifier = Modifier.fillMaxSize())
                else Text(guild.name.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ChannelSidebar(guild: GuildResponse, channels: List<ChannelResponse>, onSelect: (ChannelResponse) -> Unit) {
    Column(modifier = Modifier.width(240.dp).fillMaxHeight().background(NexusDarkMedium)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(guild.name, fontWeight = FontWeight.Bold, color = NexusTextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        }
        Divider(color = NexusOutline)
        val categories    = channels.filter { it.type == "category" }.sortedBy { it.position }
        val uncategorized = channels.filter { it.type != "category" && it.parentId == null }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            items(uncategorized) { ChannelRow(it) { onSelect(it) } }
            categories.forEach { cat ->
                item { Text(cat.name.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NexusTextMuted, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)) }
                items(channels.filter { it.parentId == cat.id }.sortedBy { it.position }) { ch -> ChannelRow(ch) { onSelect(ch) } }
            }
        }
    }
}

@Composable
fun ChannelRow(channel: ChannelResponse, onClick: () -> Unit) {
    val prefix = when (channel.type) { "voice" -> "🔊"; "announcement" -> "📢"; "forum" -> "💬"; else -> "#" }
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp).clip(RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(prefix, color = NexusTextMuted, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(channel.name, color = NexusTextMuted, fontSize = 14.sp)
    }
}
