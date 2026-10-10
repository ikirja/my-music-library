package com.kirill.musiclibrary.domain

import com.kirill.musiclibrary.domain.model.Album
import com.kirill.musiclibrary.domain.model.Artist
import com.kirill.musiclibrary.domain.model.Library
import com.kirill.musiclibrary.domain.model.SearchHit
import com.kirill.musiclibrary.domain.model.Track

/**
 * Global search over artists, albums, and tracks. Reproduces the HTML behaviour:
 * an album match also reveals its tracks, and results are matched case-insensitively
 * against a normalized haystack (artist + title [+ year]).
 */
class SearchUseCase(private val library: Library, private val index: LibraryIndex) {

    private val artistsById = library.artists.associateBy { it.id }

    fun search(rawQuery: String, limitPerSection: Int = 60): List<SearchHit> {
        val query = rawQuery.trim().lowercase()
        if (query.isEmpty()) return emptyList()

        val albumHits = ArrayList<SearchHit>()
        val trackHits = ArrayList<SearchHit>()
        val artistHits = ArrayList<SearchHit>()

        for ((album, artist) in index.allAlbums) {
            val albumHaystack = "${artist.name} ${album.title} ${album.year ?: ""}".lowercase()
            val albumMatches = albumHaystack.contains(query)
            if (albumMatches && albumHits.size < limitPerSection) {
                albumHits += SearchHit.AlbumHit(album, artist)
            }
            for (disc in album.discs) {
                for (track in disc.tracks) {
                    if (trackHits.size >= limitPerSection) break
                    val trackHaystack =
                        "${track.title} ${artist.name} ${album.title}".lowercase()
                    if (albumMatches || trackHaystack.contains(query)) {
                        trackHits += SearchHit.TrackHit(
                            com.kirill.musiclibrary.domain.model.TrackRef(track, artist, album, disc.name)
                        )
                    }
                }
            }
        }

        for (artist in library.artists) {
            if (artistHits.size >= limitPerSection) break
            val nameMatches = artist.name.lowercase().contains(query)
            val hasMatch = nameMatches || artist.albums.any { album ->
                "${artist.name} ${album.title} ${album.year ?: ""}".lowercase().contains(query)
            } || artist.albums.any { album ->
                album.discs.any { disc ->
                    disc.tracks.any { "${it.title} ${artist.name} ${album.title}".lowercase().contains(query) }
                }
            }
            if (hasMatch) artistHits += SearchHit.ArtistHit(artist)
        }

        return artistHits + albumHits + trackHits
    }
}

/** Tracks ordered by their 1-based USB play position (ascending). */
class UsbOrderUseCase(private val index: LibraryIndex) {
    fun entries(): List<com.kirill.musiclibrary.domain.model.TrackRef> = index.tracksByUsb
    fun isEmpty(): Boolean = index.tracksByUsb.isEmpty()
}
