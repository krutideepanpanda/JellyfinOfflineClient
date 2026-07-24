package com.example.jellyfinoffline.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jellyfinoffline.JellyfinClientManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.operations.ItemsApi
import org.jellyfin.sdk.model.api.ItemFields
import java.util.UUID

class HomeViewModel : ViewModel() {
    private val _shows = MutableStateFlow<List<ShowItem>>(emptyList())
    val shows: StateFlow<List<ShowItem>> = _shows
    
    init {
        loadShows()
    }
    
    private fun loadShows() {
        viewModelScope.launch {
            try {
                val api = JellyfinClientManager.getApi()
                val itemsApi = ItemsApi(api)
                val userId = JellyfinClientManager.userId
                
                val result = itemsApi.getItems(
                    userId = userId?.let { UUID.fromString(it) },
                    includeItemTypes = listOf("Series", "Movie"),
                    recursive = true,
                    fields = listOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO)
                )
                val items = result.content.items?.map { 
                    ShowItem(id = it.id.toString(), name = it.name ?: "Unknown") 
                } ?: emptyList()
                _shows.value = items
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

data class ShowItem(val id: String, val name: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onShowClick: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val shows by viewModel.shows.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Media") })
        }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            items(shows) { show ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { onShowClick(show.id) }
                ) {
                    Text(
                        text = show.name,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
