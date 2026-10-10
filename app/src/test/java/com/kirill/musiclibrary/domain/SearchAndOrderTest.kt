package com.kirill.musiclibrary.domain

import com.google.common.truth.Truth.assertThat
import com.kirill.musiclibrary.domain.model.Album
import com.kirill.musiclibrary.domain.model.Artist
import com.kirill.musiclibrary.domain.model.Counts
import com.kirill.musiclibrary.domain.model.Disc
import com.kirill.musiclibrary.domain.model.Library
import com.kirill.musiclibrary.domain.model.SearchHit
import com.kirill.musiclibrary.domain.model.Track
import org.junit.Test

class SearchAndOrderTest {

    private fun track(id: String, title: String, usb: Int? = null, hasLyrics: Boolean = false) =
        Track(
            id = id,
            title = title,
            n = 1,
            durationSeconds = 180.0,
            sizeBytes = 1_000_000,
            file = "Artist/$id.flac",
            usb = usb,
            hasLyrics = hasLyrics,
        )

    private val trackA = track("t-a", "Opium", usb = 2, hasLyrics = true)
    private val trackB = track("t-b", "ХалиГалиКришна", usb = 1)

    private val album = Album(
        id = "agata-kristi-1995-opium",
        folder = "Agata Kristi - 1995 - Opium",
        title = "Opium",
        year = 1995,
        trackCount = 2,
        durationSeconds = 360.0,
        coverAsset = "covers/agata-kristi-1995-opium.webp",
        discs = listOf(Disc(name = null, tracks = listOf(trackA, trackB))),
    )

    private val library = Library(
        schemaVersion = 1,
        generatedAt = "2026-10-10T05:00:00Z",
        sourceHash = "sha256:abc",
        counts = Counts(1, 1, 1, 2, 1, 1, 360.0),
        artists = listOf(Artist(id = "agata-kristi", name = "Agata Kristi", albums = listOf(album))),
    )

    private val index = LibraryIndex.from(library)

    @Test
    fun search_matchesAlbumAndRevealsTracks() {
        val results = SearchUseCase(library, index).search("opium")
        assertThat(results.filterIsInstance<SearchHit.AlbumHit>()).hasSize(1)
        assertThat(results.filterIsInstance<SearchHit.TrackHit>()).hasSize(2)
    }

    @Test
    fun search_matchesCyrillicTrackTitle() {
        val results = SearchUseCase(library, index).search("ХалиГали")
        val tracks = results.filterIsInstance<SearchHit.TrackHit>()
        assertThat(tracks).hasSize(1)
        assertThat(tracks.first().ref.track.id).isEqualTo("t-b")
    }

    @Test
    fun search_blankReturnsNothing() {
        assertThat(SearchUseCase(library, index).search("   ")).isEmpty()
    }

    @Test
    fun usbOrder_isAscendingByPosition() {
        val entries = UsbOrderUseCase(index).entries()
        assertThat(entries.map { it.track.usb }).containsExactly(1, 2).inOrder()
        assertThat(entries.first().track.id).isEqualTo("t-b")
    }

    @Test
    fun index_buildsDerivedViews() {
        assertThat(index.allTracks).hasSize(2)
        assertThat(index.tracksWithLyrics.map { it.track.id }).containsExactly("t-a")
        assertThat(index.trackById["t-a"]?.album?.id).isEqualTo(album.id)
        assertThat(index.artistOf(album.id).name).isEqualTo("Agata Kristi")
    }
}
