package com.example.jellyfinoffline

import kotlinx.coroutines.test.runTest
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.jellyfin.sdk.model.api.AuthenticateUserByName
import org.jellyfin.sdk.api.client.extensions.userApi
import org.junit.Test
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LoginApiTest {

    @Test
    fun testLoginWithDemoServer() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val jellyfin = createJellyfin {
            this.context = context
            clientInfo = ClientInfo(
                name = "Jellyfin Offline Client Test",
                version = "1.0.0"
            )
            deviceInfo = org.jellyfin.sdk.model.DeviceInfo(
                id = "test-device-id",
                name = "Robolectric Test Device"
            )
        }
        
        val api = jellyfin.createApi(baseUrl = "https://demo.jellyfin.org/stable")
        
        try {
            println("Attempting login...")
            val result = api.userApi.authenticateUserByName(
                data = AuthenticateUserByName(
                    username = "demo",
                    pw = "" // Demo server has an empty password
                )
            )
            println("Login Success! Result: $result")
        } catch (e: Exception) {
            println("Login Failed: ${e.javaClass.simpleName} - ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
