package org.sjbtimdan.linden.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.Screen
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.nav_entry
import org.sjbtimdan.linden.resources.nav_ledger
import org.sjbtimdan.linden.resources.nav_settings

/** Height of the frosted pill itself. */
internal val FloatingNavigationBarHeight = 72.dp

/** Gap between the pill and the content/device edges. */
internal val FloatingNavigationBarMargin = 10.dp

/** Extra clearance below scrollable content so the last row clears the pill. */
internal val FloatingNavigationBarContentGap = 8.dp

private val FloatingNavigationBarShape = RoundedCornerShape(percent = 50)
private const val PILL_SHADOW = 10
private const val PILL_BLUR_RADIUS = 24
private const val PILL_NOISE = 0.05f
private const val PILL_TINT_LIGHT = 0.38f
private const val PILL_TINT_DARK = 0.5f
private const val PILL_FALLBACK_LIGHT = 0.9f
private const val PILL_FALLBACK_DARK = 0.92f
private const val PILL_HAIRLINE_ALPHA = 0.55f
private const val PILL_SHEEN_ALPHA = 0.06f

/**
 * Floating bottom navigation: a frosted pill over the scrolling content, with
 * a spring-selected capsule on the side tabs and a raised gradient Add circle
 * in the center. The blur samples the content marked by [HazeState]'s source in
 * `App`; Android 31+ and desktop blur natively, older Android falls back to a
 * tinted scrim. The bar is drawn after the content and owns the bottom inset.
 */
@Composable
internal fun FloatingNavigationBar(
    selected: Screen,
    onSelect: (Screen) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val style = navPillStyle()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = FloatingNavigationBarMargin)
            .testTag("bottomNav"),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(FloatingNavigationBarHeight),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .shadow(PILL_SHADOW.dp, FloatingNavigationBarShape, clip = false)
                    .clip(FloatingNavigationBarShape)
                    .hazeBlur(input = HazeInput.Sources(hazeState), style = style)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = PILL_SHEEN_ALPHA), Color.Transparent),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = PILL_HAIRLINE_ALPHA),
                        shape = FloatingNavigationBarShape,
                    ),
            )
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomNavItem(
                    label = stringResource(Res.string.nav_ledger),
                    icon = Icons.AutoMirrored.Filled.List,
                    selected = selected == Screen.Ledger,
                    onSelect = { onSelect(Screen.Ledger) },
                )
                BottomNavItem(
                    label = stringResource(Res.string.nav_entry),
                    icon = Icons.Filled.Add,
                    selected = selected == Screen.Entry,
                    onSelect = { onSelect(Screen.Entry) },
                    center = true,
                )
                BottomNavItem(
                    label = stringResource(Res.string.nav_settings),
                    icon = Icons.Default.Settings,
                    selected = selected == Screen.Settings,
                    onSelect = { onSelect(Screen.Settings) },
                )
            }
        }
    }
}

@Composable
private fun navPillStyle(): HazeBlurStyle {
    val surface = MaterialTheme.colorScheme.surface
    val light = surface.luminance() >= 0.5f
    return remember(surface, light) {
        HazeBlurStyle {
            blurRadius(PILL_BLUR_RADIUS.dp)
            noiseFactor(PILL_NOISE)
            backgroundColor(surface)
            colorEffects(
                listOf(
                    HazeColorEffect.tint(
                        surface.copy(alpha = if (light) PILL_TINT_LIGHT else PILL_TINT_DARK),
                    ),
                ),
            )
            fallbackColorEffect(
                HazeColorEffect.tint(
                    surface.copy(alpha = if (light) PILL_FALLBACK_LIGHT else PILL_FALLBACK_DARK),
                ),
            )
        }
    }
}
