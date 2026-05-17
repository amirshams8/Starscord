package com.nexus.android.ui.screens.voice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.android.data.api.NexusApi
import com.nexus.android.data.api.models.VoiceLeaveRequest
import com.nexus.android.data.api.models.VoiceTokenRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import io.livekit.android.room.RoomListener
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.participant.RemoteParticipant
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VoiceUiState(
    val connecting: Boolean = false,
    val muted: Boolean = false,
    val deafened: Boolean = false,
    val speaking: Set<String> = emptySet(),
    val error: String? = null,
)

@HiltViewModel
class VoiceViewModel @Inject constructor(
    application: Application,
    private val api: NexusApi,
) : AndroidViewModel(application) {
    private val _uiState      = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()
    private val _participants = MutableStateFlow<List<String>>(emptyList())
    val participants: StateFlow<List<String>> = _participants.asStateFlow()
    private var room: Room? = null

    fun join(channelId: String) = viewModelScope.launch {
        _uiState.value = VoiceUiState(connecting = true)
        try {
            val resp = api.getVoiceToken(VoiceTokenRequest(channelId))
            if (!resp.isSuccessful || resp.body() == null) {
                _uiState.value = VoiceUiState(error = "Failed to get voice token"); return@launch
            }
            val body = resp.body()!!
            room = LiveKit.create(getApplication()).also { r ->
                r.connect(body.livekitUrl, body.token,
                    roomOptions = io.livekit.android.room.RoomOptions(adaptiveStream = true, dynacast = true))
                r.localParticipant.setMicrophoneEnabled(true)
                _participants.value = r.remoteParticipants.values.map { it.identity ?: "" }
                _uiState.value = VoiceUiState(connecting = false)
                r.addListener(object : RoomListener {
                    override fun onParticipantConnected(room: Room, participant: RemoteParticipant) {
                        _participants.value = room.remoteParticipants.values.map { it.identity ?: "" }
                    }
                    override fun onParticipantDisconnected(room: Room, participant: RemoteParticipant) {
                        _participants.value = room.remoteParticipants.values.map { it.identity ?: "" }
                    }
                    override fun onActiveSpeakersChanged(speakers: List<Participant>, room: Room) {
                        _uiState.value = _uiState.value.copy(speaking = speakers.mapNotNull { it.identity }.toSet())
                    }
                })
            }
        } catch (e: Exception) { _uiState.value = VoiceUiState(error = "Voice error: ${e.message}") }
    }

    fun leave(channelId: String) {
        viewModelScope.launch { try { api.leaveVoice(VoiceLeaveRequest(channelId)) } catch (_: Exception) {} }
        room?.disconnect(); room = null
        _uiState.value = VoiceUiState(); _participants.value = emptyList()
    }

    fun toggleMute() {
        val m = !_uiState.value.muted
        room?.localParticipant?.setMicrophoneEnabled(!m)
        _uiState.value = _uiState.value.copy(muted = m)
    }

    fun toggleDeafen() {
        val d = !_uiState.value.deafened
        room?.remoteParticipants?.values?.forEach { p ->
            p.audioTracks.values.forEach { pub -> pub.track?.enabled = !d }
        }
        _uiState.value = _uiState.value.copy(deafened = d)
    }

    override fun onCleared() { room?.disconnect(); room = null; super.onCleared() }
}
