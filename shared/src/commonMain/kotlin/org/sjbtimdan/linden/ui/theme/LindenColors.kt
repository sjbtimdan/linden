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
    expense = Color(0xFFC8462F),
    expenseContainer = Color(0xFFFFDAD3),
    income = Color(0xFF3F8C2E),
    incomeContainer = Color(0xFFD9F0C8),
    transfer = Color(0xFF4C7296),
    transferContainer = Color(0xFFD9E8F5),
)

val DarkLindenColors = LindenColors(
    expense = Color(0xFFFF8B7E),
    expenseContainer = Color(0xFF7A2A1E),
    income = Color(0xFFA4E07E),
    incomeContainer = Color(0xFF2C4A1E),
    transfer = Color(0xFFA9C5E8),
    transferContainer = Color(0xFF2C4058),
)

val LocalLindenColors = compositionLocalOf { LightLindenColors }

/** Access the theme's semantic colors. Only valid inside [LindenTheme]. */
@Composable
fun lindenColors(): LindenColors = LocalLindenColors.current
