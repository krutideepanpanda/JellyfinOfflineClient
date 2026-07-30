package com.example.jellyfinoffline.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00A4DC), // Jellyfin Brand Cyan/Blue
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003D52),
    onPrimaryContainer = Color(0xFF9EE0FF),
    secondary = Color(0xFF00D1B2), // Neon Teal
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = Color(0xFFA7FCEE),
    tertiary = Color(0xFFA881FF), // Neon Purple
    onTertiary = Color.Black,
    background = Color(0xFF000000), // OLED True Black for Pixel 10 Super Actua Display
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF0B0B0B), // Ultra dark surface
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF171717),
    onSurfaceVariant = Color(0xFFA6A6A6),
    error = Color(0xFFFF5252),
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0085B2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3EFFF),
    onPrimaryContainer = Color(0xFF001F2D),
    secondary = Color(0xFF009688),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB9F6CA),
    onSecondaryContainer = Color(0xFF00201A),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE9ECEF),
    onSurfaceVariant = Color(0xFF495057)
)

@Composable
fun JellyfinOfflineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ (API 31+), ideal for Google Pixel 10 (API 35)
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                // Apply Pixel 10 OLED True Black override to dynamic dark scheme
                dynamicDarkColorScheme(context).copy(
                    background = Color(0xFF000000),
                    surface = Color(0xFF0B0B0B)
                )
            } else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
