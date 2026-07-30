package com.example.jellyfinoffline.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val SMART_SYNC_ENABLED = booleanPreferencesKey("smart_sync_enabled")
        val SYNC_EPISODE_COUNT = intPreferencesKey("sync_episode_count")
        val STREAMING_BITRATE = stringPreferencesKey("streaming_bitrate")
        val SERVER_URL = stringPreferencesKey("server_url")
        val USER_ID = stringPreferencesKey("user_id")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
    }

    val smartSyncEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SMART_SYNC_ENABLED] ?: true
        }

    val syncEpisodeCount: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[SYNC_EPISODE_COUNT] ?: 3
        }

    val streamingBitrate: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[STREAMING_BITRATE] ?: "Original / Direct Play"
        }

    val serverUrl: Flow<String?> = context.dataStore.data.map { it[SERVER_URL] }
    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }

    suspend fun saveSmartSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SMART_SYNC_ENABLED] = enabled
        }
    }

    suspend fun saveSyncEpisodeCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[SYNC_EPISODE_COUNT] = count
        }
    }

    suspend fun saveStreamingBitrate(bitrate: String) {
        context.dataStore.edit { preferences ->
            preferences[STREAMING_BITRATE] = bitrate
        }
    }

    suspend fun saveAuthData(url: String, userId: String, token: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL] = url
            preferences[USER_ID] = userId
            preferences[ACCESS_TOKEN] = token
        }
    }

    suspend fun clearAuthData() {
        context.dataStore.edit { preferences ->
            preferences.remove(SERVER_URL)
            preferences.remove(USER_ID)
            preferences.remove(ACCESS_TOKEN)
        }
    }
}
