package com.suzunei.lumirisiptv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LumirisColorScheme = darkColorScheme(
    primary = LumirisPurple,
    secondary = LumirisGold,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onPrimary = OnSurfaceDark,
    onSecondary = BackgroundDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceMuted,
    error = AccentRed,
)

@Composable
fun LumirisTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LumirisColorScheme,
        typography = LumirisTypography,
        content = content,
    )
}
