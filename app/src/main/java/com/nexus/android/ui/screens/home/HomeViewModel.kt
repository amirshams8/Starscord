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
import org.json.JSONObject
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val createGuildLoading: Boolean = false,
    val createChannelLoading: Boolean = false,
    val inviteCode: String? = null,
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

    fun showCreateGuildDialog()   { _uiState.value = _uiState.value.copy(showCreateGuildDialog = true,    error = null) }
    fun hideCreateGuildDialog()   { _uiState.value = _uiState.value.copy(showCreateGuildDialog = false,   error = null) }
    fun showJoinGuildDialog()     { _uiState.value = _uiState.value.copy(showJoinGuildDialog = true,      error = null) }
    fun hideJoinGuildDialog()     { _uiState.value = _uiState.value.copy(showJoinGuildDialog = false,     error = null) }
    fun showCreateChannelDialog() { _uiState.value = _uiState.value.copy(showCreateChannelDialog = true,  error = null) }
    fun hideCreateChannelDialog() { _uiState.value = _uiState.value.copy(showCreateChannelDialog = false, error = null) }
    fun showInviteDialog(code: String) { _uiState.value = _uiState.value.copy(showInviteDialog = true, inviteCode = code) }
    fun hideInviteDialog()        { _uiState.value = _uiState.value.copy(showInviteDialog = false, inviteCode = null) }
    fun clearError()              { _uiState.value = _uiState.value.copy(error = null) }

    // ── Guild loading ──────────────────────────────────────────────────────────

    fun loadGuilds() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loading = true, error = null)
        try {
            val guilds = api.getMyGuilds().body() ?: emptyList()
            _guilds.value = guilds
            // FIX: auto-select first guild on cold start so channels load without requiring a tap
            if (guilds.isNotEmpty() && _selectedGuild.value == null) {
                selectGuild(guilds.first())
            }
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
        if (name.trim().length < 2) {
            _uiState.value = _uiState.value.copy(error = "Server name must be at least 2 characters")
            return@launch
        }
        _uiState.value = _uiState.value.copy(createGuildLoading = true, error = null)
        try {
            val resp = api.createGuild(CreateGuildRequest(name.trim()))
            if (resp.isSuccessful && resp.body() != null) {
                val newGuild = resp.body()!!
                _guilds.value = _guilds.value + newGuild
                _uiState.value = _uiState.value.copy(createGuildLoading = false, showCreateGuildDialog = false, error = null)
                selectGuild(newGuild)
            } else {
                val serverMsg = try {
                    resp.errorBody()?.string()?.let { JSONObject(it).optString("error", null) }
                } catch (_: Exception) { null }
                _uiState.value = _uiState.value.copy(
                    createGuildLoading = false,
                    error = serverMsg ?: when (resp.code()) {
                        400  -> "Invalid server name"
                        401  -> "Not logged in. Please restart the app."
                        409  -> "A server with that name already exists"
                        500  -> "Server error. Is the backend running?"
                        else -> "Failed to create server (${resp.code()})"
                    }
                )
            }
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("Unable to resolve host") == true ||
                e.message?.contains("Failed to connect") == true ||
                e.message?.contains("Connection refused") == true ->
                    "Cannot reach server. Check your network or backend URL."
                else -> "Network error: ${e.message}"
            }
            _uiState.value = _uiState.value.copy(createGuildLoading = false, error = msg)
        }
    }

    // ── Join guild via invite ──────────────────────────────────────────────────

    fun joinGuildByInvite(code: String) = viewModelScope.launch {
        val cleanCode = code.trim()
            .removePrefix("https://discord.gg/")
            .removePrefix("https://nexus.gg/")
            .removePrefix("nexus.gg/")
        if (cleanCode.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Invalid invite link")
            return@launch
        }
        _uiState.value = _uiState.value.copy(loading = true, error = null)
        try {
            val resp = api.useInvite(cleanCode)
            if (resp.isSuccessful && resp.body() != null) {
                val guild = resp.body()!!
                if (_guilds.value.none { it.id == guild.id }) _guilds.value = _guilds.value + guild
                _uiState.value = _uiState.value.copy(loading = false, showJoinGuildDialog = false, error = null)
                selectGuild(guild)
            } else {
                val serverMsg = try {
                    resp.errorBody()?.string()?.let { JSONObject(it).optString("error", null) }
                } catch (_: Exception) { null }
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error   = serverMsg ?: when (resp.code()) {
                        400 -> "Invalid or expired invite"
                        404 -> "Invite not found"
                        else -> "Failed to join server (${resp.code()})"
                    }
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(loading = false, error = "Network error: ${e.message}")
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
            val resp = api.createChannel(guildId, CreateChannelRequest(name.trim(), type, parentId))
            if (resp.isSuccessful && resp.body() != null) {
                val ch = resp.body()!!
                _channels.value = _channels.value + ch
                _uiState.value = _uiState.value.copy(createChannelLoading = false, showCreateChannelDialog = false, error = null)
            } else {
                val serverMsg = try {
                    resp.errorBody()?.string()?.let { JSONObject(it).optString("error", null) }
                } catch (_: Exception) { null }
                _uiState.value = _uiState.value.copy(
                    createChannelLoading = false,
                    error = serverMsg ?: "Failed to create channel"
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(createChannelLoading = false, error = "Network error: ${e.message}")
        }
    }
}
