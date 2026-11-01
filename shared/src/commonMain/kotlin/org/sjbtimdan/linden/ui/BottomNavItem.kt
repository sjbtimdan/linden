package org.sjbtimdan.linden.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.ui.theme.LindenColors
import org.sjbtimdan.linden.ui.theme.lindenColors

private val NavIconArea = 44.dp
private val SideCapsuleWidth = 56.dp
private val SideCapsuleHeight = 30.dp
private val CenterCircleSize = 44.dp
private val CenterCircleLift = 4.dp

/**
 * One tab of the floating navigation pill. Side tabs wear a gradient capsule
 * that springs in when selected; the center tab is always a raised Add circle,
 * with a gold ring and a small scale-up while selected. The label stays visible
 * so the tab keeps its [Role.Tab] text semantics.
 */
@Composable
internal fun RowScope.BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onSelect: () -> Unit,
    center: Boolean = false,
) {
    val colors = lindenColors()
    val dark = colors.heroGradient.first().luminance() < 0.2f
    val selectedBrush = if (dark) {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primaryContainer,
            ),
        )
    } else {
        Brush.linearGradient(colors.heroGradient)
    }
    val selectedContent = if (dark) MaterialTheme.colorScheme.onPrimary else colors.heroContent
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "navIconScale",
    )
    val iconTint by animateColorAsState(
        if (selected) selectedContent else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navIconTint",
    )
    val labelColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navLabelColor",
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .then(if (center) Modifier else Modifier.clip(RoundedCornerShape(24.dp)))
            .selectable(
                selected = selected,
                interactionSource = null,
                indication = if (center) null else LocalIndication.current,
                role = Role.Tab,
                onClick = onSelect,
            )
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(NavIconArea),
            contentAlignment = Alignment.Center,
        ) {
            if (center) {
                CenterAddCircle(
                    icon = icon,
                    selected = selected,
                    iconTint = selectedContent,
                    iconScale = iconScale,
                    brush = selectedBrush,
                    colors = colors,
                )
            } else {
                SideCapsule(
                    icon = icon,
                    selected = selected,
                    iconTint = iconTint,
                    iconScale = iconScale,
                    brush = selectedBrush,
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SideCapsule(icon: ImageVector, selected: Boolean, iconTint: Color, iconScale: Float, brush: Brush) {
    val pillAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "navPillAlpha",
    )
    Box(
        modifier = Modifier.size(width = SideCapsuleWidth, height = SideCapsuleHeight),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = pillAlpha
                    scaleX = 0.9f + 0.1f * pillAlpha
                    scaleY = scaleX
                }
                .clip(CircleShape)
                .background(brush),
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )
    }
}

@Composable
private fun CenterAddCircle(
    icon: ImageVector,
    selected: Boolean,
    iconTint: Color,
    iconScale: Float,
    brush: Brush,
    colors: LindenColors,
) {
    val ringAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.55f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "navCenterRing",
    )
    val circleScale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "navCenterScale",
    )
    Box(
        modifier = Modifier
            .offset(y = -CenterCircleLift)
            .size(CenterCircleSize)
            .graphicsLayer {
                scaleX = circleScale
                scaleY = circleScale
            }
            .shadow(6.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(brush)
            .border(1.5.dp, colors.heroAccent.copy(alpha = ringAlpha), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )
    }
}
