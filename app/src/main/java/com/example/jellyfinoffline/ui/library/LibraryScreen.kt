package com.example.jellyfinoffline.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.jellyfinoffline.ui.home.ShowItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.operations.ItemsApi
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import java.util.UUID

class LibraryViewModel : ViewModel() {
    private val _items = MutableStateFlow<List<ShowItem>>(emptyList())
    val items: StateFlow<List<ShowItem>> = _items
    
    fun loadLibrary(libraryId: String) {
        viewModelScope.launch {
            try {
                val api = JellyfinClientManager.getApi()
                val itemsApi = ItemsApi(api)
                
                val userIdStr = JellyfinClientManager.userId
                if (userIdStr == null) return@launch
                val userId = UUID.fromString(userIdStr)
                val baseUrl = api.baseUrl
                val token = api.accessToken ?: ""
                
                withContext(Dispatchers.IO) {
                    try {
                        val result = itemsApi.getItems(
                            userId = userId,
                            parentId = UUID.fromString(libraryId),
                            sortBy = listOf(ItemSortBy.DATE_CREATED),
                            sortOrder = listOf(SortOrder.DESCENDING),
                            fields = listOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO)
                        )
                        
                        val parsedItems = result.content.items.map {
                            ShowItem(
                                id = it.id.toString(),
                                name = it.name ?: "Unknown",
                                type = it.type?.toString() ?: "Movie",
                                imageUrl = "$baseUrl/Items/${it.id}/Images/Primary?maxHeight=400&api_key=$token",
                                isLandscape = false
                            )
                        }
                        _items.value = parsedItems
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
fun LibraryScreen(
    libraryId: String,
    libraryName: String,
    onShowClick: (String) -> Unit,
    onMovieClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val items by viewModel.items.collectAsState()
    val haptics = LocalHapticFeedback.current
    
    LaunchedEffect(libraryId) {
        viewModel.loadLibrary(libraryId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(libraryName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            items(items) { item ->
                LibraryCard(item = item, onClick = {
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

@Composable
fun LibraryCard(item: ShowItem, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Box(modifier = Modifier.height(180.dp).fillMaxWidth()) {
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
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
