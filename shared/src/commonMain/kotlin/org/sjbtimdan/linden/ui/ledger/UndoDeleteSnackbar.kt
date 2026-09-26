package org.sjbtimdan.linden.ui.ledger

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.entry_deleted
import org.sjbtimdan.linden.resources.entry_undo

/**
 * Shows the undo offer for the entry most recently deleted from the edit
 * dialog: a snackbar with an Undo action and a dismiss button, up for
 * [SnackbarDuration.Long]. Undo re-inserts the entry; any other outcome —
 * dismissing, timing out, or the host leaving composition — makes the delete
 * final. Place next to the screen's [androidx.compose.material3.SnackbarHost].
 */
@Composable
fun LedgerViewModel.UndoDeleteSnackbar(hostState: SnackbarHostState) {
    val deleted by lastDeleted.collectAsState()
    val message = stringResource(Res.string.entry_deleted)
    val undoLabel = stringResource(Res.string.entry_undo)
    // Leaving the screen cancels showSnackbar without returning a result, so the
    // offer would otherwise stay alive and reappear on the next visit.
    DisposableEffect(Unit) {
        onDispose { clearLastDeleted() }
    }
    LaunchedEffect(deleted) {
        if (deleted == null) return@LaunchedEffect
        val result = hostState.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) undoLastDeleted() else clearLastDeleted()
    }
}
