package org.sjbtimdan.linden.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors beyond the Material 3 roles, adapted per light/dark theme.
 * Used for entry-type amounts, avatars and tinted pills.
 */
data class LindenColors(
    val expense: Color,
    val expenseContainer: Color,
    val income: Color,
    val incomeContainer: Color,
    val transfer: Color,
    val transferContainer: Color,
)

val LightLindenColors = LindenColors(
    expense = Color(0xFFD64535),
    expenseContainer = Color(0xFFFFE0DA),
    income = Color(0xFF27A059),
    incomeContainer = Color(0xFFC9F2D6),
    transfer = Color(0xFF3E7CB8),
    transferContainer = Color(0xFFD9EAFB),
)

val DarkLindenColors = LindenColors(
    expense = Color(0xFFFF9E92),
    expenseContainer = Color(0xFF8A3225),
    income = Color(0xFFA3E887),
    incomeContainer = Color(0xFF315A28),
    transfer = Color(0xFFB8D4F5),
    transferContainer = Color(0xFF33506F),
)

val LocalLindenColors = compositionLocalOf { LightLindenColors }

/** Access the theme's semantic colors. Only valid inside [LindenTheme]. */
@Composable
fun lindenColors(): LindenColors = LocalLindenColors.current
