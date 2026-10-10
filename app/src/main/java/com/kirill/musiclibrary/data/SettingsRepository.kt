package com.kirill.musiclibrary.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Persists the two user preferences: theme mode and dynamic colour. */
class SettingsRepository(private val context: Context) {

    private val themeKey = intPreferencesKey("theme_mode")
    private val dynamicKey = booleanPreferencesKey("dynamic_color")

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = ThemeMode.entries.getOrElse(prefs[themeKey] ?: ThemeMode.SYSTEM.ordinal) {
                ThemeMode.SYSTEM
            },
            dynamicColor = prefs[dynamicKey] ?: true,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[themeKey] = mode.ordinal }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[dynamicKey] = enabled }
    }
}
