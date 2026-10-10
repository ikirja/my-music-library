package com.kirill.musiclibrary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kirill.musiclibrary.ui.AppViewModel
import com.kirill.musiclibrary.ui.MusicLibraryRoot
import com.kirill.musiclibrary.ui.theme.MusicLibraryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MusicLibraryApp).container
        setContent {
            val viewModel: AppViewModel = viewModel(factory = AppViewModel.factory(container))
            val settings by viewModel.settings.collectAsStateWithLifecycle(initialValue = viewModel.currentSettings)
            MusicLibraryTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
            ) {
                MusicLibraryRoot(viewModel = viewModel)
            }
        }
    }
}
