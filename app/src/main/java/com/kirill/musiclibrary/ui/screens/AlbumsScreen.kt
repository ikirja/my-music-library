package com.kirill.musiclibrary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kirill.musiclibrary.domain.model.Album
import com.kirill.musiclibrary.domain.model.Artist
import com.kirill.musiclibrary.ui.AppViewModel
import com.kirill.musiclibrary.ui.LibraryContent
import com.kirill.musiclibrary.ui.components.AssetCover

private enum class AlbumSort(val label: String) {
    YEAR("Year"),
    ARTIST("Artist"),
    TITLE("Title"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    viewModel: AppViewModel,
    onOpenAlbum: (String) -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Albums") }) }) { padding ->
        LibraryContent(viewModel) { _, index ->
            var sort by remember { mutableStateOf(AlbumSort.YEAR) }
            val sorted = when (sort) {
                AlbumSort.YEAR -> index.allAlbums.sortedWith(
                    compareBy({ it.second.name.lowercase() }, { it.first.year ?: 0 })
                )
                AlbumSort.ARTIST -> index.allAlbums.sortedWith(
                    compareBy({ it.second.name.lowercase() }, { it.first.year ?: 0 })
                )
                AlbumSort.TITLE -> index.allAlbums.sortedBy { it.first.title.lowercase() }
            }
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AlbumSort.entries.forEach { option ->
                        FilterChip(
                            selected = sort == option,
                            onClick = { sort = option },
                            label = { Text(option.label) },
                        )
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(sorted, key = { it.first.id }) { (album, artist) ->
                        AlbumGridCard(album, artist) { onOpenAlbum(album.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumGridCard(album: Album, artist: Artist, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        ) {
            AssetCover(
                coverAsset = album.coverAsset,
                title = album.title,
                modifier = Modifier.fillMaxSize(),
                targetPx = 384,
                contentDescription = "${album.title} cover",
            )
        }
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = "${artist.name} · ${album.year ?: "—"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
