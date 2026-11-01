package org.sjbtimdan.linden.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private const val GLOW_LIGHT_ALPHA = 0.12f
private const val GLOW_DARK_ALPHA = 0.10f

/**
 * Ambient app backdrop: a vertical tint from [MaterialTheme.colorScheme.background]
 * to `surfaceContainer` with one faint warm glow bleeding in from the top-right,
 * reusing the hero card's accent. Content is wrapped in a transparent surface so
 * text keeps the usual content color.
 */
@Composable
fun LindenBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val glow = lindenColors().heroAccent.copy(
        alpha = if (colors.background.luminance() > 0.5f) GLOW_LIGHT_ALPHA else GLOW_DARK_ALPHA,
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(colors.background, colors.surfaceContainer)))
                val glowCenter = Offset(size.width * 0.9f, size.height * 0.08f)
                val glowRadius = size.minDimension * 0.9f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glow, Color.Transparent),
                        center = glowCenter,
                        radius = glowRadius,
                    ),
                    radius = glowRadius,
                    center = glowCenter,
                )
            },
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
            contentColor = colors.onBackground,
        ) {
            content()
        }
    }
}
