package com.example.jellyfinoffline.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineEpisodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(episode: OfflineEpisode)

    @Query("SELECT * FROM offline_episodes WHERE episodeId = :episodeId")
    suspend fun getEpisode(episodeId: String): OfflineEpisode?

    @Query("SELECT * FROM offline_episodes WHERE showId = :showId ORDER BY seasonNumber ASC, episodeNumber ASC")
    fun getEpisodesForShow(showId: String): Flow<List<OfflineEpisode>>

    @Query("SELECT * FROM offline_episodes ORDER BY title ASC")
    fun getAllOfflineEpisodes(): Flow<List<OfflineEpisode>>

    @Query("DELETE FROM offline_episodes WHERE episodeId = :episodeId")
    suspend fun deleteEpisode(episodeId: String)
}
