package com.kirill.musiclibrary.domain

import com.kirill.musiclibrary.domain.model.Library
import com.kirill.musiclibrary.domain.model.Lyric

/** Lifecycle of the eagerly-parsed library graph. */
sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Ready(val library: Library, val index: LibraryIndex) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

/**
 * Read-only access to the packaged library assets. The in-memory graph is built once on
 * first access; lyrics are loaded lazily and cached.
 */
interface LibraryRepository {
    val state: kotlinx.coroutines.flow.StateFlow<LibraryUiState>

    /** Parse `library.json` once. Safe to call repeatedly; later calls are no-ops. */
    suspend fun load()

    /** Lazily parse and cache `lyrics.json`; returns the lyric for [trackId] or null. */
    suspend fun lyric(trackId: String): Lyric?

    /** The full lazily-loaded lyrics map (parsed once, cached). */
    suspend fun lyrics(): Map<String, Lyric>
}
