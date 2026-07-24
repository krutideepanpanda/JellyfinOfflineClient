package com.example.jellyfinoffline.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.jellyfinoffline.JellyfinClientManager
import com.example.jellyfinoffline.data.AppDatabase
import com.example.jellyfinoffline.data.DownloadStatus
import com.example.jellyfinoffline.data.OfflineEpisode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.operations.TvShowsApi
import java.io.File
import java.util.UUID

class SmartSyncManager(private val context: Context) {
    
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.offlineEpisodeDao()
    private val workManager = WorkManager.getInstance(context)
    
    fun onEpisodeProgress(episodeId: String, showId: String, progressPercentage: Float) {
        CoroutineScope(Dispatchers.IO).launch {
            if (progressPercentage >= 90f) {
                // User finished the episode, delete it to save space
                val episode = dao.getEpisode(episodeId)
                if (episode != null && episode.downloadPath != null) {
                    val file = File(episode.downloadPath)
                    if (file.exists()) {
                        file.delete()
                    }
                    dao.deleteEpisode(episodeId)
                }
            }
            
            // Queue next few episodes if not already queued
            queueNextEpisodes(showId, episodeId, count = 3)
        }
    }
    
    private suspend fun queueNextEpisodes(showId: String, currentEpisodeId: String, count: Int) {
        try {
            val api = JellyfinClientManager.getApi()
            val userId = JellyfinClientManager.userId ?: return
            
            val tvShowsApi = TvShowsApi(api)
            
            // Fetch all episodes for the series to find the sequence
            val result = tvShowsApi.getEpisodes(
                seriesId = UUID.fromString(showId),
                userId = UUID.fromString(userId)
            )
            
            val episodes = result.content.items ?: return
            val currentIndex = episodes.indexOfFirst { it.id.toString() == currentEpisodeId }
            if (currentIndex == -1) return
            
            val nextEpisodes = episodes.drop(currentIndex + 1).take(count)
            
            for (ep in nextEpisodes) {
                val epId = ep.id.toString()
                val existing = dao.getEpisode(epId)
                
                if (existing == null) {
                    val newEpisode = OfflineEpisode(
                        episodeId = epId,
                        showId = showId,
                        seasonId = ep.parentId?.toString() ?: "",
                        episodeNumber = ep.indexNumber ?: 0,
                        seasonNumber = ep.parentIndexNumber ?: 0,
                        downloadPath = null,
                        status = DownloadStatus.QUEUED
                    )
                    dao.insertOrUpdate(newEpisode)
                    
                    val inputData = Data.Builder()
                        .putString("EPISODE_ID", epId)
                        .build()
                        
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .build()
                        
                    val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                        .setInputData(inputData)
                        .setConstraints(constraints)
                        .build()
                        
                    workManager.enqueue(request)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
