package com.example.jellyfinoffline.ui.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jellyfinoffline.JellyfinClientManager
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
                
                val result = tvShowsApi.getEpisodes(
                    seriesId = UUID.fromString(showId),
                    userId = userId?.let { UUID.fromString(it) }
                )
                val items = result.content.items?.map { 
                    EpisodeItem(id = it.id.toString(), name = it.name ?: "Unknown", season = it.parentIndexNumber ?: 1, episode = it.indexNumber ?: 1)
                } ?: emptyList()
                _episodes.value = items
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class ShowDetailsViewModelFactory(private val showId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ShowDetailsViewModel(showId) as T
    }
}

data class EpisodeItem(val id: String, val name: String, val season: Int, val episode: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDetailsScreen(
    showId: String,
    onEpisodeClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: ShowDetailsViewModel = viewModel(factory = ShowDetailsViewModelFactory(showId))
    val episodes by viewModel.episodes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Episodes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            items(episodes) { ep ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { onEpisodeClick(ep.id) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = ep.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "S${ep.season} E${ep.episode}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
