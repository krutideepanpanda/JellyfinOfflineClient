package com.example.jellyfinoffline.sync

import android.content.Context
import android.os.Environment
import androidx.work.CoroutineWorker
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

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val episodeId = inputData.getString("EPISODE_ID") ?: return@withContext Result.failure()
        
        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.offlineEpisodeDao()
        
        val episode = dao.getEpisode(episodeId) ?: return@withContext Result.failure()
        
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
            
            val outputFile = File(jellyfinDir, "$episodeId.mp4")
            
            // Real download with progress reporting
            val connection = URL(downloadUrl).openConnection()
            val totalBytes = connection.contentLengthLong
            
            connection.getInputStream().use { input ->
                FileOutputStream(outputFile).use { output ->
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
                    downloadPath = outputFile.absolutePath
                )
            )
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Reset to QUEUED if it fails so it can be retried by WorkManager or later
            dao.insertOrUpdate(episode.copy(status = DownloadStatus.QUEUED))
            Result.retry()
        }
    }
}
