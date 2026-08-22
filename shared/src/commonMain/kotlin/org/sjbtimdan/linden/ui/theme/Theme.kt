package org.sjbtimdan.linden.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.sjbtimdan.linden.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6B4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4EBD9),
    onPrimaryContainer = Color(0xFF0A3B24),
    secondary = Color(0xFF8A6A38),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3E3C2),
    onSecondaryContainer = Color(0xFF3F2E0C),
    tertiary = Color(0xFF4E6B8A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6E5F5),
    onTertiaryContainer = Color(0xFF1B2E44),
    background = Color(0xFFF5F3EC),
    onBackground = Color(0xFF1B1C18),
    surface = Color(0xFFFBFAF5),
    onSurface = Color(0xFF1B1C18),
    surfaceVariant = Color(0xFFE9E7DE),
    onSurfaceVariant = Color(0xFF49483F),
    surfaceContainer = Color(0xFFEFEDE4),
    surfaceContainerHigh = Color(0xFFE9E7DD),
    surfaceContainerHighest = Color(0xFFE3E1D7),
    outline = Color(0xFF77756C),
    outlineVariant = Color(0xFFC8C6BB),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CD8B4),
    onPrimary = Color(0xFF07331F),
    primaryContainer = Color(0xFF254D37),
    onPrimaryContainer = Color(0xFFB8EFCC),
    secondary = Color(0xFFE3C28A),
    onSecondary = Color(0xFF3E2F0D),
    secondaryContainer = Color(0xFF5A4620),
    onSecondaryContainer = Color(0xFFF7DFB0),
    tertiary = Color(0xFFB1C9E8),
    onTertiary = Color(0xFF21344B),
    tertiaryContainer = Color(0xFF374B64),
    onTertiaryContainer = Color(0xFFD6E5F8),
    background = Color(0xFF131713),
    onBackground = Color(0xFFE2E4DD),
    surface = Color(0xFF161A16),
    onSurface = Color(0xFFE2E4DD),
    surfaceVariant = Color(0xFF41483F),
    onSurfaceVariant = Color(0xFFC0C8BC),
    surfaceContainer = Color(0xFF1E221D),
    surfaceContainerHigh = Color(0xFF282D27),
    surfaceContainerHighest = Color(0xFF333833),
    outline = Color(0xFF8A9288),
    outlineVariant = Color(0xFF41483F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val LindenTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)

@Composable
fun LindenTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val lindenColors = if (darkTheme) DarkLindenColors else LightLindenColors

    CompositionLocalProvider(LocalLindenColors provides lindenColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = LindenTypography,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
            ) {
                content()
            }
        }
    }
}
