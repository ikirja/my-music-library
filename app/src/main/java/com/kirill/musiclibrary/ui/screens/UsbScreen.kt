package com.kirill.musiclibrary.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kirill.musiclibrary.ui.AppViewModel
import com.kirill.musiclibrary.ui.LibraryContent
import com.kirill.musiclibrary.ui.components.CenteredMessage
import com.kirill.musiclibrary.ui.components.TrackRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsbScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenTrack: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("On USB") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LibraryContent(viewModel) { _, index ->
            val entries = index.tracksByUsb
            if (entries.isEmpty()) {
                CenteredMessage(
                    title = "Nothing on the USB stick yet",
                    body = "No track has a USB position in the current snapshot. Run the copy-to-usb " +
                        "skill, then refresh the app data with the update-library skill.",
                    modifier = Modifier.padding(padding),
                )
                return@LibraryContent
            }
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item("header") {
                    Text(
                        "${entries.size} tracks in USB play order",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                var lastGroup: String? = null
                entries.forEach { ref ->
                    val group = "${ref.artist.name}|${ref.album.title}"
                    if (group != lastGroup) {
                        lastGroup = group
                        item("g-$group") {
                            Text(
                                "${ref.artist.name} — ${ref.album.title}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                    item("t-${ref.track.id}") {
                        TrackRow(
                            ref = ref,
                            onClick = { onOpenTrack(ref.track.id) },
                            showCover = true,
                        )
                    }
                }
            }
        }
    }
}
