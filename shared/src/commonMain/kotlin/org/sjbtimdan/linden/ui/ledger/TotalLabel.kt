package org.sjbtimdan.linden.ui.ledger

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.theme.lindenColors

@Composable
internal fun TotalLabel(total: Long?, currency: Currency, hidden: Boolean = false) {
    val colors = lindenColors()
    val tint = when {
        total != null && total < 0 -> colors.expense
        total != null && total > 0 -> colors.income
        else -> null
    }
    val container = when {
        total != null && total < 0 -> colors.expenseContainer
        total != null && total > 0 -> colors.incomeContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val animatedContainer by animateColorAsState(container, label = "totalContainer")
    val animatedTint by animateColorAsState(
        tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
        label = "totalTint",
    )
    Surface(
        shape = RoundedCornerShape(50),
        color = animatedContainer,
    ) {
        AnimatedContent(
            targetState = when {
                hidden -> "***"
                total != null -> formatTotal(total, currency)
                else -> "–"
            },
            transitionSpec = {
                (slideInVertically { height -> height / 2 } + fadeIn()) togetherWith
                    (slideOutVertically { height -> -height / 2 } + fadeOut())
            },
            label = "totalValue",
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = animatedTint,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
