package com.kirill.musiclibrary.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Curated fallback seed scheme (deep indigo / teal accent) echoing the LIBRARY.html palette:
// --accent:#7c9cff, --accent2:#56e0c0, --accent3:#c07cff.
private val Indigo = Color(0xFF5B6CFF)
private val IndigoDark = Color(0xFFB9C3FF)
private val Teal = Color(0xFF2AA893)
private val TealLight = Color(0xFF6FF5D8)
private val Violet = Color(0xFF9A5BFF)

val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDEE1FF),
    onPrimaryContainer = Color(0xFF001159),
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB8F2E4),
    onSecondaryContainer = Color(0xFF002019),
    tertiary = Violet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEADDFF),
    onTertiaryContainer = Color(0xFF21005D),
    background = Color(0xFFFBF8FF),
    surface = Color(0xFFFBF8FF),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
)

val DarkColors = darkColorScheme(
    primary = IndigoDark,
    onPrimary = Color(0xFF1A2A9E),
    primaryContainer = Color(0xFF3846BE),
    onPrimaryContainer = Color(0xFFDEE1FF),
    secondary = TealLight,
    onSecondary = Color(0xFF00382E),
    secondaryContainer = Color(0xFF005144),
    onSecondaryContainer = Color(0xFFB8F2E4),
    tertiary = Color(0xFFD0BCFF),
    onTertiary = Color(0xFF381E72),
    tertiaryContainer = Color(0xFF4F378B),
    onTertiaryContainer = Color(0xFFEADDFF),
    background = Color(0xFF121318),
    surface = Color(0xFF121318),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
)
