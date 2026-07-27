package com.example.jellyfinoffline.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.jellyfinoffline.JellyfinClientManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.operations.ItemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import java.util.UUID

class HomeViewModel : ViewModel() {
    private val _shows = MutableStateFlow<List<ShowItem>>(emptyList())
    val shows: StateFlow<List<ShowItem>> = _shows
    
    private val _movies = MutableStateFlow<List<ShowItem>>(emptyList())
    val movies: StateFlow<List<ShowItem>> = _movies
    
    init {
        loadMedia()
    }
    
    fun loadMedia() {
        viewModelScope.launch {
            try {
                val api = JellyfinClientManager.getApi()
                val itemsApi = ItemsApi(api)
                val userId = JellyfinClientManager.userId
                val baseUrl = api.baseUrl
                val token = api.accessToken ?: ""
                
                val seriesResult = itemsApi.getItems(
                    userId = userId?.let { UUID.fromString(it) },
                    includeItemTypes = listOf(BaseItemKind.SERIES),
                    recursive = true,
                    fields = listOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO),
                )
                _shows.value = seriesResult.content.items.map { 
                    ShowItem(
                        id = it.id.toString(),
                        name = it.name ?: "Unknown Show",
                        type = "SERIES",
                        imageUrl = "$baseUrl/Items/${it.id}/Images/Primary?maxHeight=300&api_key=$token"
                    ) 
                }

                val movieResult = itemsApi.getItems(
                    userId = userId?.let { UUID.fromString(it) },
                    includeItemTypes = listOf(BaseItemKind.MOVIE),
                    recursive = true,
                    fields = listOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO),
                )
                _movies.value = movieResult.content.items.map { 
                    ShowItem(
                        id = it.id.toString(),
                        name = it.name ?: "Unknown Movie",
                        type = "MOVIE",
                        imageUrl = "$baseUrl/Items/${it.id}/Images/Primary?maxHeight=300&api_key=$token"
                    ) 
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

data class ShowItem(val id: String, val name: String, val type: String = "SERIES", val imageUrl: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onShowClick: (String) -> Unit,
    onMovieClick: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val shows by viewModel.shows.collectAsState()
    val movies by viewModel.movies.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jellyfin Library", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "📺 TV Shows & Anime",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (shows.isEmpty()) {
                    Text("No TV shows found or loading...", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(shows) { show ->
                            MediaCard(item = show, onClick = { onShowClick(show.id) })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text(
                    text = "🎬 Movies",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (movies.isEmpty()) {
                    Text("No movies found or loading...", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(movies) { movie ->
                            MediaCard(item = movie, onClick = { onMovieClick(movie.id) })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun MediaCard(item: ShowItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Box(modifier = Modifier.height(200.dp).fillMaxWidth()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                )
            }
            Text(
                text = item.name,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
