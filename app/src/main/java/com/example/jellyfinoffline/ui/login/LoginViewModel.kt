package com.example.jellyfinoffline.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jellyfinoffline.JellyfinClientManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.api.client.extensions.quickConnectApi
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.AuthenticateUserByName
import org.jellyfin.sdk.model.api.QuickConnectDto

class LoginViewModel : ViewModel() {
    
    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _discoveredServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    val discoveredServers: StateFlow<List<DiscoveredServer>> = _discoveredServers

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    init {
        discoverServers()
    }

    fun discoverServers() {
        viewModelScope.launch {
            val servers = ServerDiscoveryManager.discoverServers()
            _discoveredServers.value = servers
            if (servers.isNotEmpty() && _serverUrl.value.isBlank()) {
                _serverUrl.value = servers.first().url
            }
        }
    }

    fun updateServerUrl(url: String) { _serverUrl.value = url }
    fun updateUsername(name: String) { _username.value = name }
    fun updatePassword(pwd: String) { _password.value = pwd }

    fun login() {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val api = JellyfinClientManager.jellyfin.createApi(
                    baseUrl = _serverUrl.value.ifBlank { "http://localhost:8096" },
                )
                
                val authResult = api.userApi.authenticateUserByName(
                    data = AuthenticateUserByName(
                        username = _username.value,
                        pw = _password.value,
                    ),
                )
                
                val apiWithToken = JellyfinClientManager.jellyfin.createApi(
                    baseUrl = _serverUrl.value.ifBlank { "http://localhost:8096" },
                    accessToken = authResult.content.accessToken
                )
                
                JellyfinClientManager.updateApi(apiWithToken, authResult.content.user?.id?.toString())
                _loginState.value = LoginState.Success(api)
                
            } catch (e: Exception) {
                e.printStackTrace()
                _loginState.value = LoginState.Error(e.message ?: "Authentication failed")
            }
        }
    }
    
    fun quickConnect() {
        if (_serverUrl.value.isBlank()) {
            _loginState.value = LoginState.Error("Please enter or select a Server URL first")
            return
        }
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val api = JellyfinClientManager.jellyfin.createApi(
                    baseUrl = _serverUrl.value,
                )
                val qcResult = api.quickConnectApi.initiateQuickConnect()
                val code = qcResult.content.code ?: "482-910"
                val secret = qcResult.content.secret ?: ""
                
                _loginState.value = LoginState.QuickConnectWaiting(code, secret)
                startQuickConnectPolling(api, secret)
            } catch (e: Exception) {
                e.printStackTrace()
                _loginState.value = LoginState.Error(e.message ?: "Quick Connect failed to initiate")
            }
        }
    }

    private fun startQuickConnectPolling(api: ApiClient, secret: String) {
        viewModelScope.launch {
            try {
                var authenticated = false
                while (!authenticated && _loginState.value is LoginState.QuickConnectWaiting) {
                    delay(3000)
                    val stateResult = api.quickConnectApi.getQuickConnectState(secret = secret)
                    if (stateResult.content.authenticated == true) {
                        authenticated = true
                        val authResult = api.userApi.authenticateWithQuickConnect(
                            data = QuickConnectDto(secret = secret)
                        )
                        val apiWithToken = JellyfinClientManager.jellyfin.createApi(
                            baseUrl = _serverUrl.value,
                            accessToken = authResult.content.accessToken
                        )
                        JellyfinClientManager.updateApi(apiWithToken, authResult.content.user?.id?.toString())
                        _loginState.value = LoginState.Success(api)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cancelQuickConnect() {
        _loginState.value = LoginState.Idle
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class QuickConnectWaiting(val code: String, val secret: String) : LoginState()
    data class Success(val api: ApiClient) : LoginState()
    data class Error(val message: String) : LoginState()
}
