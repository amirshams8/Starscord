package com.nexus.android.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.android.data.api.NexusApi
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

@HiltViewModel
class ChatViewModel @Inject constructor(private val api: NexusApi, private val gateway: GatewayClient) : ViewModel() {
    private val _messages     = MutableStateFlow<List<MessageResponse>>(emptyList())
    val messages: StateFlow<List<MessageResponse>> = _messages.asStateFlow()

    private val _channelName  = MutableStateFlow("channel")
    val channelName: StateFlow<String> = _channelName.asStateFlow()

    private val _typingUsers  = MutableStateFlow<List<String>>(emptyList())
    val typingUsers: StateFlow<List<String>> = _typingUsers.asStateFlow()

    private var typingJob: Job? = null

    fun loadChannel(channelId: String, guildId: String) = viewModelScope.launch {
        try { api.getChannel(channelId).body()?.let { _channelName.value = it.name } } catch (_: Exception) {}
        try { api.getMessages(channelId).body()?.let { _messages.value = it } } catch (_: Exception) {}
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
                        typingJob = launch { delay(10000); _typingUsers.value = _typingUsers.value.filter { it != uid } }
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
}
