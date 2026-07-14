package com.ezirs.ui.theme
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
private val LightColorScheme = lightColorScheme(
    primary = md_theme_primary,
    onPrimary = md_theme_onPrimary,
    primaryContainer = md_theme_primaryContainer,
    onPrimaryContainer = md_theme_onPrimaryContainer,
    secondaryContainer = md_theme_secondaryContainer,
    onSecondaryContainer = md_theme_onSecondaryContainer,
    error = md_theme_error,
    onError = md_theme_onError,
    background = md_theme_background,
    onBackground = md_theme_onBackground,
    surface = md_theme_surface,
    onSurface = md_theme_onSurface,
    surfaceVariant = md_theme_surfaceVariant,
    onSurfaceVariant = md_theme_onSurfaceVariant,
    outline = md_theme_outline,
    outlineVariant = md_theme_outlineVariant
)
private val DarkColorScheme = darkColorScheme(
    primary = dark_md_theme_primary,
    onPrimary = dark_md_theme_onPrimary,
    primaryContainer = dark_md_theme_primaryContainer,
    onPrimaryContainer = dark_md_theme_onPrimaryContainer,
    secondaryContainer = dark_md_theme_secondaryContainer,
    onSecondaryContainer = dark_md_theme_onSecondaryContainer,
    error = dark_md_theme_error,
    onError = dark_md_theme_onError,
    background = dark_md_theme_background,
    onBackground = dark_md_theme_onBackground,
    surface = dark_md_theme_surface,
    onSurface = dark_md_theme_onSurface,
    surfaceVariant = dark_md_theme_surfaceVariant,
    onSurfaceVariant = dark_md_theme_onSurfaceVariant,
    outline = dark_md_theme_outline,
    outlineVariant = dark_md_theme_outlineVariant
)
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
