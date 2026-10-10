package com.kirill.musiclibrary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kirill.musiclibrary.domain.SearchUseCase
import com.kirill.musiclibrary.domain.model.SearchHit
import com.kirill.musiclibrary.ui.AppViewModel
import com.kirill.musiclibrary.ui.LibraryContent
import com.kirill.musiclibrary.ui.components.CenteredMessage
import com.kirill.musiclibrary.ui.components.TrackRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenTrack: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LibraryContent(viewModel) { library, index ->
            var query by remember { mutableStateOf("") }
            val useCase = remember(library) { SearchUseCase(library, index) }
            val hits = remember(query, library) { useCase.search(query) }
            Column(modifier = Modifier.padding(padding).fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("Artists, albums, tracks, lyrics") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                when {
                    query.isBlank() -> CenteredMessage(
                        title = "Search the library",
                        body = "Type to search across artists, albums, and tracks.",
                    )
                    hits.isEmpty() -> CenteredMessage(title = "No matches", body = "Nothing matches \"$query\".")
                    else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(hits) { hit ->
                            SearchResultRow(hit, onOpenArtist, onOpenAlbum, onOpenTrack)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    hit: SearchHit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenTrack: (String) -> Unit,
) {
    when (hit) {
        is SearchHit.ArtistHit -> ResultRow(
            title = hit.displayTitle,
            subtitle = "Artist · ${hit.subtitle}",
            onClick = { onOpenArtist(hit.artist.id) },
        )
        is SearchHit.AlbumHit -> ResultRow(
            title = hit.displayTitle,
            subtitle = "Album · ${hit.subtitle}",
            onClick = { onOpenAlbum(hit.album.id) },
        )
        is SearchHit.TrackHit -> TrackRow(
            ref = hit.ref,
            onClick = { onOpenTrack(hit.ref.track.id) },
            subtitle = hit.subtitle,
        )
    }
}

@Composable
private fun ResultRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
