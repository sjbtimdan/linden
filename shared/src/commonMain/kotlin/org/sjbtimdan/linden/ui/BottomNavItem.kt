package org.sjbtimdan.linden.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.ui.theme.lindenColors

/** Matches M3's tall navigation-bar container; the bar row only sets this as a minimum. */
private val NavBarHeight = 80.dp

/**
 * One bottom-navigation tab. The selected tab wears the same gradient as the
 * total-balance hero, with the icon and label tinted to match; selection
 * fades and scales the pill in.
 */
@Composable
internal fun RowScope.BottomNavItem(label: String, icon: ImageVector, selected: Boolean, onSelect: () -> Unit) {
    val colors = lindenColors()
    val pillAlpha by animateFloatAsState(if (selected) 1f else 0f, label = "navPillAlpha")
    val iconTint by animateColorAsState(
        if (selected) colors.heroContent else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navIconTint",
    )
    val labelColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navLabelColor",
    )
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .selectable(selected = selected, role = Role.Tab, onClick = onSelect)
            .defaultMinSize(minHeight = NavBarHeight)
            .weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(width = 64.dp, height = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Only the pill fades out: the icon stays visible while unselected.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = pillAlpha
                        scaleX = 0.92f + 0.08f * pillAlpha
                        scaleY = scaleX
                    }
                    .clip(CircleShape)
                    .background(Brush.linearGradient(colors.heroGradient)),
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
        )
    }
}
