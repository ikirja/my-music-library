package com.kirill.musiclibrary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kirill.musiclibrary.domain.Format
import com.kirill.musiclibrary.domain.model.Lyric
import com.kirill.musiclibrary.domain.model.TrackRef
import com.kirill.musiclibrary.ui.AppViewModel
import com.kirill.musiclibrary.ui.LibraryContent
import com.kirill.musiclibrary.ui.components.AssetCover
import com.kirill.musiclibrary.ui.components.CenteredMessage
import com.kirill.musiclibrary.ui.components.StatChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailScreen(
    viewModel: AppViewModel,
    trackId: String,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lyrics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LibraryContent(viewModel) { _, index ->
            val ref = index.trackById[trackId]
            if (ref == null) {
                CenteredMessage("Track not found", modifier = Modifier.padding(padding))
                return@LibraryContent
            }
            var lyric by remember(trackId) { mutableStateOf<Lyric?>(null) }
            var loaded by remember(trackId) { mutableStateOf(false) }
            LaunchedEffect(trackId) {
                lyric = viewModel.lyric(trackId)
                loaded = true
            }
            val scroll = rememberScrollState()
            LaunchedEffect(trackId) { scroll.scrollTo(0) }
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxWidth()
                    .verticalScroll(scroll),
            ) {
                LyricHeader(ref)
                if (!loaded) {
                    CenteredMessage(title = "Loading lyrics…")
                } else if (lyric == null || lyric!!.text.isBlank()) {
                    Text(
                        "No lyrics found for this track.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                    LyricFooter(ref, source = null)
                } else {
                    val current = lyric!!
                    if (current.romaji) {
                        StatChip("Romaji", Modifier.padding(horizontal = 16.dp), emphasized = true)
                    }
                    SelectionContainer {
                        Text(
                            text = current.text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    LyricFooter(ref, source = current.source)
                }
            }
        }
    }
}

@Composable
private fun LyricHeader(ref: TrackRef) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssetCover(
            coverAsset = ref.album.coverAsset,
            title = ref.album.title,
            modifier = Modifier.size(96.dp),
            targetPx = 256,
            contentDescription = "${ref.album.title} cover",
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                ref.track.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${ref.artist.name} — ${ref.album.title}${ref.album.year?.let { " · $it" } ?: ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LyricFooter(ref: TrackRef, source: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatChip("Lyrics source: ${source ?: "unknown"}")
        StatChip(Format.duration(ref.track.durationSeconds))
        StatChip(Format.size(ref.track.sizeBytes))
        StatChip(Format.usb(ref.track.usb), emphasized = ref.track.usb != null)
    }
}
