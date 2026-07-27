package com.example.jellyfinoffline.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_episodes")
data class OfflineEpisode(
    @PrimaryKey val episodeId: String,
    val showId: String,
    val seasonId: String,
    val title: String = "Unknown Title",
    val episodeNumber: Int,
    val seasonNumber: Int,
    val downloadPath: String?,
    val status: DownloadStatus,
    val progressPercentage: Float = 0f,
    val mediaType: String = "SERIES" // SERIES or MOVIE
)

enum class DownloadStatus {
    QUEUED, DOWNLOADING, COMPLETED
}
