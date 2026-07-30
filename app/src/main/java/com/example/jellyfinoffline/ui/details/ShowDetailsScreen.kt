package com.example.jellyfinoffline.ui.details

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.work.OutOfQuotaPolicy
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
import org.jellyfin.sdk.api.operations.ItemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import java.util.UUID

data class SeriesMetadata(
    val name: String,
    val overview: String?,
    val backdropUrl: String?,
    val logoUrl: String?,
    val year: Int?,
    val genres: List<String>,
    val communityRating: Float?
)

data class EpisodeItem(
    val id: String,
    val name: String,
    val overview: String?,
    val runTimeTicks: Long?,
    val season: Int,
    val episode: Int,
    val imageUrl: String?
)

class ShowDetailsViewModel(private val showId: String) : ViewModel() {
    private val _episodes = MutableStateFlow<List<EpisodeItem>>(emptyList())
    val episodes: StateFlow<List<EpisodeItem>> = _episodes

    private val _metadata = MutableStateFlow<SeriesMetadata?>(null)
    val metadata: StateFlow<SeriesMetadata?> = _metadata

    init {
        loadShowData()
    }

    private fun loadShowData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = JellyfinClientManager.getApi()
                val itemsApi = ItemsApi(api)
                val userId = JellyfinClientManager.userId
                val baseUrl = api.baseUrl
                val token = api.accessToken ?: ""
                
                // Fetch Series Metadata
                val seriesResult = itemsApi.getItems(
                    userId = userId?.let { UUID.fromString(it) },
                    ids = listOf(UUID.fromString(showId)),
                    fields = listOf(ItemFields.OVERVIEW, ItemFields.GENRES, ItemFields.ORIGINAL_TITLE)
                )
                
                seriesResult.content.items?.firstOrNull()?.let { item ->
                    val hasBackdrop = (item.backdropImageTags?.size ?: 0) > 0
                    val hasLogo = item.imageTags?.keys?.any { it.toString().equals("Logo", ignoreCase = true) } == true
                    
                    _metadata.value = SeriesMetadata(
                        name = item.name ?: "Unknown Series",
                        overview = item.overview,
                        backdropUrl = if (hasBackdrop) "$baseUrl/Items/${item.id}/Images/Backdrop/0?maxWidth=1920&api_key=$token" else null,
                        logoUrl = if (hasLogo) "$baseUrl/Items/${item.id}/Images/Logo?maxWidth=800&api_key=$token" else null,
                        year = item.productionYear,
                        genres = item.genres?.toList() ?: emptyList(),
                        communityRating = item.communityRating?.toFloat()
                    )
                }

                // Fetch Episodes using recursive getItems (Reliable on 10.8)
                val episodesResult = itemsApi.getItems(
                    userId = userId?.let { UUID.fromString(it) },
                    parentId = UUID.fromString(showId),
                    recursive = true,
                    includeItemTypes = listOf(BaseItemKind.EPISODE),
                    fields = listOf(ItemFields.OVERVIEW)
                )
                
                val items = episodesResult.content.items?.map { 
                    EpisodeItem(
                        id = it.id.toString(),
                        name = it.name ?: "Unknown Episode",
                        overview = it.overview,
                        runTimeTicks = it.runTimeTicks,
                        season = it.parentIndexNumber ?: 1,
                        episode = it.indexNumber ?: 1,
                        imageUrl = "$baseUrl/Items/${it.id}/Images/Primary?maxHeight=300&api_key=$token"
                    )
                }?.sortedWith(compareBy({ it.season }, { it.episode })) ?: emptyList()
                
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
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
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
    val metadata by viewModel.metadata.collectAsState()
    
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
                title = { Text(metadata?.name ?: "Show Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Rich Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    if (metadata?.backdropUrl != null) {
                        AsyncImage(
                            model = metadata!!.backdropUrl,
                            contentDescription = "Backdrop",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
                    }
                    
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.6f),
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )
                    
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .padding(top = 40.dp)
                    ) {
                        if (metadata?.logoUrl != null) {
                            AsyncImage(
                                model = metadata!!.logoUrl,
                                contentDescription = "Logo",
                                modifier = Modifier.height(80.dp).padding(bottom = 12.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                text = metadata?.name ?: "",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            metadata?.year?.let {
                                Text("$it", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            if (!metadata?.genres.isNullOrEmpty()) {
                                Text(metadata!!.genres.take(3).joinToString(" • "), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                            }
                            metadata?.communityRating?.let {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("★ $it", style = MaterialTheme.typography.bodyMedium, color = Color.Yellow)
                            }
                        }
                    }
                }
            }
            
            // Plot Overview
            if (!metadata?.overview.isNullOrBlank()) {
                item {
                    Text(
                        text = metadata!!.overview!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Season Selector
            item {
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
            }

            // Episodes List
            if (filteredEpisodes.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No episodes available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filteredEpisodes) { ep ->
                    EpisodeCard(
                        ep = ep,
                        onClick = { onEpisodeClick(ep.id) },
                        onDownload = { viewModel.downloadEpisode(context, ep) }
                    )
                }
            }
        }
    }
}

@Composable
fun EpisodeCard(ep: EpisodeItem, onClick: () -> Unit, onDownload: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
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
                        text = "${ep.episode}. ${ep.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Season ${ep.season}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (ep.runTimeTicks != null && ep.runTimeTicks > 0) {
                        val minutes = ep.runTimeTicks / 10000 / 1000 / 60
                        Text(
                            text = "$minutes min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.align(Alignment.CenterVertically).padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (!ep.overview.isNullOrBlank()) {
                Text(
                    text = ep.overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 4.dp)
                )
            }
        }
    }
}
