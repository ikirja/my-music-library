package com.kirill.musiclibrary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kirill.musiclibrary.domain.Format
import com.kirill.musiclibrary.domain.model.TrackRef
import com.kirill.musiclibrary.ui.AppViewModel
import com.kirill.musiclibrary.ui.LibraryContent
import com.kirill.musiclibrary.ui.components.AssetCover
import com.kirill.musiclibrary.ui.components.CenteredMessage
import com.kirill.musiclibrary.ui.components.StatChip
import com.kirill.musiclibrary.ui.components.TrackRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    viewModel: AppViewModel,
    albumId: String,
    onBack: () -> Unit,
    onOpenTrack: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Album") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LibraryContent(viewModel) { _, index ->
            val album = index.albumById[albumId]
            if (album == null) {
                CenteredMessage("Album not found", modifier = Modifier.padding(padding))
                return@LibraryContent
            }
            val collapsed = remember(album.id) { mutableStateMapOf<String, Boolean>() }
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item("hero") {
                    Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                        AssetCover(
                            coverAsset = album.coverAsset,
                            title = album.title,
                            modifier = Modifier.fillMaxSize(),
                            targetPx = 768,
                            contentDescription = "${album.title} cover",
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0xCC000000)),
                                    )
                                ),
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                        ) {
                            Text(
                                album.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                            )
                            Text(
                                "${album.year ?: "—"} · ${album.trackCount} tracks · ${Format.longDuration(album.durationSeconds)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
                item("chips") {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatChip("${album.year ?: "—"}")
                        StatChip("${album.trackCount} tracks")
                        StatChip(Format.longDuration(album.durationSeconds))
                    }
                }
                album.discs.forEachIndexed { discIndex, disc ->
                    val key = "disc-$discIndex"
                    val isCollapsed = collapsed[key] == true
                    if (album.discs.size > 1) {
                        item("$key-header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { collapsed[key] = !isCollapsed }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    disc.name ?: "Disc ${discIndex + 1}",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Icon(
                                    if (isCollapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                                    contentDescription = if (isCollapsed) "Expand" else "Collapse",
                                )
                            }
                        }
                    }
                    if (!isCollapsed) {
                        items(disc.tracks.size, key = { "${key}-${disc.tracks[it].id}" }) { i ->
                            val track = disc.tracks[i]
                            TrackRow(
                                ref = TrackRef(track, index.artistOf(album.id), album, disc.name),
                                onClick = { onOpenTrack(track.id) },
                            )
                        }
                    }
                    item("$key-divider") { HorizontalDivider() }
                }
            }
        }
    }
}
