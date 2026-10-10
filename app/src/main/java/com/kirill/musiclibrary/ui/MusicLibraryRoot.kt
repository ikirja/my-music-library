package com.kirill.musiclibrary.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.kirill.musiclibrary.ui.nav.AdaptiveNavigationScaffold
import com.kirill.musiclibrary.ui.nav.AlbumRoute
import com.kirill.musiclibrary.ui.nav.AlbumsRoute
import com.kirill.musiclibrary.ui.nav.ArtistRoute
import com.kirill.musiclibrary.ui.nav.ArtistsRoute
import com.kirill.musiclibrary.ui.nav.HomeRoute
import com.kirill.musiclibrary.ui.nav.LyricsRoute
import com.kirill.musiclibrary.ui.nav.SearchRoute
import com.kirill.musiclibrary.ui.nav.SettingsRoute
import com.kirill.musiclibrary.ui.nav.TopDestination
import com.kirill.musiclibrary.ui.nav.TrackRoute
import com.kirill.musiclibrary.ui.nav.TracksRoute
import com.kirill.musiclibrary.ui.nav.UsbRoute
import com.kirill.musiclibrary.ui.screens.AlbumDetailScreen
import com.kirill.musiclibrary.ui.screens.AlbumsScreen
import com.kirill.musiclibrary.ui.screens.ArtistDetailScreen
import com.kirill.musiclibrary.ui.screens.ArtistsScreen
import com.kirill.musiclibrary.ui.screens.HomeScreen
import com.kirill.musiclibrary.ui.screens.LyricsScreen
import com.kirill.musiclibrary.ui.screens.SearchScreen
import com.kirill.musiclibrary.ui.screens.SettingsScreen
import com.kirill.musiclibrary.ui.screens.TrackDetailScreen
import com.kirill.musiclibrary.ui.screens.TracksScreen
import com.kirill.musiclibrary.ui.screens.UsbScreen

@Composable
fun MusicLibraryRoot(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val selectedTop = topLevelFor(backStackEntry?.destination)

    AdaptiveNavigationScaffold(
        destinations = TopDestination.entries.toList(),
        selected = selectedTop,
        onSelect = { dest -> navController.navigateToTop(dest) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenSearch = { navController.navigate(SearchRoute) },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    onOpenUsb = { navController.navigate(UsbRoute) },
                    onOpenArtists = { navController.navigateToTop(TopDestination.ARTISTS) },
                    onOpenAlbums = { navController.navigateToTop(TopDestination.ALBUMS) },
                    onOpenTracks = { navController.navigateToTop(TopDestination.TRACKS) },
                    onOpenAlbum = { navController.navigate(AlbumRoute(it)) },
                )
            }
            composable<ArtistsRoute> {
                ArtistsScreen(viewModel) { navController.navigate(ArtistRoute(it)) }
            }
            composable<ArtistRoute> { entry ->
                val id = entry.toRoute<ArtistRoute>().artistId
                ArtistDetailScreen(
                    viewModel = viewModel,
                    artistId = id,
                    onBack = { navController.popBackStack() },
                    onOpenAlbum = { navController.navigate(AlbumRoute(it)) },
                )
            }
            composable<AlbumsRoute> {
                AlbumsScreen(viewModel) { navController.navigate(AlbumRoute(it)) }
            }
            composable<AlbumRoute> { entry ->
                val id = entry.toRoute<AlbumRoute>().albumId
                AlbumDetailScreen(
                    viewModel = viewModel,
                    albumId = id,
                    onBack = { navController.popBackStack() },
                    onOpenTrack = { navController.navigate(TrackRoute(it)) },
                )
            }
            composable<TracksRoute> {
                TracksScreen(viewModel) { navController.navigate(TrackRoute(it)) }
            }
            composable<LyricsRoute> {
                LyricsScreen(viewModel) { navController.navigate(TrackRoute(it)) }
            }
            composable<TrackRoute> { entry ->
                val id = entry.toRoute<TrackRoute>().trackId
                TrackDetailScreen(
                    viewModel = viewModel,
                    trackId = id,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<UsbRoute> {
                UsbScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenTrack = { navController.navigate(TrackRoute(it)) },
                )
            }
            composable<SearchRoute> {
                SearchScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenArtist = { navController.navigate(ArtistRoute(it)) },
                    onOpenAlbum = { navController.navigate(AlbumRoute(it)) },
                    onOpenTrack = { navController.navigate(TrackRoute(it)) },
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateToTop(dest: TopDestination) {
    val route = when (dest) {
        TopDestination.HOME -> HomeRoute
        TopDestination.ARTISTS -> ArtistsRoute
        TopDestination.ALBUMS -> AlbumsRoute
        TopDestination.TRACKS -> TracksRoute
        TopDestination.LYRICS -> LyricsRoute
    }
    navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(graph.startDestinationId) { saveState = true }
    }
}

private fun topLevelFor(destination: NavDestination?): TopDestination? {
    val routes = destination?.hierarchy?.joinToString(" ") { it.route ?: "" } ?: return null
    return when {
        routes.contains("HomeRoute") -> TopDestination.HOME
        routes.contains("ArtistsRoute") || routes.contains("ArtistRoute") -> TopDestination.ARTISTS
        routes.contains("AlbumsRoute") || routes.contains("AlbumRoute") -> TopDestination.ALBUMS
        routes.contains("TracksRoute") || routes.contains("TrackRoute") -> TopDestination.TRACKS
        routes.contains("LyricsRoute") -> TopDestination.LYRICS
        else -> null
    }
}
