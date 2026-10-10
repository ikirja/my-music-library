package com.kirill.musiclibrary.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.kirill.musiclibrary.ui.AppViewModel
import com.kirill.musiclibrary.ui.LibraryContent
import com.kirill.musiclibrary.ui.components.TrackRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracksScreen(
    viewModel: AppViewModel,
    onOpenTrack: (String) -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Tracks") }) }) { padding ->
        LibraryContent(viewModel) { _, index ->
            var query by remember { mutableStateOf("") }
            val tracks = remember(query) {
                val q = query.trim().lowercase()
                if (q.isEmpty()) index.allTracks
                else index.allTracks.filter {
                    it.track.title.lowercase().contains(q) ||
                        it.artist.name.lowercase().contains(q) ||
                        it.album.title.lowercase().contains(q)
                }
            }
            Column(modifier = Modifier.padding(padding).fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("Filter tracks") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(tracks, key = { it.track.id }) { ref ->
                        TrackRow(
                            ref = ref,
                            onClick = { onOpenTrack(ref.track.id) },
                            subtitle = "${ref.artist.name} — ${ref.album.title}",
                        )
                    }
                }
            }
        }
    }
}
