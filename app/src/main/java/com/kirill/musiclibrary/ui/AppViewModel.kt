package com.kirill.musiclibrary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kirill.musiclibrary.AppContainer
import com.kirill.musiclibrary.data.AppSettings
import com.kirill.musiclibrary.data.SettingsRepository
import com.kirill.musiclibrary.data.ThemeMode
import com.kirill.musiclibrary.domain.LibraryRepository
import com.kirill.musiclibrary.domain.LibraryUiState
import com.kirill.musiclibrary.domain.model.Lyric
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Single ViewModel for the read-only app: library graph + user settings. */
class AppViewModel(
    private val repository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val libraryState: StateFlow<LibraryUiState> = repository.state

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings(),
    )

    val currentSettings: AppSettings get() = settings.value

    init {
        viewModelScope.launch { repository.load() }
    }

    suspend fun lyric(trackId: String): Lyric? = repository.lyric(trackId)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                AppViewModel(container.libraryRepository, container.settingsRepository)
            }
        }
    }
}
