package com.nexus.android.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.android.data.api.NexusApi
import com.nexus.android.data.api.models.EditMessageRequest
import com.nexus.android.data.api.models.MessageResponse
import com.nexus.android.data.api.models.SendMessageRequest
import com.nexus.android.data.gateway.GatewayClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val editingMessageId: String? = null,
    val editingContent: String = "",
    val error: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val api: NexusApi,
    private val gateway: GatewayClient,
) : ViewModel() {

    private val _messages    = MutableStateFlow<List<MessageResponse>>(emptyList())
    val messages: StateFlow<List<MessageResponse>> = _messages.asStateFlow()

    private val _channelName = MutableStateFlow("channel")
    val channelName: StateFlow<String> = _channelName.asStateFlow()

    private val _typingUsers = MutableStateFlow<List<String>>(emptyList())
    val typingUsers: StateFlow<List<String>> = _typingUsers.asStateFlow()

    private val _uiState     = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Track currently logged-in userId (populated on loadChannel)
    var currentUserId: String = ""
        private set

    private var typingJob: Job? = null

    fun loadChannel(channelId: String, guildId: String) = viewModelScope.launch {
        try { api.getChannel(channelId).body()?.let { _channelName.value = it.name } } catch (_: Exception) {}
        try { api.getMessages(channelId).body()?.let { _messages.value = it } } catch (_: Exception) {}
        // Fetch own user id for permission checks
        try { api.getMe().body()?.let { currentUserId = it.id } } catch (_: Exception) {}

        launch {
            gateway.events.collect { event ->
                when (event.type) {
                    "MESSAGE_CREATE" -> if (event.data.optString("channel_id") == channelId) refreshMessages(channelId)
                    "MESSAGE_UPDATE" -> if (event.data.optString("channel_id") == channelId) refreshMessages(channelId)
                    "MESSAGE_DELETE" -> _messages.value = _messages.value.filter { it.id != event.data.optString("id") }
                    "TYPING_START"   -> if (event.data.optString("channel_id") == channelId) {
                        val uid = event.data.optString("user_id")
                        _typingUsers.value = (_typingUsers.value + uid).distinct()
                        typingJob?.cancel()
                        typingJob = launch { delay(10_000); _typingUsers.value = _typingUsers.value.filter { it != uid } }
                    }
                }
            }
        }
    }

    private fun refreshMessages(channelId: String) = viewModelScope.launch {
        try { api.getMessages(channelId).body()?.let { _messages.value = it } } catch (_: Exception) {}
    }

    fun sendMessage(channelId: String, content: String) = viewModelScope.launch {
        try { api.sendMessage(channelId, SendMessageRequest(content = content)) } catch (_: Exception) {}
    }

    fun sendTyping(channelId: String) = viewModelScope.launch {
        try { api.sendTyping(channelId) } catch (_: Exception) {}
    }

    // ── Edit ──────────────────────────────────────────────────────────────────

    fun startEditing(message: MessageResponse) {
        _uiState.value = _uiState.value.copy(
            editingMessageId = message.id,
            editingContent   = message.content ?: "",
        )
    }

    fun onEditContentChange(v: String) {
        _uiState.value = _uiState.value.copy(editingContent = v)
    }

    fun cancelEditing() {
        _uiState.value = _uiState.value.copy(editingMessageId = null, editingContent = "")
    }

    fun submitEdit(channelId: String) = viewModelScope.launch {
        val id      = _uiState.value.editingMessageId ?: return@launch
        val content = _uiState.value.editingContent.trim()
        if (content.isBlank()) return@launch
        try {
            api.editMessage(channelId, id, EditMessageRequest(content))
            cancelEditing()
            refreshMessages(channelId)
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(error = "Failed to edit message")
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun deleteMessage(channelId: String, messageId: String) = viewModelScope.launch {
        try {
            api.deleteMessage(channelId, messageId)
            _messages.value = _messages.value.filter { it.id != messageId }
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(error = "Failed to delete message")
        }
    }

    // ── React ─────────────────────────────────────────────────────────────────

    fun toggleReaction(channelId: String, messageId: String, emoji: String) = viewModelScope.launch {
        try {
            val msg = _messages.value.find { it.id == messageId } ?: return@launch
            val alreadyReacted = msg.reactions?.any { it.emoji == emoji && it.userId == currentUserId } == true
            if (alreadyReacted) api.removeReaction(channelId, messageId, emoji)
            else api.addReaction(channelId, messageId, emoji)
            refreshMessages(channelId)
        } catch (_: Exception) {}
    }
}
