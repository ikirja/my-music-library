package com.kirill.musiclibrary.domain.model

import kotlinx.serialization.Serializable

/**
 * Immutable, read-only library graph. Root of `library.json` (schema v1).
 *
 * These are also the wire DTOs: the data layer deserializes straight into the domain
 * models, so there is no lossy mapping step and no duplicated shape.
 */
@Serializable
data class Library(
    val schemaVersion: Int,
    val generatedAt: String,
    val sourceHash: String,
    val counts: Counts,
    val artists: List<Artist>,
)

@Serializable
data class Counts(
    val artists: Int,
    val albums: Int,
    val discs: Int,
    val tracks: Int,
    val lyrics: Int,
    val covers: Int,
    val durationSeconds: Double,
)

@Serializable
data class Artist(
    val id: String,
    val name: String,
    val albums: List<Album>,
)

@Serializable
data class Album(
    val id: String,
    val folder: String,
    val title: String,
    val year: Int? = null,
    val trackCount: Int,
    val durationSeconds: Double,
    val coverAsset: String? = null,
    val discs: List<Disc>,
)

@Serializable
data class Disc(
    val name: String? = null,
    val tracks: List<Track>,
)

@Serializable
data class Track(
    val id: String,
    val title: String,
    val n: Int? = null,
    val durationSeconds: Double,
    val sizeBytes: Long,
    val file: String,
    val usb: Int? = null,
    val hasLyrics: Boolean,
)

/** `lyrics.json` values, keyed by [Track.id]. */
@Serializable
data class Lyric(
    val text: String,
    val romaji: Boolean = false,
    val source: String? = null,
)

/** Flattened track paired with the artist/album it belongs to (for lists and USB). */
data class TrackRef(
    val track: Track,
    val artist: Artist,
    val album: Album,
    val discName: String?,
)

/** A single search hit across an artist, album, or track. */
sealed interface SearchHit {
    val displayTitle: String
    val subtitle: String

    data class ArtistHit(val artist: Artist) : SearchHit {
        override val displayTitle: String get() = artist.name
        override val subtitle: String get() = "${artist.albums.size} albums"
    }

    data class AlbumHit(val album: Album, val artist: Artist) : SearchHit {
        override val displayTitle: String get() = album.title
        override val subtitle: String get() = "${artist.name}${album.year?.let { " · $it" } ?: ""}"
    }

    data class TrackHit(val ref: TrackRef) : SearchHit {
        override val displayTitle: String get() = ref.track.title
        override val subtitle: String get() = "${ref.artist.name} — ${ref.album.title}"
    }
}
