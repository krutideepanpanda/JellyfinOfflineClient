package com.example.jellyfinoffline.sync

import android.content.Context
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.jellyfinoffline.JellyfinClientManager
import com.example.jellyfinoffline.data.AppDatabase
import com.example.jellyfinoffline.data.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class DownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val channelId = "download_channel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Downloads",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Downloading Episode")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()

        return ForegroundInfo(1991, notification)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val episodeId = inputData.getString("EPISODE_ID") ?: return@withContext Result.failure()
        
        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.offlineEpisodeDao()
        
        val episode = dao.getEpisode(episodeId) ?: return@withContext Result.failure()
        
        var outputFile: File? = null
        try {
            dao.insertOrUpdate(episode.copy(status = DownloadStatus.DOWNLOADING))
            
            val api = JellyfinClientManager.getApi()
            val baseUrl = api.baseUrl
            val token = api.accessToken ?: return@withContext Result.failure()
            
            // Construct the download URL
            val downloadUrl = "$baseUrl/Items/$episodeId/Download?api_key=$token"
            
            // Use app-specific external directory to avoid permission issues on Android 13+
            val moviesDir = applicationContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: return@withContext Result.failure()
            val jellyfinDir = File(moviesDir, "JellyfinOffline")
            if (!jellyfinDir.exists()) jellyfinDir.mkdirs()
            
            val targetFile = File(jellyfinDir, "$episodeId.mp4")
            outputFile = targetFile
            
            // Real download with progress reporting
            val connection = URL(downloadUrl).openConnection()
            val totalBytes = connection.contentLengthLong
            
            connection.getInputStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead = input.read(buffer)
                    var totalRead = 0L
                    var lastReportedProgress = 0f
                    
                    while (bytesRead != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        
                        if (totalBytes > 0) {
                            val progress = (totalRead.toFloat() / totalBytes.toFloat()) * 100f
                            if (progress - lastReportedProgress >= 5f) {
                                lastReportedProgress = progress
                                dao.insertOrUpdate(episode.copy(status = DownloadStatus.DOWNLOADING, progressPercentage = progress))
                            }
                        }
                        bytesRead = input.read(buffer)
                    }
                }
            }
            
            dao.insertOrUpdate(
                episode.copy(
                    status = DownloadStatus.COMPLETED,
                    progressPercentage = 100f,
                    downloadPath = targetFile.absolutePath
                )
            )
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Clean up partial incomplete file on failure/retry to prevent storage bloat
            val fileToDelete = outputFile
            if (fileToDelete != null && fileToDelete.exists()) {
                try { fileToDelete.delete() } catch (ex: Exception) { ex.printStackTrace() }
            }
            // Reset to QUEUED if it fails so it can be retried by WorkManager or later
            dao.insertOrUpdate(episode.copy(status = DownloadStatus.QUEUED))
            Result.retry()
        }
    }
}
