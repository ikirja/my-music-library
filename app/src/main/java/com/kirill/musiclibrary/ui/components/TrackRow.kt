package com.kirill.musiclibrary.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kirill.musiclibrary.domain.Format
import com.kirill.musiclibrary.domain.model.TrackRef

/**
 * One track row: number, title, duration, size, USB badge, and a lyrics indicator, optionally
 * with the album cover thumbnail (used on the On-USB list).
 */
@Composable
fun TrackRow(
    ref: TrackRef,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showCover: Boolean = false,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showCover) {
            AssetCover(
                coverAsset = ref.album.coverAsset,
                title = ref.album.title,
                modifier = Modifier.size(48.dp),
                targetPx = 128,
                contentDescription = "${ref.album.title} cover",
            )
        }
        Text(
            text = Format.trackNumber(ref.track.n),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ref.track.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (ref.track.hasLyrics) {
            Icon(
                imageVector = Icons.Filled.Lyrics,
                contentDescription = "Lyrics available",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = Format.duration(ref.track.durationSeconds),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = Format.size(ref.track.sizeBytes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp),
        )
        StatChip(text = Format.usb(ref.track.usb), emphasized = ref.track.usb != null)
    }
}
