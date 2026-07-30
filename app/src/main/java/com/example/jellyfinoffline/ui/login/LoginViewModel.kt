package com.example.jellyfinoffline.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jellyfinoffline.JellyfinClientManager
import com.example.jellyfinoffline.ui.settings.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.api.client.extensions.quickConnectApi
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.AuthenticateUserByName
import org.jellyfin.sdk.model.api.QuickConnectDto

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    
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
        checkSavedSession()
        discoverServers()
    }

    private fun checkSavedSession() {
        viewModelScope.launch {
            try {
                val u = repository.serverUrl.first()
                val t = repository.accessToken.first()
                val id = repository.userId.first()
                if (!u.isNullOrBlank() && !t.isNullOrBlank() && !id.isNullOrBlank()) {
                    val api = JellyfinClientManager.jellyfin.createApi(baseUrl = u, accessToken = t)
                    JellyfinClientManager.updateApi(api, id)
                    _serverUrl.value = u
                    _loginState.value = LoginState.Success(api)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
            val cleanUrl = _serverUrl.value.ifBlank { "http://localhost:8096" }.trim().removeSuffix("/")
            try {
                val api = JellyfinClientManager.jellyfin.createApi(
                    baseUrl = cleanUrl,
                )
                
                var authResult: Pair<String, String>? = null
                try {
                    val sdkResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        api.userApi.authenticateUserByName(
                            data = AuthenticateUserByName(
                                username = _username.value.trim(),
                                pw = _password.value,
                            ),
                        )
                    }
                    authResult = Pair(sdkResult.content.accessToken ?: "", sdkResult.content.user?.id?.toString() ?: "")
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback for Jellyfin 10.8.x which requires X-Emby-Authorization header
                    authResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val prefs = this@LoginViewModel.getApplication<Application>().getSharedPreferences("jellyfin_prefs", android.content.Context.MODE_PRIVATE)
                        var deviceId = prefs.getString("device_id", null)
                        if (deviceId == null) {
                            deviceId = java.util.UUID.randomUUID().toString()
                            prefs.edit().putString("device_id", deviceId).apply()
                        }
                        val deviceModel = android.os.Build.MODEL ?: "Android Device"
                        val authHeader = "MediaBrowser Client=\"Jellyfin Offline Client\", Device=\"${deviceModel}\", DeviceId=\"${deviceId}\", Version=\"1.0.0\""
                        
                        val url = java.net.URL("$cleanUrl/Users/AuthenticateByName")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("Accept", "application/json")
                        conn.setRequestProperty("X-Emby-Authorization", authHeader)
                        conn.setRequestProperty("Authorization", authHeader)
                        conn.doOutput = true
                        
                        val jsonObj = org.json.JSONObject()
                        jsonObj.put("Username", _username.value.trim())
                        jsonObj.put("Pw", _password.value)
                        val jsonInputString = jsonObj.toString()
                        
                        conn.outputStream.use { os ->
                            val input = jsonInputString.toByteArray(kotlin.text.Charsets.UTF_8)
                            os.write(input, 0, input.size)
                        }
                        
                        val responseCode = conn.responseCode
                        if (responseCode !in 200..299) {
                            val errorStream = conn.errorStream ?: conn.inputStream
                            val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                            throw Exception("Server Error ($responseCode): $errorResponse")
                        }
                        
                        val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = org.json.JSONObject(responseBody)
                        val accessToken = json.getString("AccessToken")
                        val userId = json.getJSONObject("User").getString("Id")
                        Pair(accessToken, userId)
                    }
                }
                
                val apiWithToken = JellyfinClientManager.jellyfin.createApi(
                    baseUrl = cleanUrl,
                    accessToken = authResult!!.first
                )
                
                val user_id = authResult!!.second
                JellyfinClientManager.updateApi(apiWithToken, user_id)
                repository.saveAuthData(cleanUrl, user_id, authResult!!.first)
                _loginState.value = LoginState.Success(apiWithToken)
                
            } catch (e: Exception) {
                e.printStackTrace()
                _loginState.value = LoginState.Error(e.message ?: "Unknown error")
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
                val cleanUrl = _serverUrl.value.trim().removeSuffix("/")
                val api = JellyfinClientManager.jellyfin.createApi(
                    baseUrl = cleanUrl,
                )
                val qcResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { api.quickConnectApi.initiateQuickConnect() }
                val code = qcResult.content.code ?: ""
                val secret = qcResult.content.secret ?: ""
                
                _loginState.value = LoginState.QuickConnectWaiting(code, secret)
                startQuickConnectPolling(api, secret, cleanUrl)
            } catch (e: Exception) {
                e.printStackTrace()
                _loginState.value = LoginState.Error(e.message ?: "Quick Connect failed to initiate")
            }
        }
    }

    private fun startQuickConnectPolling(api: ApiClient, secret: String, cleanUrl: String) {
        viewModelScope.launch {
            try {
                var authenticated = false
                while (!authenticated && _loginState.value is LoginState.QuickConnectWaiting) {
                    delay(3000)
                    val stateResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { api.quickConnectApi.getQuickConnectState(secret = secret) }
                    if (stateResult.content.authenticated == true) {
                        authenticated = true
                        val authResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            api.userApi.authenticateWithQuickConnect(
                                data = QuickConnectDto(secret = secret)
                            )
                        }
                        val apiWithToken = JellyfinClientManager.jellyfin.createApi(
                            baseUrl = cleanUrl,
                            accessToken = authResult.content.accessToken
                        )
                        val user_id = authResult.content.user?.id?.toString() ?: ""
                        JellyfinClientManager.updateApi(apiWithToken, user_id)
                        repository.saveAuthData(cleanUrl, user_id, authResult.content.accessToken ?: "")
                        _loginState.value = LoginState.Success(apiWithToken)
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

