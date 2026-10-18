package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.ledger_day_total
import org.sjbtimdan.linden.ui.entry.HIDDEN_AMOUNT
import org.sjbtimdan.linden.ui.theme.lindenColors

@Composable
fun DayHeader(
    label: String,
    total: Long?,
    currency: Currency,
    hidden: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = lindenColors()
    val totalColor = when {
        hidden || total == null -> MaterialTheme.colorScheme.onSurfaceVariant
        total < 0 -> colors.expense
        total > 0 -> colors.income
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val totalText = when {
        hidden -> stringResource(Res.string.ledger_day_total, HIDDEN_AMOUNT)
        total != null -> stringResource(Res.string.ledger_day_total, formatTotal(total, currency))
        else -> stringResource(Res.string.ledger_day_total, "–")
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.testTag("dayHeader"),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = totalText,
            style = MaterialTheme.typography.labelLarge,
            color = totalColor,
            maxLines = 1,
            modifier = Modifier.testTag("dayHeaderTotal"),
        )
    }
}
