package com.nexus.android.ui.screens.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
fun ChatScreen(
    channelId: String,
    guildId: String,
    onBack: () -> Unit,
    onOpenMembers: (String, String) -> Unit,
    vm: ChatViewModel = hiltViewModel(),
) {
    val messages    by vm.messages.collectAsState()
    val channelName by vm.channelName.collectAsState()
    val typingUsers by vm.typingUsers.collectAsState()
    val uiState     by vm.uiState.collectAsState()
    val listState   = rememberLazyListState()
    var input       by remember { mutableStateOf("") }
    var emojiPickerMessageId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(channelId)     { vm.loadChannel(channelId, guildId) }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

    Column(modifier = Modifier.fillMaxSize().background(NexusDarkLight)) {
        TopAppBar(
            title = {
                Row(
                    modifier = Modifier.clickable { onOpenMembers(channelId, guildId) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("#$channelName", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("›", color = NexusTextMuted, fontSize = 18.sp)
                }
            },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
            actions = {
                IconButton(onClick = { onOpenMembers(channelId, guildId) }) {
                    Icon(Icons.Default.People, contentDescription = "Members", tint = NexusTextMuted)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusDarkMedium),
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageItem(
                    message      = msg,
                    isOwn        = msg.author.id == vm.currentUserId,
                    editingId    = uiState.editingMessageId,
                    editingText  = uiState.editingContent,
                    onEditChange = vm::onEditContentChange,
                    onEditSubmit = { vm.submitEdit(channelId) },
                    onEditCancel = vm::cancelEditing,
                    onEdit       = { vm.startEditing(msg) },
                    onDelete     = { vm.deleteMessage(channelId, msg.id) },
                    onReact      = { emoji -> vm.toggleReaction(channelId, msg.id, emoji) },
                    onOpenEmoji  = { emojiPickerMessageId = msg.id },
                )
            }
        }

        if (emojiPickerMessageId != null) {
            Surface(color = NexusDarkMedium) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf("👍","❤️","😂","😮","😢","🔥","🎉","✅").forEach { emoji ->
                        Text(emoji, fontSize = 22.sp, modifier = Modifier.clickable {
                            vm.toggleReaction(channelId, emojiPickerMessageId!!, emoji)
                            emojiPickerMessageId = null
                        })
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { emojiPickerMessageId = null }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NexusTextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (typingUsers.isNotEmpty()) {
            Text("${typingUsers.joinToString(", ")} is typing...",
                fontSize = 12.sp, color = NexusTextMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        }

        if (uiState.editingMessageId != null) {
            Row(
                modifier = Modifier.fillMaxWidth().background(NexusDarkMedium).padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = NexusBlurple, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Editing message", color = NexusBlurple, fontSize = 12.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = vm::cancelEditing) { Text("Cancel", color = NexusTextMuted, fontSize = 12.sp) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(NexusDarkMedium).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = if (uiState.editingMessageId != null) uiState.editingContent else input,
                onValueChange = { v ->
                    if (uiState.editingMessageId != null) vm.onEditContentChange(v)
                    else { input = v; vm.sendTyping(channelId) }
                },
                placeholder = { Text("Message #$channelName", color = NexusTextMuted) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Color.Transparent,
                    unfocusedBorderColor    = Color.Transparent,
                    focusedContainerColor   = NexusDarkLight,
                    unfocusedContainerColor = NexusDarkLight,
                ),
                maxLines = 5,
            )
            Spacer(Modifier.width(8.dp))
            if (uiState.editingMessageId != null) {
                IconButton(onClick = { vm.submitEdit(channelId) },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = NexusGreen)) {
                    Icon(Icons.Default.Check, contentDescription = "Confirm edit", tint = Color.White)
                }
            } else {
                IconButton(
                    onClick = { if (input.isNotBlank()) { vm.sendMessage(channelId, input.trim()); input = "" } },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = NexusBlurple)) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: MessageResponse,
    isOwn: Boolean,
    editingId: String?,
    editingText: String,
    onEditChange: (String) -> Unit,
    onEditSubmit: () -> Unit,
    onEditCancel: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReact: (String) -> Unit,
    onOpenEmoji: () -> Unit,
) {
    val sdf    = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val parser = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()) }
    var showMenu          by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Message") },
            text  = { Text("Are you sure? This cannot be undone.") },
            confirmButton = { TextButton(onClick = { showDeleteConfirm = false; onDelete() }) { Text("Delete", color = NexusRed) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = { if (isOwn) showMenu = true })
                .padding(vertical = 4.dp),
        ) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(NexusBlurple), contentAlignment = Alignment.Center) {
                if (message.author.avatar != null)
                    AsyncImage(model = message.author.avatar, contentDescription = null, modifier = Modifier.fillMaxSize())
                else
                    Text(message.author.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(message.author.username, fontWeight = FontWeight.Bold, color = NexusTextPrimary, fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    val time = remember(message.createdAt) {
                        try { sdf.format(parser.parse(message.createdAt)!!) } catch (_: Exception) { "" }
                    }
                    Text(time, color = NexusTextMuted, fontSize = 11.sp)
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
                if (editingId == message.id) {
                    OutlinedTextField(value = editingText, onValueChange = onEditChange,
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp), singleLine = false, maxLines = 5)
                } else {
                    if (!message.content.isNullOrBlank())
                        Text(message.content, color = NexusTextPrimary, fontSize = 15.sp, lineHeight = 20.sp)
                }
                if (!message.reactions.isNullOrEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                        message.reactions.take(10).forEach { r ->
                            Surface(shape = RoundedCornerShape(12.dp), color = NexusDarkMedium, onClick = { onReact(r.emoji) }) {
                                Text("${r.emoji} ${r.count}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 12.sp, color = NexusTextPrimary)
                            }
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = NexusDarkMedium, onClick = { onOpenEmoji() }) {
                            Text("＋", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 12.sp, color = NexusTextMuted)
                        }
                    }
                }
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("Add Reaction") }, onClick = { showMenu = false; onOpenEmoji() },
                leadingIcon = { Text("😀", fontSize = 16.sp) })
            if (isOwn) {
                DropdownMenuItem(text = { Text("Edit Message") }, onClick = { showMenu = false; onEdit() },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) })
                DropdownMenuItem(text = { Text("Delete Message", color = NexusRed) },
                    onClick = { showMenu = false; showDeleteConfirm = true },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = NexusRed) })
            }
        }
    }
}
