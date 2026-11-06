package org.sjbtimdan.linden.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_dismiss
import org.sjbtimdan.linden.resources.common_unknown_error

/** Renders the inline working / success / error feedback of a settings operation. */
@Composable
internal fun <T> OperationStatus(
    state: BackupState<T>,
    workingText: String,
    successText: @Composable (T) -> String,
    failureText: @Composable (String) -> String,
    onDismiss: () -> Unit,
) {
    when (state) {
        BackupState.Idle -> Unit

        BackupState.Working -> WorkingRow(text = workingText)

        is BackupState.Success -> OperationResultRow(text = successText(state.value), onDismiss = onDismiss)

        is BackupState.Error -> {
            val message = state.message ?: stringResource(Res.string.common_unknown_error)
            OperationResultRow(text = failureText(message), onDismiss = onDismiss)
        }
    }
}

@Composable
private fun WorkingRow(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun OperationResultRow(text: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = onDismiss,
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(Res.string.common_dismiss),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}
