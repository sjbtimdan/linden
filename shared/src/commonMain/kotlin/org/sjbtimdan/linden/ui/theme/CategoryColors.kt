package org.sjbtimdan.linden.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Deterministic accents for categories. The same name always maps to the same
 * color, across restarts and platforms (String.hashCode is specified).
 */
val CategoryPalette: List<Color> = listOf(
    Color(0xFF5B8C5A), // sage
    Color(0xFFC77D46), // amber
    Color(0xFF4E7BA8), // steel blue
    Color(0xFFA05E7E), // mauve
    Color(0xFF7A8A3F), // olive
    Color(0xFFB0623F), // rust
    Color(0xFF5E7D96), // slate
    Color(0xFF9A7B3E), // ochre
)

/** Index into [CategoryPalette] for [name]; always non-negative. */
fun categoryColorIndex(name: String): Int =
    (name.hashCode() and Int.MAX_VALUE) % CategoryPalette.size

/** Stable accent color for a category name. */
fun categoryColor(name: String): Color = CategoryPalette[categoryColorIndex(name)]
