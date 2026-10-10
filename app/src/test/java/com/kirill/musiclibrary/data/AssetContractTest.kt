package com.kirill.musiclibrary.data

import com.google.common.truth.Truth.assertThat
import com.kirill.musiclibrary.domain.model.Library
import com.kirill.musiclibrary.domain.model.Lyric
import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * Data-contract test over the *generated* assets shipped in the APK. Guards schema, counts,
 * referential integrity, and that every referenced cover asset exists on disk.
 */
class AssetContractTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val assetsDir: File = sequenceOf(
        File("src/main/assets/library"),
        File("app/src/main/assets/library"),
    ).firstOrNull { it.isDirectory }
        ?: error("generated assets directory not found")

    private val library: Library by lazy {
        json.decodeFromString<Library>(File(assetsDir, "library.json").readText())
    }

    private val lyrics: Map<String, Lyric> by lazy {
        json.decodeFromString<Map<String, Lyric>>(File(assetsDir, "lyrics.json").readText())
    }

    @Test
    fun countsMatchTheLibrary() {
        val counts = library.counts
        assertThat(counts.artists).isEqualTo(14)
        assertThat(counts.albums).isEqualTo(115)
        assertThat(counts.discs).isEqualTo(129)
        assertThat(counts.tracks).isEqualTo(1578)
        assertThat(counts.lyrics).isEqualTo(1030)
        assertThat(library.schemaVersion).isEqualTo(1)
        assertThat(library.sourceHash).startsWith("sha256:")
    }

    @Test
    fun schemaCountsAreSelfConsistent() {
        val actualArtists = library.artists.size
        val actualAlbums = library.artists.sumOf { it.albums.size }
        val actualDiscs = library.artists.sumOf { a -> a.albums.sumOf { it.discs.size } }
        val actualTracks = library.artists.sumOf { a ->
            a.albums.sumOf { al -> al.discs.sumOf { it.tracks.size } }
        }
        assertThat(actualArtists).isEqualTo(library.counts.artists)
        assertThat(actualAlbums).isEqualTo(library.counts.albums)
        assertThat(actualDiscs).isEqualTo(library.counts.discs)
        assertThat(actualTracks).isEqualTo(library.counts.tracks)
    }

    @Test
    fun everyTrackIdIsUniqueAndEveryLyricResolves() {
        val ids = HashSet<String>()
        for (artist in library.artists) {
            for (album in artist.albums) {
                for (disc in album.discs) {
                    for (track in disc.tracks) {
                        assertThat(ids.add(track.id)).isTrue()
                    }
                }
            }
        }
        for (key in lyrics.keys) {
            assertThat(ids).contains(key)
        }
        assertThat(lyrics.size).isEqualTo(library.counts.lyrics)
    }

    @Test
    fun usbPositionsAreUnique() {
        val positions = library.artists.flatMap { a ->
            a.albums.flatMap { al -> al.discs.flatMap { d -> d.tracks.mapNotNull { it.usb } } }
        }
        assertThat(positions).containsNoDuplicates()
    }

    @Test
    fun everyCoverAssetExists() {
        var referenced = 0
        for (artist in library.artists) {
            for (album in artist.albums) {
                val asset = album.coverAsset ?: continue
                referenced++
                assertThat(File(assetsDir, asset).isFile).isTrue()
                assertThat(asset).matches("covers/[a-z0-9.-]+\\.(webp|jpg)")
            }
        }
        assertThat(referenced).isEqualTo(library.counts.covers)
    }
}
