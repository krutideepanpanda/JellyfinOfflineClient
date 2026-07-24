package com.example.jellyfinoffline.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_episodes")
data class OfflineEpisode(
    @PrimaryKey val episodeId: String,
    val showId: String,
    val seasonId: String,
    val episodeNumber: Int,
    val seasonNumber: Int,
    val downloadPath: String?,
    val status: DownloadStatus
)

enum class DownloadStatus {
    QUEUED, DOWNLOADING, COMPLETED
}
