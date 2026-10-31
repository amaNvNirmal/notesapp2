package com.tunenotes

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB69CFF),
    onPrimary = Color(0xFF14101F),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF06201C),
    background = Color(0xFF0B0B10),
    onBackground = Color(0xFFECECF3),
    surface = Color(0xFF14141B),
    onSurface = Color(0xFFECECF3),
    surfaceVariant = Color(0xFF1D1D27),
    onSurfaceVariant = Color(0xFF9D9DB2),
    outline = Color(0xFF3A3A4A),
    error = Color(0xFFFF8A8A)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6A47F5),
    onPrimary = Color.White,
    secondary = Color(0xFF0FA89A),
    onSecondary = Color.White,
    background = Color(0xFFF6F5FB),
    onBackground = Color(0xFF14141B),
    surface = Color.White,
    onSurface = Color(0xFF14141B),
    surfaceVariant = Color(0xFFEAE8F4),
    onSurfaceVariant = Color(0xFF63637A),
    outline = Color(0xFFCFCDE0),
    error = Color(0xFFD33B3B)
)

@Composable
fun TuneNotesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
