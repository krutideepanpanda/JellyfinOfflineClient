package com.example.jellyfinoffline.ui.player

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.jellyfinoffline.JellyfinClientManager
import com.example.jellyfinoffline.data.AppDatabase
import com.example.jellyfinoffline.sync.SmartSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PlayerScreen(
    showId: String,
    episodeId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    val smartSyncManager = remember { SmartSyncManager(context) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f)
    
    LaunchedEffect(episodeId) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val dao = db.offlineEpisodeDao()
            val episode = dao.getEpisode(episodeId)
            
            val mediaItem = if (episode != null && episode.downloadPath != null && File(episode.downloadPath).exists()) {
                MediaItem.fromUri(Uri.fromFile(File(episode.downloadPath)))
            } else {
                val api = JellyfinClientManager.getApi()
                val streamUrl = "${api.baseUrl}/Videos/$episodeId/stream.mp4?api_key=${api.accessToken}"
                MediaItem.fromUri(Uri.parse(streamUrl))
            }
            
            withContext(Dispatchers.Main) {
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        }
    }
    
    // Progress tracking for Smart Sync and local SQLite database
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(5000)
            val duration = exoPlayer.duration
            val position = exoPlayer.currentPosition
            if (duration > 0) {
                val progress = (position.toFloat() / duration.toFloat()) * 100f
                smartSyncManager.onEpisodeProgress(episodeId, showId, progress)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Custom Floating Controls Overlay
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                    exoPlayer.seekTo(newPos)
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Default.FastRewind, contentDescription = "-10s", tint = Color.White)
            }

            IconButton(
                onClick = {
                    val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                    exoPlayer.seekTo(newPos)
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Default.FastForward, contentDescription = "+10s", tint = Color.White)
            }

            AssistChip(
                onClick = {
                    val nextIdx = (speeds.indexOf(playbackSpeed) + 1) % speeds.size
                    playbackSpeed = speeds[nextIdx]
                    exoPlayer.setPlaybackSpeed(playbackSpeed)
                },
                label = { Text("${playbackSpeed}x", color = Color.White) },
                leadingIcon = { Icon(Icons.Default.Speed, contentDescription = "Speed", tint = Color.White) },
                colors = AssistChipDefaults.assistChipColors(containerColor = Color.Black.copy(alpha = 0.6f))
            )
        }
    }
}
