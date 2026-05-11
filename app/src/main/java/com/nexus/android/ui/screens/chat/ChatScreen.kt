package com.nexus.android.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
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
import com.nexus.android.data.api.models.MessageResponse
import com.nexus.android.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(channelId: String, guildId: String, onBack: () -> Unit, vm: ChatViewModel = hiltViewModel()) {
    val messages     by vm.messages.collectAsState()
    val channelName  by vm.channelName.collectAsState()
    val typingUsers  by vm.typingUsers.collectAsState()
    val listState    = rememberLazyListState()
    var input        by remember { mutableStateOf("") }

    LaunchedEffect(channelId) { vm.loadChannel(channelId, guildId) }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

    Column(modifier = Modifier.fillMaxSize().background(NexusDarkLight)) {
        TopAppBar(
            title = { Text("#$channelName", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusDarkMedium),
        )
        LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(messages, key = { it.id }) { MessageItem(it) }
        }
        if (typingUsers.isNotEmpty()) {
            Text("${typingUsers.joinToString(", ")} is typing...", fontSize = 12.sp, color = NexusTextMuted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        }
        Row(modifier = Modifier.fillMaxWidth().background(NexusDarkMedium).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it; vm.sendTyping(channelId) },
                placeholder = { Text("Message #$channelName", color = NexusTextMuted) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedContainerColor = NexusDarkLight, unfocusedContainerColor = NexusDarkLight),
                maxLines = 5,
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { if (input.isNotBlank()) { vm.sendMessage(channelId, input.trim()); input = "" } },
                colors = IconButtonDefaults.iconButtonColors(containerColor = NexusBlurple),
            ) { Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White) }
        }
    }
}

@Composable
fun MessageItem(message: MessageResponse) {
    val sdf = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(NexusBlurple), contentAlignment = Alignment.Center) {
            if (message.author.avatar != null) AsyncImage(model = message.author.avatar, contentDescription = null, modifier = Modifier.fillMaxSize())
            else Text(message.author.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message.author.username, fontWeight = FontWeight.Bold, color = NexusTextPrimary, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(sdf.format(message.createdAt), color = NexusTextMuted, fontSize = 11.sp)
                if (message.editedAt != null) Text(" (edited)", color = NexusTextMuted, fontSize = 11.sp)
            }
            message.reference?.let { ref ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Divider(modifier = Modifier.width(2.dp).height(12.dp), color = NexusOutline)
                    Spacer(Modifier.width(4.dp))
                    Text(ref.author?.username ?: "", color = NexusTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Text(ref.content ?: "", color = NexusTextMuted, fontSize = 11.sp, maxLines = 1)
                }
            }
            if (!message.content.isNullOrBlank()) Text(message.content, color = NexusTextPrimary, fontSize = 15.sp, lineHeight = 20.sp)
            if (!message.reactions.isNullOrEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                    message.reactions.take(10).forEach { r ->
                        Surface(shape = RoundedCornerShape(12.dp), color = NexusDarkMedium) {
                            Text("${r.emoji} ${r.count}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 12.sp, color = NexusTextPrimary)
                        }
                    }
                }
            }
        }
    }
}
