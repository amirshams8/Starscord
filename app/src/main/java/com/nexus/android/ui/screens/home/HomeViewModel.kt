package com.nexus.android.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.android.data.api.NexusApi
import com.nexus.android.data.api.models.ChannelResponse
import com.nexus.android.data.api.models.CreateGuildRequest
import com.nexus.android.data.api.models.GuildResponse
import com.nexus.android.data.api.models.UserResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val api: NexusApi) : ViewModel() {

    private val _guilds        = MutableStateFlow<List<GuildResponse>>(emptyList())
    val guilds: StateFlow<List<GuildResponse>> = _guilds.asStateFlow()

    private val _selectedGuild = MutableStateFlow<GuildResponse?>(null)
    val selectedGuild: StateFlow<GuildResponse?> = _selectedGuild.asStateFlow()

    private val _channels      = MutableStateFlow<List<ChannelResponse>>(emptyList())
    val channels: StateFlow<List<ChannelResponse>> = _channels.asStateFlow()

    private val _dmChannels    = MutableStateFlow<List<ChannelResponse>>(emptyList())
    val dmChannels: StateFlow<List<ChannelResponse>> = _dmChannels.asStateFlow()

    private val _currentUser   = MutableStateFlow<UserResponse?>(null)
    val currentUser: StateFlow<UserResponse?> = _currentUser.asStateFlow()

    private val _showDms       = MutableStateFlow(false)
    val showDms: StateFlow<Boolean> = _showDms.asStateFlow()

    init {
        loadMe()
        loadGuilds()
        loadDmChannels()
    }

    private fun loadMe() = viewModelScope.launch {
        try { api.getMe().body()?.let { _currentUser.value = it } } catch (_: Exception) {}
    }

    private fun loadGuilds() = viewModelScope.launch {
        try { api.getMyGuilds().body()?.let { _guilds.value = it } } catch (_: Exception) {}
    }

    private fun loadDmChannels() = viewModelScope.launch {
        try { api.getDmChannels().body()?.let { _dmChannels.value = it } } catch (_: Exception) {}
    }

    fun showDmsPanel() {
        _showDms.value     = true
        _selectedGuild.value = null
    }

    fun selectGuild(guild: GuildResponse) = viewModelScope.launch {
        _showDms.value       = false
        _selectedGuild.value = guild
        try {
            api.getGuild(guild.id).body()?.let {
                _selectedGuild.value = it
                _channels.value      = it.channels ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    fun createGuild(name: String) = viewModelScope.launch {
        try {
            api.createGuild(CreateGuildRequest(name)).body()?.let { newGuild ->
                _guilds.value = _guilds.value + newGuild
                selectGuild(newGuild)
            }
        } catch (_: Exception) {}
    }

    fun joinByInvite(codeOrUrl: String) = viewModelScope.launch {
        // Support full URLs like https://nexus.gg/invite/ABC123 or bare codes
        val code = codeOrUrl.trimEnd('/').substringAfterLast('/')
        try {
            api.useInvite(code).body()?.let { guild ->
                _guilds.value = (_guilds.value + guild).distinctBy { it.id }
                selectGuild(guild)
            }
        } catch (_: Exception) {}
    }
}
