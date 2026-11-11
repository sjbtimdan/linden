package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * How long the last-added receipt stays as an undo offer before it dismisses
 * itself; after that the add is final.
 */
internal val LAST_ADDED_UNDO_WINDOW = 5.seconds

@Composable
internal fun LastAddedRegion(viewModel: EntryPointViewModel, onTouched: () -> Unit) {
    val lastAdded by viewModel.lastAdded.collectAsState()
    val lastAddedWasEdit by viewModel.lastAddedWasEdit.collectAsState()
    val hideTotal by viewModel.hideTotal.collectAsState()

    // The receipt dismisses itself after a short window, after which the add is final.
    LaunchedEffect(lastAdded?.id) {
        if (lastAdded != null) {
            delay(LAST_ADDED_UNDO_WINDOW)
            viewModel.dismissLastAdded()
        }
    }

    lastAdded?.let { entry ->
        LastAddedEntry(
            entry = entry,
            hideAmounts = hideTotal,
            updated = lastAddedWasEdit,
            onEdit = {
                viewModel.editLastAdded()
                onTouched()
            },
            onUndo = {
                viewModel.undoLastAdded()
                onTouched()
            },
            onDismiss = viewModel::dismissLastAdded,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
