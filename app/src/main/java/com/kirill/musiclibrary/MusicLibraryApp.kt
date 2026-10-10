package com.kirill.musiclibrary

import android.app.Application
import android.content.Context
import com.kirill.musiclibrary.data.AssetLibraryRepository
import com.kirill.musiclibrary.data.AssetLibrarySource
import com.kirill.musiclibrary.data.SettingsRepository
import com.kirill.musiclibrary.domain.LibraryRepository
import kotlinx.serialization.json.Json

/**
 * Manual dependency container (the plan allows manual DI over Hilt for this small,
 * single-module app). Created once by [MusicLibraryApp].
 */
class AppContainer(context: Context) {
    val json: Json = Json { ignoreUnknownKeys = true }
    val libraryRepository: LibraryRepository = AssetLibraryRepository(AssetLibrarySource(context.assets), json)
    val settingsRepository: SettingsRepository = SettingsRepository(context.applicationContext)
}

class MusicLibraryApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
