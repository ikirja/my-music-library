package com.kirill.musiclibrary.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

// Type-safe navigation routes (navigation-compose 2.8+).
@Serializable
object HomeRoute

@Serializable
object ArtistsRoute

@Serializable
object AlbumsRoute

@Serializable
object TracksRoute

@Serializable
object LyricsRoute

@Serializable
object UsbRoute

@Serializable
object SearchRoute

@Serializable
object SettingsRoute

@Serializable
data class ArtistRoute(val artistId: String)

@Serializable
data class AlbumRoute(val albumId: String)

@Serializable
data class TrackRoute(val trackId: String)

/** The five primary destinations that live in the bottom bar / navigation rail. */
enum class TopDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    ARTISTS("Artists", Icons.Filled.Person),
    ALBUMS("Albums", Icons.Filled.Album),
    TRACKS("Tracks", Icons.Filled.MusicNote),
    LYRICS("Lyrics", Icons.Filled.Lyrics),
}
