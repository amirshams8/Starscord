package com.nexus.android.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.android.data.api.NexusApi
import com.nexus.android.data.api.models.UpdateUserRequest
import com.nexus.android.data.api.models.UserResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ProfileField { Username, Bio, Pronouns, CustomStatus }

data class ProfileUiState(
    val editing: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val username: String = "",
    val bio: String = "",
    val pronouns: String = "",
    val customStatus: String = "",
)

@HiltViewModel
class ProfileViewModel @Inject constructor(private val api: NexusApi) : ViewModel() {

    private val _user    = MutableStateFlow<UserResponse?>(null)
    val user: StateFlow<UserResponse?> = _user.asStateFlow()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init { loadMe() }

    private fun loadMe() = viewModelScope.launch {
        try {
            api.getMe().body()?.let { u ->
                _user.value = u
                _uiState.value = _uiState.value.copy(
                    username     = u.username,
                    bio          = u.bio ?: "",
                    pronouns     = u.pronouns ?: "",
                    customStatus = u.customStatus ?: "",
                )
            }
        } catch (_: Exception) {}
    }

    fun toggleEdit() {
        _uiState.value = _uiState.value.copy(editing = !_uiState.value.editing, error = null)
        if (!_uiState.value.editing) loadMe() // reload on cancel
    }

    fun onFieldChange(field: ProfileField, value: String) {
        _uiState.value = when (field) {
            ProfileField.Username     -> _uiState.value.copy(username = value)
            ProfileField.Bio          -> _uiState.value.copy(bio = value)
            ProfileField.Pronouns     -> _uiState.value.copy(pronouns = value)
            ProfileField.CustomStatus -> _uiState.value.copy(customStatus = value)
        }
    }

    fun saveProfile() = viewModelScope.launch {
        val s = _uiState.value
        if (s.username.isBlank()) {
            _uiState.value = s.copy(error = "Username cannot be empty"); return@launch
        }
        _uiState.value = s.copy(saving = true, error = null)
        try {
            val resp = api.updateMe(UpdateUserRequest(
                username     = s.username.trim(),
                bio          = s.bio.trim().ifBlank { null },
                pronouns     = s.pronouns.trim().ifBlank { null },
                customStatus = s.customStatus.trim().ifBlank { null },
            ))
            if (resp.isSuccessful && resp.body() != null) {
                _user.value = resp.body()
                _uiState.value = _uiState.value.copy(saving = false, editing = false)
            } else {
                _uiState.value = _uiState.value.copy(saving = false, error = "Failed to save. Username may be taken.")
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(saving = false, error = "Network error: ${e.message}")
        }
    }
}
