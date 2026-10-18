package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.pace_after_this

/** One-line Pace feedback under the amount field after an expense is entered. */
@Composable
fun PaceCaption(pace: PaceAfterThis, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(
            Res.string.pace_after_this,
            amountLabel(pace.dailyAllowanceMinor, pace.currency),
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .testTag("paceAfterThis"),
    )
}

private fun amountLabel(amount: Long, currency: Currency): String =
    "${formatAmountCompact(amount, currency.decimalDigits)} ${currency.symbol}"
