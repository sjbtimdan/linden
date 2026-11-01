package org.sjbtimdan.linden.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app's card surface: a luminance step above the background, a hairline
 * border and a shadow tinted with the current accent instead of black.
 */
@Composable
fun Modifier.lindenCard(shape: Shape = CardShape, elevation: Dp = CardElevation): Modifier {
    val accent = MaterialTheme.colorScheme.primary
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = accent.copy(alpha = 0.20f),
            spotColor = accent.copy(alpha = 0.26f),
        )
        .clip(shape)
        .background(lindenColors().cardSurface)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
}
