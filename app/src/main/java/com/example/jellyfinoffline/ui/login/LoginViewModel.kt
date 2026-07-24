package com.example.jellyfinoffline.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jellyfinoffline.JellyfinClientManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.operations.UserApi
import org.jellyfin.sdk.model.api.AuthenticateUserByName
import org.jellyfin.sdk.model.api.QuickConnectDto

class LoginViewModel : ViewModel() {
    
    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun updateServerUrl(url: String) { _serverUrl.value = url }
    fun updateUsername(name: String) { _username.value = name }
    fun updatePassword(pwd: String) { _password.value = pwd }

    fun login() {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                // Initialize the API client with the provided URL
                val api = JellyfinClientManager.jellyfin.createApi(
                    baseUrl = _serverUrl.value.ifBlank { "http://localhost:8096" }
                )
                
                val userApi = UserApi(api)
                val authResult = userApi.authenticateUserByName(
                    AuthenticateUserByName(
                        username = _username.value,
                        pw = _password.value
                    )
                )
                
                // Keep the token for future calls
                api.accessToken = authResult.content.accessToken
                api.userId = authResult.content.user?.id
                
                JellyfinClientManager.updateApi(api, authResult.content.user?.id?.toString())
                
                _loginState.value = LoginState.Success(api)
                
            } catch (e: Exception) {
                e.printStackTrace()
                _loginState.value = LoginState.Error(e.message ?: "Authentication failed")
            }
        }
    }
    
    fun quickConnect() {
        // Quick connect implementation requires a multi-step process (get code, wait for auth)
        // Disabling for now to ensure project compiles
        _loginState.value = LoginState.Error("Quick Connect not yet implemented")
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val api: ApiClient) : LoginState()
    data class Error(val message: String) : LoginState()
}
