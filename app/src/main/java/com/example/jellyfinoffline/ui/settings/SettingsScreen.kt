package com.example.jellyfinoffline.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jellyfinoffline.JellyfinClientManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit
) {
    var smartSyncEnabled by remember { mutableStateOf(true) }
    var syncEpisodeCount by remember { mutableIntStateOf(3) }
    var streamingBitrate by remember { mutableStateOf("Original / Direct Play") }

    val bitrates = listOf("Original / Direct Play", "1080p (10 Mbps)", "720p (4 Mbps)", "480p (1.5 Mbps)")
    val episodeCounts = listOf(1, 3, 5)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings & Preferences", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Server Info Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🌐 Server & Session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Server URL: ${try { JellyfinClientManager.getApi().baseUrl } catch (e: Exception) { "Not connected" }}", style = MaterialTheme.typography.bodyMedium)
                    Text("User ID: ${JellyfinClientManager.userId ?: "Not logged in"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onLogout, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Logout & Switch Server")
                    }
                }
            }

            // Smart Sync Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("⚡ Smart Offline Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Auto-download upcoming episodes in background", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = smartSyncEnabled, onCheckedChange = { smartSyncEnabled = it })
                    }

                    if (smartSyncEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Episodes to queue ahead:", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            episodeCounts.forEach { count ->
                                FilterChip(
                                    selected = syncEpisodeCount == count,
                                    onClick = { syncEpisodeCount = count },
                                    label = { Text("$count episodes") }
                                )
                            }
                        }
                    }
                }
            }

            // Streaming Quality Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🎬 Streaming Quality / Bitrate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Maximum video bitrate when playing over network", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    bitrates.forEach { bitrate ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (bitrate == streamingBitrate),
                                    onClick = { streamingBitrate = bitrate }
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (bitrate == streamingBitrate),
                                onClick = { streamingBitrate = bitrate }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(bitrate, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
