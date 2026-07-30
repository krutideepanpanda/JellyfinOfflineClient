package com.example.jellyfinoffline

import android.content.Context
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo

object JellyfinClientManager {
    lateinit var jellyfin: Jellyfin
        private set

    private var activeApi: ApiClient? = null
    var userId: String? = null

    fun init(context: Context) {
        jellyfin = createJellyfin {
            this.context = context
            clientInfo = ClientInfo(
                name = "Jellyfin Offline Client",
                version = "1.0.0",
            )
            val prefs = context.getSharedPreferences("jellyfin_prefs", Context.MODE_PRIVATE)
            var deviceId = prefs.getString("device_id", null)
            if (deviceId == null) {
                deviceId = java.util.UUID.randomUUID().toString()
                prefs.edit().putString("device_id", deviceId).apply()
            }
            deviceInfo = org.jellyfin.sdk.model.DeviceInfo(
                id = deviceId,
                name = android.os.Build.MODEL ?: "Android Device"
            )
        }
    }

    fun updateApi(api: ApiClient, userId: String?) {
        this.activeApi = api
        this.userId = userId
    }

    fun getApi(): ApiClient {
        return activeApi ?: throw IllegalStateException("API Client not initialized. Please login first.")
    }
}
