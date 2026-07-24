package com.example.jellyfinoffline.ui.player

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    val smartSyncManager = remember { SmartSyncManager(context) }
    
    LaunchedEffect(episodeId) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val dao = db.offlineEpisodeDao()
            val episode = dao.getEpisode(episodeId)
            
            val mediaItem = if (episode != null && episode.downloadPath != null && File(episode.downloadPath).exists()) {
                MediaItem.fromUri(Uri.fromFile(File(episode.downloadPath)))
            } else {
                val api = JellyfinClientManager.getApi()
                // Simple stream URL construction
                // In a more complex app, you'd use api.mediaInfoApi.getPlaybackInfo to handle transcoding
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
    
    // Progress tracking for Smart Sync
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(5000) // Check every 5 seconds
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
