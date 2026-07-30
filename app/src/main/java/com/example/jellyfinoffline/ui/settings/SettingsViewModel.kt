package com.example.jellyfinoffline.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    val smartSyncEnabled: StateFlow<Boolean> = repository.smartSyncEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val syncEpisodeCount: StateFlow<Int> = repository.syncEpisodeCount
        .stateIn(viewModelScope, SharingStarted.Eagerly, 3)

    val streamingBitrate: StateFlow<String> = repository.streamingBitrate
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Original / Direct Play")

    fun updateSmartSync(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSmartSyncEnabled(enabled)
        }
    }

    fun updateSyncCount(count: Int) {
        viewModelScope.launch {
            repository.saveSyncEpisodeCount(count)
        }
    }

    fun updateStreamingBitrate(bitrate: String) {
        viewModelScope.launch {
            repository.saveStreamingBitrate(bitrate)
        }
    }

    fun clearAuthData() {
        viewModelScope.launch {
            repository.clearAuthData()
        }
    }
}
