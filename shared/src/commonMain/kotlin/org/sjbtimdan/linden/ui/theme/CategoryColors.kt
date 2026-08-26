package org.sjbtimdan.linden.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

private const val CATEGORY_PALETTE_SIZE = 8

/**
 * Deterministic accents for categories. The same name always maps to the same
 * palette index, across restarts and platforms (String.hashCode is specified).
 */
val CategoryPalette: List<Color> = listOf(
    Color(0xFF2B6FA6), // blue
    Color(0xFFC05B1E), // vermillion
    Color(0xFF1F8A70), // teal
    Color(0xFFA67C0A), // ochre
    Color(0xFFA84E86), // mauve
    Color(0xFF7C8F33), // olive
    Color(0xFF6E5CB8), // violet
    Color(0xFFD0608F), // pink
)

/** Same hues as [CategoryPalette], lightened for dark surfaces. */
val DarkCategoryPalette: List<Color> = listOf(
    Color(0xFF6FA8DC),
    Color(0xFFE8925A),
    Color(0xFF5CC9AE),
    Color(0xFFD9B654),
    Color(0xFFE28FC2),
    Color(0xFFB3C75A),
    Color(0xFFA699E8),
    Color(0xFFF0A3C9),
)

val LocalCategoryPalette = compositionLocalOf { CategoryPalette }

/** Index into the category palettes for [name]; always non-negative. */
fun categoryColorIndex(name: String): Int = (name.hashCode() and Int.MAX_VALUE) % CATEGORY_PALETTE_SIZE

/** Light-theme accent color for a category name. */
fun categoryColor(name: String): Color = CategoryPalette[categoryColorIndex(name)]

/** Theme-aware accent color for a stable name (categories, accounts). Only valid inside [LindenTheme]. */
@Composable
fun accentColor(name: String): Color = LocalCategoryPalette.current[categoryColorIndex(name)]
