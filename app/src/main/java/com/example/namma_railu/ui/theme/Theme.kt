package com.example.namma_railu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RailBlue80,
    secondary = RailSlate80,
    tertiary = RailAmber80,
    background = Color(0xFF0B121A),
    surface = Color(0xFF111B26),
    surfaceVariant = Color(0xFF223142),
    onPrimary = Color(0xFF082B55),
    onSecondary = Color(0xFF17202A),
    onTertiary = Color(0xFF3D2600),
    onBackground = Color(0xFFEAF2FB),
    onSurface = Color(0xFFEAF2FB),
    onSurfaceVariant = Color(0xFFC7D6E5),
    primaryContainer = Color(0xFF0F4A97),
    onPrimaryContainer = Color(0xFFD9ECFF),
    secondaryContainer = Color(0xFF2E4052),
    onSecondaryContainer = Color(0xFFE3EEF8),
    tertiaryContainer = Color(0xFF5A3A00),
    onTertiaryContainer = Color(0xFFFFE5BF),
    errorContainer = Color(0xFF8B1A1A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = RailBlue40,
    secondary = RailSlate40,
    tertiary = RailAmber40,
    background = Color(0xFFF7FAFE),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE3ECF5),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0E1B2A),
    onSurface = Color(0xFF0E1B2A),
    onSurfaceVariant = Color(0xFF41576D),
    primaryContainer = Color(0xFFD9ECFF),
    onPrimaryContainer = Color(0xFF062B5A),
    secondaryContainer = Color(0xFFDCE7F2),
    onSecondaryContainer = Color(0xFF183043),
    tertiaryContainer = Color(0xFFFFE7BF),
    onTertiaryContainer = Color(0xFF4F2F00),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun Namma_RailuTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}