package com.kirill.musiclibrary.domain

import com.kirill.musiclibrary.domain.model.Album
import com.kirill.musiclibrary.domain.model.Library
import com.kirill.musiclibrary.domain.model.Track
import com.kirill.musiclibrary.domain.model.TrackRef
import kotlin.math.roundToInt

/**
 * Formatting rules reproduced exactly from `LIBRARY.html` (see `plans/PLAN.md` §8.4):
 * per-track duration `m:ss`, long totals `h mm`, size MB with 0 decimals when >= 10 MB
 * else 1, and the deterministic gradient hue / initials for placeholder covers.
 */
object Format {

    /** `fmt(d)` — `m:ss`. */
    fun duration(seconds: Double): String {
        val total = (seconds.isFinite().let { if (it) seconds else 0.0 }).roundToInt().coerceAtLeast(0)
        val m = total / 60
        val s = total % 60
        return "$m:${s.toString().padStart(2, '0')}"
    }

    /** `fmtLong(d)` — `h mm` above an hour, else `m:ss`. */
    fun longDuration(seconds: Double): String {
        val total = seconds.roundToInt().coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "${h}h ${m.toString().padStart(2, '0')}m" else "$m:${s.toString().padStart(2, '0')}"
    }

    /** `size(b)` — MB, 0 decimals when >= 10 MB else 1. */
    fun size(bytes: Long): String {
        val mb = bytes / 1048576.0
        val decimals = if (bytes >= 10485760L) 0 else 1
        return String.format("%.${decimals}f MB", mb)
    }

    /** `initials(name)` — first letters of the first two whitespace-separated words. */
    fun initials(name: String): String {
        val words = name.replace(Regex("[^\\w\\s]"), " ").trim().split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
        val first = words.getOrNull(0)?.firstOrNull() ?: '?'
        val second = words.getOrNull(1)?.firstOrNull()
        return ("" + first + (second ?: "")).uppercase()
    }

    /** `hue(str)` — `(h*31 + charCode) % 360`. */
    fun hue(value: String): Int {
        var h = 0
        for (ch in value) {
            h = (h * 31 + ch.code) % 360
        }
        return h
    }

    /** USB badge text: `#n` or `N/A`. */
    fun usb(position: Int?): String = position?.let { "#$it" } ?: "N/A"

    /** Track number, zero-padded to two digits, or an em dash when absent. */
    fun trackNumber(n: Int?): String = n?.toString()?.padStart(2, '0') ?: "—"
}

/** Derived, pre-computed views over an immutable [Library]. */
data class LibraryIndex(
    val allAlbums: List<Pair<Album, com.kirill.musiclibrary.domain.model.Artist>>,
    val allTracks: List<TrackRef>,
    val tracksByUsb: List<TrackRef>,
    val tracksWithLyrics: List<TrackRef>,
    val artistById: Map<String, com.kirill.musiclibrary.domain.model.Artist>,
    val albumById: Map<String, Album>,
    val albumArtistById: Map<String, com.kirill.musiclibrary.domain.model.Artist>,
    val trackById: Map<String, TrackRef>,
) {
    fun artistOf(albumId: String): com.kirill.musiclibrary.domain.model.Artist =
        albumArtistById.getValue(albumId)

    companion object {
        fun from(library: Library): LibraryIndex {
            val allAlbums = ArrayList<Pair<Album, com.kirill.musiclibrary.domain.model.Artist>>()
            val allTracks = ArrayList<TrackRef>()
            val artistById = HashMap<String, com.kirill.musiclibrary.domain.model.Artist>()
            val albumById = HashMap<String, Album>()
            val albumArtistById = HashMap<String, com.kirill.musiclibrary.domain.model.Artist>()
            val trackById = HashMap<String, TrackRef>()
            for (artist in library.artists) {
                artistById[artist.id] = artist
                for (album in artist.albums) {
                    allAlbums += album to artist
                    albumById[album.id] = album
                    albumArtistById[album.id] = artist
                    for (disc in album.discs) {
                        for (track in disc.tracks) {
                            val ref = TrackRef(track, artist, album, disc.name)
                            allTracks += ref
                            trackById[track.id] = ref
                        }
                    }
                }
            }
            return LibraryIndex(
                allAlbums = allAlbums,
                allTracks = allTracks,
                tracksByUsb = allTracks.filter { it.track.usb != null }.sortedBy { it.track.usb },
                tracksWithLyrics = allTracks.filter { it.track.hasLyrics },
                artistById = artistById,
                albumById = albumById,
                albumArtistById = albumArtistById,
                trackById = trackById,
            )
        }
    }
}
