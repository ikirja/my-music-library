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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.kirill.musiclibrary.domain.LibraryIndex
import com.kirill.musiclibrary.domain.model.Album
import com.kirill.musiclibrary.ui.AppViewModel
import com.kirill.musiclibrary.ui.LibraryContent
import com.kirill.musiclibrary.ui.components.AssetCover
import com.kirill.musiclibrary.ui.components.SectionTitle
import com.kirill.musiclibrary.ui.components.StatChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUsb: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenTracks: () -> Unit,
    onOpenAlbum: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Music Library") },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        LibraryContent(viewModel) { library, index ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item("stats") { StatsHero(
                    artists = library.counts.artists,
                    albums = library.counts.albums,
                    tracks = library.counts.tracks,
                    duration = Format.longDuration(library.counts.durationSeconds),
                ) }
                item("shortcuts") {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatChip("Browse artists", Modifier.clickableChip(onOpenArtists))
                        StatChip("All albums", Modifier.clickableChip(onOpenAlbums))
                        StatChip("All tracks", Modifier.clickableChip(onOpenTracks))
                    }
                }
                item("usb") { UsbSummary(index.tracksByUsb.size, onOpenUsb) }
                item("featured-title") { SectionTitle("Featured covers") }
                item("featured") { FeaturedRow(index, onOpenAlbum) }
            }
        }
    }
}

private fun Modifier.clickableChip(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

@Composable
private fun StatsHero(artists: Int, albums: Int, tracks: Int, duration: String) {
    Card(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Your offline catalogue", style = MaterialTheme.typography.titleMedium)
            Text(
                "$artists artists · $albums albums",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "${"%,d".format(java.util.Locale.US, tracks)} tracks · $duration",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun UsbSummary(count: Int, onOpenUsb: () -> Unit) {
    Card(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        onClick = onOpenUsb,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Usb, contentDescription = null)
            Column {
                Text("On USB", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (count == 0) "No tracks on the stick yet" else "$count tracks in play order",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FeaturedRow(index: LibraryIndex, onOpenAlbum: (String) -> Unit) {
    val featured: List<Pair<Album, com.kirill.musiclibrary.domain.model.Artist>> =
        index.allAlbums.filter { it.first.coverAsset != null }.take(16)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(featured, key = { it.first.id }) { (album, artist) ->
            Column(
                modifier = Modifier
                    .size(width = 132.dp, height = 190.dp)
                    .clickableChip { onOpenAlbum(album.id) },
            ) {
                AssetCover(
                    coverAsset = album.coverAsset,
                    title = album.title,
                    modifier = Modifier.size(132.dp),
                    targetPx = 384,
                    contentDescription = "${album.title} cover",
                )
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
