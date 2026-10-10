package com.kirill.musiclibrary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kirill.musiclibrary.domain.Format
import com.kirill.musiclibrary.domain.model.Artist
import com.kirill.musiclibrary.ui.AppViewModel
import com.kirill.musiclibrary.ui.LibraryContent
import com.kirill.musiclibrary.ui.components.AssetCover
import com.kirill.musiclibrary.ui.components.StatChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(
    viewModel: AppViewModel,
    onOpenArtist: (String) -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Artists") }) }) { padding ->
        LibraryContent(viewModel) { library, _ ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(library.artists, key = { it.id }) { artist ->
                    ArtistListRow(artist) { onOpenArtist(artist.id) }
                }
            }
        }
    }
}

@Composable
private fun ArtistListRow(artist: Artist, onClick: () -> Unit) {
    val tracks = artist.albums.sumOf { it.trackCount }
    val duration = Format.longDuration(artist.albums.sumOf { it.durationSeconds })
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(artist.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${artist.albums.size} albums",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "$tracks tracks · $duration",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 14.dp),
        )
        HorizontalDivider()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    viewModel: AppViewModel,
    artistId: String,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Artist") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LibraryContent(viewModel) { _, index ->
            val artist = index.artistById[artistId]
            if (artist == null) {
                com.kirill.musiclibrary.ui.components.CenteredMessage(
                    title = "Artist not found",
                    modifier = Modifier.padding(padding),
                )
                return@LibraryContent
            }
            val tracks = artist.albums.sumOf { it.trackCount }
            val duration = Format.longDuration(artist.albums.sumOf { it.durationSeconds })
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item("header") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(artist.name, style = MaterialTheme.typography.headlineSmall)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            StatChip("${artist.albums.size} albums")
                            StatChip("$tracks tracks")
                            StatChip(duration)
                        }
                    }
                }
                items(artist.albums, key = { it.id }) { album ->
                    Card(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth(),
                        onClick = { onOpenAlbum(album.id) },
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AssetCover(
                                coverAsset = album.coverAsset,
                                title = album.title,
                                modifier = Modifier.size(72.dp),
                                targetPx = 256,
                                contentDescription = "${album.title} cover",
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    album.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${album.year ?: "—"} · ${album.trackCount} tracks · ${Format.longDuration(album.durationSeconds)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
