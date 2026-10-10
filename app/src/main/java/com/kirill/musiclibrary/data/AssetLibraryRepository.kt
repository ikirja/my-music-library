package com.kirill.musiclibrary.data

import android.content.res.AssetManager
import com.kirill.musiclibrary.domain.LibraryRepository
import com.kirill.musiclibrary.domain.LibraryIndex
import com.kirill.musiclibrary.domain.LibraryUiState
import com.kirill.musiclibrary.domain.model.Library
import com.kirill.musiclibrary.domain.model.Lyric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Reads the generated JSON assets shipped inside the APK. */
class AssetLibrarySource(private val assets: AssetManager) {
    fun readLibraryJson(): String =
        assets.open("library/library.json").bufferedReader().use { it.readText() }

    fun readLyricsJson(): String =
        assets.open("library/lyrics.json").bufferedReader().use { it.readText() }
}

/** Parses the asset JSON once on first access and exposes the immutable graph. */
class AssetLibraryRepository(
    private val source: AssetLibrarySource,
    private val json: Json,
) : LibraryRepository {

    private val _state = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    override val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    private val loadMutex = Mutex()

    @Volatile
    private var loaded = false

    @Volatile
    private var lyricsCache: Map<String, Lyric>? = null

    override suspend fun load() {
        if (loaded) return
        loadMutex.withLock {
            if (loaded) return
            _state.value = LibraryUiState.Loading
            _state.value = try {
                val library = withContext(Dispatchers.IO) {
                    json.decodeFromString<Library>(source.readLibraryJson())
                }
                LibraryUiState.Ready(library, LibraryIndex.from(library))
            } catch (e: Exception) {
                LibraryUiState.Error(e.message ?: "Failed to load the library data")
            }
            loaded = true
        }
    }

    override suspend fun lyrics(): Map<String, Lyric> {
        lyricsCache?.let { return it }
        return withContext(Dispatchers.IO) {
            val parsed = json.decodeFromString<Map<String, Lyric>>(source.readLyricsJson())
            lyricsCache = parsed
            parsed
        }
    }

    override suspend fun lyric(trackId: String): Lyric? = lyrics()[trackId]
}
