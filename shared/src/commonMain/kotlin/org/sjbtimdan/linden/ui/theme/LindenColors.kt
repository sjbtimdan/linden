package org.sjbtimdan.linden.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors beyond the Material 3 roles, adapted per light/dark theme.
 * Used for entry-type amounts, avatars, tinted pills and the total-balance hero.
 */
data class LindenColors(
    val expense: Color,
    val expenseContainer: Color,
    val income: Color,
    val incomeContainer: Color,
    val transfer: Color,
    val heroGradient: List<Color>,
    val heroContent: Color,
    val heroAccent: Color,
)

val LightLindenColors = LindenColors(
    expense = Color(0xFFD64535),
    expenseContainer = Color(0xFFFFE0DA),
    income = Color(0xFF27A059),
    incomeContainer = Color(0xFFC9F2D6),
    transfer = Color(0xFF3E7CB8),
    heroGradient = listOf(
        Color(0xFF0B3D25),
        Color(0xFF15613C),
        Color(0xFF1E7A49),
    ),
    heroContent = Color(0xFFFFFFFF),
    heroAccent = Color(0xFFF2C879),
)

val DarkLindenColors = LindenColors(
    expense = Color(0xFFFF9E92),
    expenseContainer = Color(0xFF8A3225),
    income = Color(0xFFA3E887),
    incomeContainer = Color(0xFF315A28),
    transfer = Color(0xFFB8D4F5),
    heroGradient = listOf(
        Color(0xFF082A1B),
        Color(0xFF0F4A31),
        Color(0xFF166142),
    ),
    heroContent = Color(0xFFF0F7F1),
    heroAccent = Color(0xFFF0C87E),
)

val LocalLindenColors = compositionLocalOf { LightLindenColors }

/** Access the theme's semantic colors. Only valid inside [LindenTheme]. */
@Composable
fun lindenColors(): LindenColors = LocalLindenColors.current
