package com.nexus.android.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.android.data.api.NexusApi
import com.nexus.android.data.api.models.LoginRequest
import com.nexus.android.data.api.models.RegisterRequest
import com.nexus.android.di.AuthInterceptor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false,
    val registerSuccess: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: NexusApi,
    private val authInterceptor: AuthInterceptor,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun onEmailChange(v: String)    { _state.value = _state.value.copy(email    = v, error = null) }
    fun onPasswordChange(v: String) { _state.value = _state.value.copy(password = v, error = null) }
    fun onUsernameChange(v: String) { _state.value = _state.value.copy(username = v, error = null) }

    fun login() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        try {
            val resp = api.login(LoginRequest(_state.value.email, _state.value.password))
            if (resp.isSuccessful && resp.body() != null) {
                // Persist token in EncryptedSharedPreferences — survives restarts
                authInterceptor.accessToken = resp.body()!!.accessToken
                _state.value = _state.value.copy(loading = false, loginSuccess = true)
            } else {
                _state.value = _state.value.copy(loading = false, error = "Invalid email or password")
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(loading = false, error = "Network error: ${e.message}")
        }
    }

    fun register() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        if (_state.value.username.isBlank() || _state.value.email.isBlank() || _state.value.password.length < 8) {
            _state.value = _state.value.copy(loading = false, error = "All fields required. Password min 8 chars.")
            return@launch
        }
        try {
            val resp = api.register(
                RegisterRequest(_state.value.username, _state.value.email, _state.value.password)
            )
            if (resp.isSuccessful) {
                _state.value = _state.value.copy(loading = false, registerSuccess = true)
            } else {
                _state.value = _state.value.copy(loading = false, error = "Username or email already taken")
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(loading = false, error = "Network error: ${e.message}")
        }
    }
}
