package com.kirill.musiclibrary.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues

/**
 * Adaptive navigation shell: a bottom navigation bar on compact width and a navigation rail on
 * medium/expanded width. This is a dependency-light stand-in for
 * `NavigationSuiteScaffold` (the `material3-adaptive` artifact), preserving the plan's
 * bar ↔ rail behaviour without adding the experimental adaptive dependency.
 */
@Composable
fun AdaptiveNavigationScaffold(
    destinations: List<TopDestination>,
    selected: TopDestination?,
    onSelect: (TopDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val widthDp = LocalConfiguration.current.screenWidthDp
    if (widthDp >= 720) {
        Row(modifier = modifier.fillMaxSize()) {
            NavigationRail {
                destinations.forEach { dest ->
                    NavigationRailItem(
                        selected = dest == selected,
                        onClick = { onSelect(dest) },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) { content(PaddingValues(0.dp)) }
        }
    } else {
        androidx.compose.material3.Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    destinations.forEach { dest ->
                        NavigationBarItem(
                            selected = dest == selected,
                            onClick = { onSelect(dest) },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            content(innerPadding)
        }
    }
}
