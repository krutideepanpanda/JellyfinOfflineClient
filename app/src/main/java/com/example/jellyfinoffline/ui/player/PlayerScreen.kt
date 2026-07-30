package com.example.jellyfinoffline.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.util.Rational
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.AudioAttributes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.TrackSelectionDialogBuilder
import android.graphics.Color as AndroidColor
import androidx.media3.common.C
import androidx.compose.material.icons.filled.Audiotrack
import com.example.jellyfinoffline.JellyfinClientManager
import com.example.jellyfinoffline.data.AppDatabase
import com.example.jellyfinoffline.sync.SmartSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.File

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun PlayerScreen(
    showId: String,
    episodeId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val exoPlayer = remember { 
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
            
        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build() 
    }
    val smartSyncManager = remember { SmartSyncManager(context) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f)
    var isPortrait by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        val activity = context.findActivity()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
    
    LaunchedEffect(episodeId) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val dao = db.offlineEpisodeDao()
            val episode = dao.getEpisode(episodeId)
            
            val mediaItem = if (episode != null && episode.downloadPath != null && File(episode.downloadPath).exists()) {
                MediaItem.fromUri(Uri.fromFile(File(episode.downloadPath)))
            } else {
                val api = JellyfinClientManager.getApi()
                val settingsRepo = com.example.jellyfinoffline.ui.settings.SettingsRepository(context)
                val bitrateSetting = settingsRepo.streamingBitrate.first()
                
                val bitrateParam = when (bitrateSetting) {
                    "1080p - 10 Mbps" -> "&maxStreamingBitrate=10000000&videoBitRate=10000000"
                    "720p - 4 Mbps" -> "&maxStreamingBitrate=4000000&videoBitRate=4000000"
                    "480p - 1.5 Mbps" -> "&maxStreamingBitrate=1500000&videoBitRate=1500000"
                    "360p - 720 kbps" -> "&maxStreamingBitrate=720000&videoBitRate=720000"
                    else -> "&static=true"
                }
                
                val streamUrl = "${api.baseUrl}/Videos/$episodeId/stream.mp4?api_key=${api.accessToken}$bitrateParam"
                MediaItem.fromUri(Uri.parse(streamUrl))
            }
            
            withContext(Dispatchers.Main) {
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        }
    }
    
    val haptics = LocalHapticFeedback.current

    // Progress tracking for Smart Sync and local SQLite database
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(5000)
            val duration = exoPlayer.duration
            val position = exoPlayer.currentPosition
            if (exoPlayer.isPlaying && duration > 0) {
                val progress = (position.toFloat() / duration.toFloat()) * 100f
                smartSyncManager.onEpisodeProgress(episodeId, showId, progress)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            val activity = context.findActivity()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
                    keepScreenOn = true
                    subtitleView?.apply {
                        setStyle(
                            CaptionStyleCompat(
                                AndroidColor.WHITE,
                                AndroidColor.TRANSPARENT,
                                AndroidColor.TRANSPARENT,
                                CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                                AndroidColor.BLACK,
                                null
                            )
                        )
                        setApplyEmbeddedStyles(true)
                        setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * 1.2f)
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Custom Floating Controls Overlay
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                    exoPlayer.seekTo(newPos)
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Default.FastRewind, contentDescription = "-10s", tint = Color.White)
            }

            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                    exoPlayer.seekTo(newPos)
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Default.FastForward, contentDescription = "+10s", tint = Color.White)
            }

            AssistChip(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val nextIdx = (speeds.indexOf(playbackSpeed) + 1) % speeds.size
                    playbackSpeed = speeds[nextIdx]
                    exoPlayer.setPlaybackSpeed(playbackSpeed)
                },
                label = { Text("${playbackSpeed}x", color = Color.White) },
                leadingIcon = { Icon(Icons.Default.Speed, contentDescription = "Speed", tint = Color.White) },
                colors = AssistChipDefaults.assistChipColors(containerColor = Color.Black.copy(alpha = 0.6f))
            )

            AssistChip(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val activity = context.findActivity()
                    if (activity != null) {
                        TrackSelectionDialogBuilder(activity, "Audio Tracks", exoPlayer, C.TRACK_TYPE_AUDIO)
                            .build()
                            .show()
                    }
                },
                label = { Text("Audio", color = Color.White) },
                leadingIcon = { Icon(Icons.Default.Audiotrack, contentDescription = "Audio Tracks", tint = Color.White) },
                colors = AssistChipDefaults.assistChipColors(containerColor = Color.Black.copy(alpha = 0.6f))
            )

            AssistChip(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val activity = context.findActivity()
                    if (activity != null) {
                        TrackSelectionDialogBuilder(activity, "Subtitles", exoPlayer, C.TRACK_TYPE_TEXT)
                            .build()
                            .show()
                    }
                },
                label = { Text("Subs", color = Color.White) },
                colors = AssistChipDefaults.assistChipColors(containerColor = Color.Black.copy(alpha = 0.6f))
            )

            AssistChip(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    val activity = context.findActivity()
                    isPortrait = !isPortrait
                    activity?.requestedOrientation = if (isPortrait) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                },
                label = { Text(if (isPortrait) "Portrait" else "Landscape", color = Color.White) },
                leadingIcon = { Icon(Icons.Default.ScreenRotation, contentDescription = "Rotate", tint = Color.White) },
                colors = AssistChipDefaults.assistChipColors(containerColor = Color.Black.copy(alpha = 0.6f))
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AssistChip(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        val activity = context.findActivity()
                        activity?.enterPictureInPictureMode(
                            PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                                .build()
                        )
                    },
                    label = { Text("PiP", color = Color.White) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Color.Black.copy(alpha = 0.6f))
                )
            }
        }
    }
}
