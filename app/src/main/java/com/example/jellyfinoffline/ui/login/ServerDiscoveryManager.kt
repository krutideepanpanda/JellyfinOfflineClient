package com.example.jellyfinoffline.ui.login

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

data class DiscoveredServer(
    val name: String,
    val url: String,
    val id: String
)

object ServerDiscoveryManager {
    private const val DISCOVERY_PORT = 7359
    private const val DISCOVERY_MESSAGE = "who is JellyfinServer?"
    private const val TIMEOUT_MS = 2000

    suspend fun discoverServers(): List<DiscoveredServer> = withContext(Dispatchers.IO) {
        val servers = mutableMapOf<String, DiscoveredServer>()
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket().apply {
                broadcast = true
                soTimeout = TIMEOUT_MS
            }

            val sendData = DISCOVERY_MESSAGE.toByteArray(Charsets.UTF_8)
            val broadcastAddress = InetAddress.getByName("255.255.255.255")
            val sendPacket = DatagramPacket(sendData, sendData.size, broadcastAddress, DISCOVERY_PORT)
            socket.send(sendPacket)

            val receiveBuffer = ByteArray(2048)
            val endTime = System.currentTimeMillis() + TIMEOUT_MS

            while (System.currentTimeMillis() < endTime) {
                try {
                    val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                    socket.receive(receivePacket)
                    
                    val responseJson = String(receivePacket.data, 0, receivePacket.length, Charsets.UTF_8)
                    val jsonObject = JSONObject(responseJson)
                    
                    val name = jsonObject.optString("Name", "Local Jellyfin Server")
                    val address = jsonObject.optString("Address", "")
                    val id = jsonObject.optString("Id", address)
                    
                    if (address.isNotEmpty() && !servers.containsKey(id)) {
                        servers[id] = DiscoveredServer(name = name, url = address, id = id)
                    }
                } catch (e: SocketTimeoutException) {
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket?.close()
        }
        servers.values.toList()
    }
}
