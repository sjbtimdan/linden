package org.sjbtimdan.linden.ui.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.capture.PendingCapture
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.capture_account_missing
import org.sjbtimdan.linden.resources.capture_collapse
import org.sjbtimdan.linden.resources.capture_dismiss
import org.sjbtimdan.linden.resources.capture_pending_count
import org.sjbtimdan.linden.resources.capture_pending_title
import org.sjbtimdan.linden.resources.capture_row_supporting
import org.sjbtimdan.linden.resources.entry_add
import org.sjbtimdan.linden.ui.theme.CardElevation
import org.sjbtimdan.linden.ui.theme.CardShape

/**
 * The captured-payment inbox pinned above the ledger's entries list: an
 * expanded card listing every capture awaiting review, or — once dismissed —
 * a one-line bar that opens it again. A capture newer than the acknowledged
 * watermark expands the card on its own.
 *
 * The notification listener writes through its own database connection, which
 * the repository flows cannot observe, so the panel re-reads on resume.
 */
@Composable
fun CapturedPaymentsBanner(
    viewModel: CapturedPaymentsViewModel,
    onAdd: (PendingCapture) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items by viewModel.items.collectAsState()
    val hasUnseen by viewModel.hasUnseen.collectAsState()
    // Reopening lasts until the next dismissal; a new capture opens the card anyway.
    var reopened by rememberSaveable { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { viewModel.refresh() }

    if (items.isEmpty()) return

    if (hasUnseen || reopened) {
        CapturedPaymentsCard(
            items = items,
            onAdd = onAdd,
            onDismissCapture = viewModel::remove,
            onCollapse = {
                reopened = false
                viewModel.acknowledge()
            },
            modifier = modifier,
        )
    } else {
        CapturedPaymentsBar(
            count = items.size,
            onExpand = { reopened = true },
            modifier = modifier,
        )
    }
}

/** The collapsed state: one tappable line that opens the card again. */
@Composable
private fun CapturedPaymentsBar(count: Int, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onExpand)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("capturedPaymentsBar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = pluralStringResource(Res.plurals.capture_pending_count, count, count),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The expanded inbox: one row per capture, capped in height for long lists. */
@Composable
private fun CapturedPaymentsCard(
    items: List<CapturedPaymentItem>,
    onAdd: (PendingCapture) -> Unit,
    onDismissCapture: (Long) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val capturableApps = rememberCapturableApps()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(CardElevation, CardShape, clip = false)
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CardShape)
            .padding(12.dp)
            .testTag("capturedPaymentsCard"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.capture_pending_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onCollapse) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.capture_collapse),
                )
            }
        }
        Column(
            modifier = Modifier
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            items.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider()
                val label = capturableApps.firstOrNull { it.packageName == item.capture.packageName }?.label
                    ?: item.capture.packageName
                val accountText = item.accountName ?: stringResource(Res.string.capture_account_missing)
                CapturedPaymentRow(
                    item = item,
                    label = label,
                    supporting = stringResource(Res.string.capture_row_supporting, label, accountText),
                    onAdd = { onAdd(item.capture) },
                    onDismiss = { onDismissCapture(item.capture.id) },
                )
            }
        }
    }
}

@Composable
private fun CapturedPaymentRow(
    item: CapturedPaymentItem,
    label: String,
    supporting: String,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.capture.description.ifBlank { label },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = item.amountText, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.capture_dismiss))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Button(onClick = onAdd) {
                Text(stringResource(Res.string.entry_add))
            }
        }
    }
}
