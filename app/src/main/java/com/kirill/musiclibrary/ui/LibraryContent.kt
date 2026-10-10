package com.kirill.musiclibrary.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirill.musiclibrary.domain.LibraryIndex
import com.kirill.musiclibrary.domain.LibraryUiState
import com.kirill.musiclibrary.domain.model.Library
import com.kirill.musiclibrary.ui.components.ErrorState
import com.kirill.musiclibrary.ui.components.LoadingState

/** Renders [content] once the library graph is parsed; shows loading/error states otherwise. */
@Composable
fun LibraryContent(
    viewModel: AppViewModel,
    content: @Composable (Library, LibraryIndex) -> Unit,
) {
    val state by viewModel.libraryState.collectAsStateWithLifecycle()
    when (val s = state) {
        is LibraryUiState.Loading -> LoadingState()
        is LibraryUiState.Error -> ErrorState(s.message)
        is LibraryUiState.Ready -> content(s.library, s.index)
    }
}
