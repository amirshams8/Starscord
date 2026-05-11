package com.nexus.android.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.android.data.api.NexusApi
import com.nexus.android.data.api.models.ChannelResponse
import com.nexus.android.data.api.models.GuildResponse
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

    init { loadGuilds() }

    private fun loadGuilds() = viewModelScope.launch {
        try { api.getMyGuilds().body()?.let { _guilds.value = it } } catch (_: Exception) {}
    }

    fun selectGuild(guild: GuildResponse) = viewModelScope.launch {
        _selectedGuild.value = guild
        try {
            api.getGuild(guild.id).body()?.let {
                _selectedGuild.value = it
                _channels.value = it.channels ?: emptyList()
            }
        } catch (_: Exception) {}
    }
}
