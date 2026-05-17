package com.nexus.android.ui.screens.voice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.android.data.api.NexusApi
import com.nexus.android.data.api.models.VoiceLeaveRequest
import com.nexus.android.data.api.models.VoiceTokenRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
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
            val r = LiveKit.create(getApplication())
            room = r
            // LiveKit 2.x: connect() is a suspend fun with no roomOptions named param
            r.connect(body.livekitUrl, body.token)
            r.localParticipant.setMicrophoneEnabled(true)
            // Snapshot current remote participants (identity is Participant.Identity value class)
            _participants.value = r.remoteParticipants.values.map { it.identity.value }
            _uiState.value = VoiceUiState(connecting = false)
            // LiveKit 2.x: observe room events via Flow
            launch {
                r.events.collect { event ->
                    when (event) {
                        is RoomEvent.ParticipantConnected -> {
                            _participants.value = r.remoteParticipants.values.map { it.identity.value }
                        }
                        is RoomEvent.ParticipantDisconnected -> {
                            _participants.value = r.remoteParticipants.values.map { it.identity.value }
                        }
                        is RoomEvent.ActiveSpeakersChanged -> {
                            _uiState.value = _uiState.value.copy(
                                speaking = event.speakers.map { it.identity.value }.toSet()
                            )
                        }
                        else -> Unit
                    }
                }
            }
        } catch (e: Exception) { _uiState.value = VoiceUiState(error = "Voice error: ${e.message}") }
    }

    fun leave(channelId: String) {
        viewModelScope.launch { try { api.leaveVoice(VoiceLeaveRequest(channelId)) } catch (_: Exception) {} }
        room?.disconnect(); room = null
        _uiState.value = VoiceUiState(); _participants.value = emptyList()
    }

    fun toggleMute() = viewModelScope.launch {
        val m = !_uiState.value.muted
        // setMicrophoneEnabled is suspend in LiveKit 2.x
        room?.localParticipant?.setMicrophoneEnabled(!m)
        _uiState.value = _uiState.value.copy(muted = m)
    }

    fun toggleDeafen() {
        val d = !_uiState.value.deafened
        // LiveKit 2.x: audioTracks replaced by getTrackPublications() or trackPublications
        room?.remoteParticipants?.values?.forEach { p ->
            p.audioTrackPublications.forEach { pub -> pub.track?.enabled = !d }
        }
        _uiState.value = _uiState.value.copy(deafened = d)
    }

    override fun onCleared() { room?.disconnect(); room = null; super.onCleared() }
}
