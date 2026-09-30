package org.sjbtimdan.linden.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.sjbtimdan.linden.ui.theme.DialogShape

/** Confirmation dialog: [confirmLabel] runs [onConfirm], dismissing runs [onDismiss]. */
@Composable
fun ConfirmDialog(title: String, body: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton = { DialogCancelButton(onDismiss) },
    )
}
