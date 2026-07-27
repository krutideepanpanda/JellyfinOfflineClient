package com.example.jellyfinoffline.ui.details

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import com.example.jellyfinoffline.JellyfinClientManager
import com.example.jellyfinoffline.data.AppDatabase
import com.example.jellyfinoffline.data.DownloadStatus
import com.example.jellyfinoffline.data.OfflineEpisode
import com.example.jellyfinoffline.sync.DownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.operations.TvShowsApi
import java.util.UUID

class ShowDetailsViewModel(private val showId: String) : ViewModel() {
    private val _episodes = MutableStateFlow<List<EpisodeItem>>(emptyList())
    val episodes: StateFlow<List<EpisodeItem>> = _episodes

    init {
        loadEpisodes()
    }

    private fun loadEpisodes() {
        viewModelScope.launch {
            try {
                val api = JellyfinClientManager.getApi()
                val tvShowsApi = TvShowsApi(api)
                val userId = JellyfinClientManager.userId
                val baseUrl = api.baseUrl
                val token = api.accessToken ?: ""
                
                val result = tvShowsApi.getEpisodes(
                    seriesId = UUID.fromString(showId),
                    userId = userId?.let { UUID.fromString(it) },
                )
                val items = result.content.items.map { 
                    EpisodeItem(
                        id = it.id.toString(),
                        name = it.name ?: "Unknown Episode",
                        season = it.parentIndexNumber ?: 1,
                        episode = it.indexNumber ?: 1,
                        imageUrl = "$baseUrl/Items/${it.id}/Images/Primary?maxHeight=200&api_key=$token"
                    )
                }
                _episodes.value = items
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun downloadEpisode(context: Context, ep: EpisodeItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val dao = db.offlineEpisodeDao()
            
            val newEpisode = OfflineEpisode(
                episodeId = ep.id,
                showId = showId,
                seasonId = "",
                title = ep.name,
                episodeNumber = ep.episode,
                seasonNumber = ep.season,
                downloadPath = null,
                status = DownloadStatus.QUEUED,
                mediaType = "SERIES"
            )
            dao.insertOrUpdate(newEpisode)
            
            val inputData = Data.Builder()
                .putString("EPISODE_ID", ep.id)
                .build()
                
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
                
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .build()
                
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}

class ShowDetailsViewModelFactory(private val showId: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ShowDetailsViewModel(showId) as T
    }
}

data class EpisodeItem(val id: String, val name: String, val season: Int, val episode: Int, val imageUrl: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDetailsScreen(
    showId: String,
    onEpisodeClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: ShowDetailsViewModel = viewModel(factory = ShowDetailsViewModelFactory(showId))
    val episodes by viewModel.episodes.collectAsState()
    var selectedSeason by remember { mutableIntStateOf(0) } // 0 means All

    val seasons = remember(episodes) {
        listOf(0) + episodes.map { it.season }.distinct().sorted()
    }
    val filteredEpisodes = remember(episodes, selectedSeason) {
        if (selectedSeason == 0) episodes else episodes.filter { it.season == selectedSeason }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Show Episodes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Season Selector Tabs
            if (seasons.size > 2) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(seasons) { s ->
                        FilterChip(
                            selected = selectedSeason == s,
                            onClick = { selectedSeason = s },
                            label = { Text(if (s == 0) "All Seasons" else "Season $s") }
                        )
                    }
                }
            }

            if (filteredEpisodes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No episodes available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredEpisodes) { ep ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEpisodeClick(ep.id) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(70.dp)
                                        .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                ) {
                                    AsyncImage(
                                        model = ep.imageUrl,
                                        contentDescription = ep.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = ep.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Season ${ep.season} • Episode ${ep.episode}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.downloadEpisode(context, ep) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download Offline",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
