package com.example.jellyfinoffline.ui.downloads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jellyfinoffline.data.AppDatabase
import com.example.jellyfinoffline.data.DownloadStatus
import com.example.jellyfinoffline.data.OfflineEpisode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onPlayClick: (showId: String, episodeId: String) -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.offlineEpisodeDao() }
    val episodesFlow = remember { dao.getAllOfflineEpisodes() }
    val episodes by episodesFlow.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Offline Media & Queue", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val completedCount = episodes.count { it.status == DownloadStatus.COMPLETED }
            val downloadingCount = episodes.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }

            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("📦 Offline Library", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("$completedCount downloaded • $downloadingCount active jobs", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (episodes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No offline downloads or queued jobs.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    items(episodes) { ep ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ep.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = if (ep.mediaType == "MOVIE") "Movie" else "Season ${ep.seasonNumber} • Episode ${ep.episodeNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row {
                                        if (ep.status == DownloadStatus.COMPLETED) {
                                            IconButton(onClick = { onPlayClick(ep.showId, ep.episodeId) }) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        ep.downloadPath?.let { path ->
                                                            val f = File(path)
                                                            if (f.exists()) f.delete()
                                                        }
                                                        dao.deleteEpisode(ep.episodeId)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                if (ep.status == DownloadStatus.DOWNLOADING) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Downloading (${ep.progressPercentage.toInt()}%)", style = MaterialTheme.typography.labelSmall)
                                    LinearProgressIndicator(
                                        progress = ep.progressPercentage / 100f,
                                        modifier = Modifier.fillMaxWidth().height(6.dp)
                                    )
                                } else if (ep.status == DownloadStatus.QUEUED) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("⏳ Queued for download...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                } else if (ep.status == DownloadStatus.COMPLETED) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("✅ Ready for offline play", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
