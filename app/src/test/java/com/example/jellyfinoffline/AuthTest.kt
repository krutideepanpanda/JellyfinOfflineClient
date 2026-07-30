package com.example.jellyfinoffline
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class AuthTest {
    @Test
    fun testAuth() {
        val cleanUrl = "https://demo.jellyfin.org/stable"
        val authHeader = "MediaBrowser Client=\"TestClient\", Device=\"TestDevice\", DeviceId=\"12345\", Version=\"1.0.0\""
        
        val url = URL("$cleanUrl/Users/AuthenticateByName")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("X-Emby-Authorization", authHeader)
        conn.setRequestProperty("Authorization", authHeader)
        conn.doOutput = true
        
        val jsonInputString = "{\"Username\":\"demo\", \"Pw\":\"wrongpass\"}"
        conn.outputStream.use { os ->
            val input = jsonInputString.toByteArray(Charsets.UTF_8)
            os.write(input, 0, input.size)
        }
        
        println("Response Code: ${conn.responseCode}")
        if (conn.responseCode !in 200..299) {
            val errorStream = conn.errorStream ?: conn.inputStream
            val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            println("Error Response: $errorResponse")
        } else {
            val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
            println("Success: $responseBody")
        }
    }
}
