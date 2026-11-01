package org.sjbtimdan.linden.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.space_grotesk_bold
import org.sjbtimdan.linden.resources.space_grotesk_medium
import org.sjbtimdan.linden.resources.space_grotesk_regular
import org.sjbtimdan.linden.resources.space_grotesk_semibold

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6E46),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F0CD),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF8F5E0E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDFA0),
    onSecondaryContainer = Color(0xFF2B1A00),
    tertiary = Color(0xFF2F6DA3),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD3E7FF),
    onTertiaryContainer = Color(0xFF0E2A47),
    background = Color(0xFFF6F5F0),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFE4E3D9),
    onSurfaceVariant = Color(0xFF45453D),
    surfaceContainer = Color(0xFFEFEEE6),
    surfaceContainerHigh = Color(0xFFE9E8E0),
    surfaceContainerHighest = Color(0xFFE3E2DA),
    outline = Color(0xFF75746B),
    outlineVariant = Color(0xFFC6C5BB),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD9A6),
    onPrimary = Color(0xFF00391C),
    primaryContainer = Color(0xFF2E5C40),
    onPrimaryContainer = Color(0xFFC4F1D3),
    secondary = Color(0xFFF0C87E),
    onSecondary = Color(0xFF3B2B00),
    secondaryContainer = Color(0xFF5F4A1F),
    onSecondaryContainer = Color(0xFFFFE3A8),
    tertiary = Color(0xFFB8CFF0),
    onTertiary = Color(0xFF1B3450),
    tertiaryContainer = Color(0xFF40587A),
    onTertiaryContainer = Color(0xFFD6E5FF),
    background = Color(0xFF101410),
    onBackground = Color(0xFFF0F2EB),
    surface = Color(0xFF1A1F1A),
    onSurface = Color(0xFFF0F2EB),
    surfaceVariant = Color(0xFF414A41),
    onSurfaceVariant = Color(0xFFCDD5C8),
    surfaceContainer = Color(0xFF222722),
    surfaceContainerHigh = Color(0xFF2C322C),
    surfaceContainerHighest = Color(0xFF383E37),
    outline = Color(0xFF9AA49A),
    outlineVariant = Color(0xFF414A41),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/** Corner radius of dialogs across the app. */
val DialogShape = RoundedCornerShape(28.dp)

/** Corner radius of list rows/cards across the app. */
val CardShape = RoundedCornerShape(20.dp)

/** Subtle shadow elevation for list-row cards. */
val CardElevation = 2.dp

/** Fixed-width digits app-wide so amounts, dates and times line up and stop shifting as values change. */
private fun TextStyle.tabularDigits(): TextStyle = copy(fontFeatureSettings = "\"tnum\" 1")

/**
 * Space Grotesk carries display, headline and title styles only; body copy,
 * labels and titleSmall keep the platform font so long text retains native
 * metrics. The family ships as static instances (composeResources/font) so it
 * renders identically on every target and on API 24-25, where variable axes
 * are ignored.
 */
@Composable
private fun lindenTypography(): Typography {
    val display = FontFamily(
        Font(Res.font.space_grotesk_regular, FontWeight.Normal),
        Font(Res.font.space_grotesk_medium, FontWeight.Medium),
        Font(Res.font.space_grotesk_semibold, FontWeight.SemiBold),
        Font(Res.font.space_grotesk_bold, FontWeight.Bold),
    )
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
        ).tabularDigits(),
        displayMedium = base.displayMedium.copy(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
        ).tabularDigits(),
        displaySmall = base.displaySmall.copy(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
        ).tabularDigits(),
        headlineLarge = base.headlineLarge.copy(
            fontFamily = display,
            fontWeight = FontWeight.Medium,
        ).tabularDigits(),
        headlineMedium = base.headlineMedium.copy(
            fontFamily = display,
            fontWeight = FontWeight.Medium,
        ).tabularDigits(),
        headlineSmall = base.headlineSmall.copy(
            fontFamily = display,
            fontWeight = FontWeight.Medium,
        ).tabularDigits(),
        titleLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
        ).tabularDigits(),
        titleMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.1.sp,
        ).tabularDigits(),
        titleSmall = base.titleSmall.tabularDigits(),
        bodyLarge = base.bodyLarge.tabularDigits(),
        bodyMedium = base.bodyMedium.tabularDigits(),
        bodySmall = base.bodySmall.tabularDigits(),
        labelLarge = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ).tabularDigits(),
        labelMedium = base.labelMedium.tabularDigits(),
        labelSmall = base.labelSmall.tabularDigits(),
    )
}

@Composable
fun LindenTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val lindenColors = if (darkTheme) DarkLindenColors else LightLindenColors
    val categoryPalette = if (darkTheme) DarkCategoryPalette else CategoryPalette

    CompositionLocalProvider(
        LocalLindenColors provides lindenColors,
        LocalCategoryPalette provides categoryPalette,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = lindenTypography(),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
            ) {
                content()
            }
        }
    }
}
