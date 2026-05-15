package com.nexus.android.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.android.data.api.NexusApi
import com.nexus.android.data.api.models.ChannelResponse
import com.nexus.android.data.api.models.CreateChannelRequest
import com.nexus.android.data.api.models.CreateGuildRequest
import com.nexus.android.data.api.models.GuildResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val createGuildLoading: Boolean = false,
    val createChannelLoading: Boolean = false,
    val inviteCode: String? = null,           // populated after generating invite
    val showCreateGuildDialog: Boolean = false,
    val showJoinGuildDialog: Boolean = false,
    val showCreateChannelDialog: Boolean = false,
    val showInviteDialog: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(private val api: NexusApi) : ViewModel() {

    private val _guilds        = MutableStateFlow<List<GuildResponse>>(emptyList())
    val guilds: StateFlow<List<GuildResponse>> = _guilds.asStateFlow()

    private val _selectedGuild = MutableStateFlow<GuildResponse?>(null)
    val selectedGuild: StateFlow<GuildResponse?> = _selectedGuild.asStateFlow()

    private val _channels      = MutableStateFlow<List<ChannelResponse>>(emptyList())
    val channels: StateFlow<List<ChannelResponse>> = _channels.asStateFlow()

    private val _uiState       = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadGuilds() }

    // ── Dialog visibility ──────────────────────────────────────────────────────

    fun showCreateGuildDialog()  { _uiState.value = _uiState.value.copy(showCreateGuildDialog = true, error = null) }
    fun hideCreateGuildDialog()  { _uiState.value = _uiState.value.copy(showCreateGuildDialog = false) }
    fun showJoinGuildDialog()    { _uiState.value = _uiState.value.copy(showJoinGuildDialog = true, error = null) }
    fun hideJoinGuildDialog()    { _uiState.value = _uiState.value.copy(showJoinGuildDialog = false) }
    fun showCreateChannelDialog(){ _uiState.value = _uiState.value.copy(showCreateChannelDialog = true, error = null) }
    fun hideCreateChannelDialog(){ _uiState.value = _uiState.value.copy(showCreateChannelDialog = false) }
    fun showInviteDialog(code: String) { _uiState.value = _uiState.value.copy(showInviteDialog = true, inviteCode = code) }
    fun hideInviteDialog()       { _uiState.value = _uiState.value.copy(showInviteDialog = false, inviteCode = null) }
    fun clearError()             { _uiState.value = _uiState.value.copy(error = null) }

    // ── Guild loading ──────────────────────────────────────────────────────────

    fun loadGuilds() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loading = true, error = null)
        try {
            api.getMyGuilds().body()?.let { _guilds.value = it }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Failed to load servers")
        } finally {
            _uiState.value = _uiState.value.copy(loading = false)
        }
    }

    fun selectGuild(guild: GuildResponse) = viewModelScope.launch {
        _selectedGuild.value = guild
        _channels.value = emptyList()
        try {
            api.getGuild(guild.id).body()?.let {
                _selectedGuild.value = it
                _channels.value = it.channels ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    // ── Create guild ───────────────────────────────────────────────────────────

    fun createGuild(name: String) = viewModelScope.launch {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Server name cannot be empty")
            return@launch
        }
        _uiState.value = _uiState.value.copy(createGuildLoading = true, error = null)
        try {
            val resp = api.createGuild(CreateGuildRequest(name.trim()))
            if (resp.isSuccessful && resp.body() != null) {
                val newGuild = resp.body()!!
                _guilds.value = _guilds.value + newGuild
                _uiState.value = _uiState.value.copy(createGuildLoading = false, showCreateGuildDialog = false)
                selectGuild(newGuild)
            } else {
                _uiState.value = _uiState.value.copy(createGuildLoading = false, error = "Failed to create server")
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(createGuildLoading = false, error = "Network error: ${e.message}")
        }
    }

    // ── Join guild via invite ──────────────────────────────────────────────────

    fun joinGuildByInvite(code: String) = viewModelScope.launch {
        val cleanCode = code.trim().removePrefix("https://discord.gg/").removePrefix("https://nexus.gg/").removePrefix("nexus.gg/")
        if (cleanCode.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Invalid invite link")
            return@launch
        }
        _uiState.value = _uiState.value.copy(loading = true, error = null)
        try {
            val resp = api.useInvite(cleanCode)
            if (resp.isSuccessful && resp.body() != null) {
                val joined = resp.body()!!
                if (_guilds.value.none { it.id == joined.id }) {
                    _guilds.value = _guilds.value + joined
                }
                _uiState.value = _uiState.value.copy(loading = false, showJoinGuildDialog = false)
                selectGuild(joined)
            } else {
                _uiState.value = _uiState.value.copy(loading = false, error = "Invalid or expired invite")
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(loading = false, error = "Network error: ${e.message}")
        }
    }

    // ── Leave guild ────────────────────────────────────────────────────────────

    fun leaveGuild(guildId: String) = viewModelScope.launch {
        try {
            api.leaveGuild(guildId)
            _guilds.value = _guilds.value.filter { it.id != guildId }
            if (_selectedGuild.value?.id == guildId) {
                _selectedGuild.value = null
                _channels.value = emptyList()
            }
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(error = "Failed to leave server")
        }
    }

    // ── Create channel ─────────────────────────────────────────────────────────

    fun createChannel(name: String, type: String = "text", parentId: String? = null) = viewModelScope.launch {
        val guildId = _selectedGuild.value?.id ?: return@launch
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Channel name cannot be empty")
            return@launch
        }
        _uiState.value = _uiState.value.copy(createChannelLoading = true, error = null)
        try {
            val resp = api.createChannel(guildId, CreateChannelRequest(name.trim().lowercase().replace(" ", "-"), type, parentId))
            if (resp.isSuccessful && resp.body() != null) {
                _channels.value = _channels.value + resp.body()!!
                _uiState.value = _uiState.value.copy(createChannelLoading = false, showCreateChannelDialog = false)
            } else {
                _uiState.value = _uiState.value.copy(createChannelLoading = false, error = "Failed to create channel")
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(createChannelLoading = false, error = "Network error: ${e.message}")
        }
    }

    // ── Generate invite ────────────────────────────────────────────────────────

    fun generateInvite(channelId: String) = viewModelScope.launch {
        try {
            val resp = api.createInvite(channelId)
            if (resp.isSuccessful && resp.body() != null) {
                showInviteDialog(resp.body()!!.code)
            } else {
                _uiState.value = _uiState.value.copy(error = "Failed to generate invite")
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Network error: ${e.message}")
        }
    }
}
