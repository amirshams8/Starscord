package com.nexus.android.ui.screens.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.android.data.api.NexusApi
import com.nexus.android.data.api.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerSettingsUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ServerSettingsViewModel @Inject constructor(private val api: NexusApi) : ViewModel() {

    private val _guild    = MutableStateFlow<GuildResponse?>(null)
    val guild: StateFlow<GuildResponse?> = _guild.asStateFlow()

    private val _members  = MutableStateFlow<List<MemberResponse>>(emptyList())
    val members: StateFlow<List<MemberResponse>> = _members.asStateFlow()

    private val _roles    = MutableStateFlow<List<RoleResponse>>(emptyList())
    val roles: StateFlow<List<RoleResponse>> = _roles.asStateFlow()

    private val _invites  = MutableStateFlow<List<GuildInviteResponse>>(emptyList())
    val invites: StateFlow<List<GuildInviteResponse>> = _invites.asStateFlow()

    private val _channels = MutableStateFlow<List<ChannelResponse>>(emptyList())
    val channels: StateFlow<List<ChannelResponse>> = _channels.asStateFlow()

    private val _uiState  = MutableStateFlow(ServerSettingsUiState())
    val uiState: StateFlow<ServerSettingsUiState> = _uiState.asStateFlow()

    fun load(guildId: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loading = true, error = null)
        try {
            api.getGuild(guildId).body()?.let { g ->
                _guild.value    = g
                _channels.value = g.channels ?: emptyList()
                _roles.value    = g.roles    ?: emptyList()
            }
            api.getGuildMembers(guildId).body()?.let { _members.value = it }
            if (_roles.value.isEmpty()) {
                api.getGuildRoles(guildId).body()?.let { _roles.value = it }
            }
            try { api.getGuildInvites(guildId).body()?.let { _invites.value = it } } catch (_: Exception) {}
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Failed to load: ${e.message}")
        } finally {
            _uiState.value = _uiState.value.copy(loading = false)
        }
    }

    fun saveGuildName(guildId: String, name: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        _uiState.value = _uiState.value.copy(saving = true)
        try {
            api.updateGuild(guildId, mapOf("name" to name)).body()?.let {
                _guild.value = it
                _uiState.value = _uiState.value.copy(saving = false, saveSuccess = true)
            }
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(saving = false, error = "Failed to save")
        }
    }

    fun deleteInvite(code: String) = viewModelScope.launch {
        try {
            api.deleteInvite(code)
            _invites.value = _invites.value.filter { it.code != code }
        } catch (_: Exception) {}
    }
}
