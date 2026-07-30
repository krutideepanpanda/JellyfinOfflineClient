package com.example.jellyfinoffline

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.model.api.AuthenticateUserByName
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EmulationLoginTest {
    @Test
    fun testAuthenticateUserByNameRequestHeadersAndBody() = runBlocking<Unit> {
        val serverSocket = java.net.ServerSocket(0)
        val port = serverSocket.localPort
        
        kotlin.concurrent.thread {
            try {
                val client = serverSocket.accept()
                val reader = java.io.BufferedReader(java.io.InputStreamReader(client.inputStream))
                
                println("===== RAW HTTP REQUEST =====")
                var line: String?
                var contentLength = 0
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.isEmpty()) break
                    println(line)
                    if (line!!.lowercase().startsWith("content-length:")) {
                        contentLength = line!!.split(":")[1].trim().toInt()
                    }
                }
                
                if (contentLength > 0) {
                    val bodyChars = CharArray(contentLength)
                    reader.read(bodyChars)
                    println("Body:")
                    println(String(bodyChars))
                }
                println("============================")
                
                val out = java.io.PrintWriter(client.outputStream)
                out.print("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n{\"User\":{\"Id\":\"test\"},\"AccessToken\":\"test\"}")
                out.flush()
                client.close()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                serverSocket.close()
            }
        }

        val context = ApplicationProvider.getApplicationContext<Application>()
        val jellyfin = createJellyfin {
            this.context = context
            clientInfo = ClientInfo("Jellyfin Offline Client", "1.0.0")
            deviceInfo = DeviceInfo("test-device-id", "Test Device")
        }
        
        val api = jellyfin.createApi(baseUrl = "http://localhost:${port}")

        try {
            api.userApi.authenticateUserByName(
                data = AuthenticateUserByName(
                    username = "testuser",
                    pw = "mysecurepassword"
                )
            )
        } catch (e: Exception) {
            println("Request failed: ${e.message}")
        }
    }
}
