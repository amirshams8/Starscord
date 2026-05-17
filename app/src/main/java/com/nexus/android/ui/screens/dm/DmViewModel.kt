package com.nexus.android.ui.screens.dm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.android.data.api.NexusApi
import com.nexus.android.data.api.models.ChannelResponse
import com.nexus.android.data.api.models.OpenDmRequest
import com.nexus.android.data.gateway.GatewayClient
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DmUiState(val loading: Boolean = false, val error: String? = null)

@HiltViewModel
class DmViewModel @Inject constructor(
    private val api: NexusApi,
    private val gateway: GatewayClient,
) : ViewModel() {
    private val _dmChannels = MutableStateFlow<List<ChannelResponse>>(emptyList())
    val dmChannels: StateFlow<List<ChannelResponse>> = _dmChannels.asStateFlow()
    private val _uiState = MutableStateFlow(DmUiState())
    val uiState: StateFlow<DmUiState> = _uiState.asStateFlow()
    private var myUserId = ""
    private val gson = Gson()

    init { loadDms(); collectGateway() }

    private fun loadDms() = viewModelScope.launch {
        _uiState.value = DmUiState(loading = true)
        try {
            myUserId = api.getMe().body()?.id ?: ""
            _dmChannels.value = stripSelf(api.getDmChannels().body() ?: emptyList())
            _uiState.value = DmUiState()
        } catch (e: Exception) { _uiState.value = DmUiState(error = e.message) }
    }

    private fun collectGateway() = viewModelScope.launch {
        gateway.events.collect { event ->
            if (event.type == "DM_CHANNEL_CREATE") {
                val ch = gson.fromJson(event.data.toString(), ChannelResponse::class.java)
                if (_dmChannels.value.none { it.id == ch.id })
                    _dmChannels.value = listOf(stripSelf(listOf(ch)).first()) + _dmChannels.value
            }
        }
    }

    fun openDm(recipientId: String, onSuccess: (String) -> Unit) = viewModelScope.launch {
        try {
            val resp = api.openDm(OpenDmRequest(recipientId))
            if (resp.isSuccessful && resp.body() != null) {
                val ch = resp.body()!!
                if (_dmChannels.value.none { it.id == ch.id })
                    _dmChannels.value = listOf(stripSelf(listOf(ch)).first()) + _dmChannels.value
                onSuccess(ch.id)
            } else _uiState.value = _uiState.value.copy(error = "User not found")
        } catch (e: Exception) { _uiState.value = _uiState.value.copy(error = e.message) }
    }

    private fun stripSelf(channels: List<ChannelResponse>) =
        channels.map { ch -> ch.copy(dmParticipants = ch.dmParticipants?.filter { it.userId != myUserId }) }
}
