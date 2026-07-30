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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.operations.ItemsApi
import org.jellyfin.sdk.api.operations.UserLibraryApi
import org.jellyfin.sdk.api.operations.UserViewsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.ImageType
import java.util.UUID

data class ShowItem(
    val id: String, 
    val name: String, 
    val type: String = "SERIES", 
    val imageUrl: String? = null,
    val isLandscape: Boolean = false,
    val percentagePlayed: Double? = null
)

class HomeViewModel : ViewModel() {
    private val _resumeItems = MutableStateFlow<List<ShowItem>>(emptyList())
    val resumeItems: StateFlow<List<ShowItem>> = _resumeItems
    
    private val _libraries = MutableStateFlow<List<ShowItem>>(emptyList())
    val libraries: StateFlow<List<ShowItem>> = _libraries
    
    private val _latestItems = MutableStateFlow<List<ShowItem>>(emptyList())
    val latestItems: StateFlow<List<ShowItem>> = _latestItems
    
    init {
        loadMedia()
    }
    
    fun loadMedia() {
        viewModelScope.launch {
            try {
                val api = JellyfinClientManager.getApi()
                val itemsApi = ItemsApi(api)
                val userViewsApi = UserViewsApi(api)
                val userLibraryApi = UserLibraryApi(api)
                
                val userIdStr = JellyfinClientManager.userId
                if (userIdStr == null) return@launch
                val userId = UUID.fromString(userIdStr)
                val baseUrl = api.baseUrl
                val token = api.accessToken ?: ""
                
                withContext(Dispatchers.IO) {
                    // 1. Resume Items (Continue Watching)
                    try {
                        val resumeResult = itemsApi.getResumeItems(
                            userId = userId,
                            limit = 10,
                            fields = listOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO),
                            mediaTypes = listOf(MediaType.VIDEO)
                        )
                        val rItems = resumeResult.content.items.map {
                            val percentage = it.userData?.playedPercentage
                            ShowItem(
                                id = it.id.toString(),
                                name = it.name ?: "Unknown",
                                type = it.type?.toString() ?: "Movie",
                                imageUrl = "$baseUrl/Items/${it.id}/Images/Primary?maxWidth=600&api_key=$token",
                                isLandscape = true,
                                percentagePlayed = percentage
                            )
                        }
                        _resumeItems.value = rItems
                    } catch (e: Exception) { e.printStackTrace() }

                    // 2. User Views (Libraries)
                    try {
                        val viewsResult = userViewsApi.getUserViews(userId = userId)
                        val views = viewsResult.content.items ?: emptyList()
                        val lItems = views.map {
                            ShowItem(
                                id = it.id.toString(),
                                name = it.name ?: "Unknown",
                                type = "LIBRARY",
                                imageUrl = "$baseUrl/Items/${it.id}/Images/Primary?maxWidth=400&api_key=$token",
                                isLandscape = true
                            )
                        }
                        _libraries.value = lItems
                    } catch (e: Exception) { e.printStackTrace() }

                    // 3. Latest Media
                    try {
                        val latestResult = userLibraryApi.getLatestMedia(
                            userId = userId,
                            limit = 20,
                            fields = listOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO),
                            includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE)
                        )
                        val latItems = latestResult.content.map {
                            ShowItem(
                                id = it.id.toString(),
                                name = it.name ?: "Unknown",
                                type = it.type?.toString() ?: "Movie",
                                imageUrl = "$baseUrl/Items/${it.id}/Images/Primary?maxHeight=400&api_key=$token",
                                isLandscape = false
                            )
                        }
                        _latestItems.value = latItems
                    } catch (e: Exception) { e.printStackTrace() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onShowClick: (String) -> Unit,
    onMovieClick: (String) -> Unit = {},
    onLibraryClick: (String, String) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = viewModel(),
) {
    val resumeItems by viewModel.resumeItems.collectAsState()
    val libraries by viewModel.libraries.collectAsState()
    val latestItems by viewModel.latestItems.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jellyfin", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            
            // Continue Watching
            if (resumeItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Continue Watching",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(resumeItems) { item ->
                            MediaCard(item = item, onClick = { 
                                if (item.type.equals("Movie", ignoreCase = true) || item.type.equals("Episode", ignoreCase = true)) {
                                    onMovieClick(item.id)
                                } else {
                                    onShowClick(item.id)
                                }
                            })
                        }
                    }
                }
            }
            
            // My Media (Libraries)
            if (libraries.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "My Media",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(libraries) { item ->
                            MediaCard(item = item, onClick = { 
                                onLibraryClick(item.id, item.name)
                            })
                        }
                    }
                }
            }
            
            // Latest Media
            if (latestItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Latest Media",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(latestItems) { item ->
                            MediaCard(item = item, onClick = { 
                                if (item.type.equals("Movie", ignoreCase = true)) {
                                    onMovieClick(item.id)
                                } else {
                                    onShowClick(item.id)
                                }
                            })
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun MediaCard(item: ShowItem, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    
    val width = if (item.isLandscape) 240.dp else 140.dp
    val height = if (item.isLandscape) 135.dp else 200.dp
    
    Card(
        modifier = Modifier
            .width(width)
            .clickable(onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Box(modifier = Modifier.height(height).fillMaxWidth()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                )
                
                // Progress Bar for Resume Items
                if (item.percentagePlayed != null) {
                    LinearProgressIndicator(
                        progress = { (item.percentagePlayed / 100).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    )
                }
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
