package org.sjbtimdan.linden.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_write_failed

/**
 * Shows a friendly message when the ViewModel's latest write failed and consumes
 * the failure, so a later one can be shown again. Raw exception text is
 * deliberately never rendered. Place once per screen that hosts CRUD, next to
 * its [androidx.compose.material3.SnackbarHost].
 */
@Composable
fun AppViewModel.ErrorSnackbar(hostState: SnackbarHostState) {
    val error by error.collectAsState()
    val message = stringResource(Res.string.common_write_failed)
    LaunchedEffect(error) {
        if (error) {
            hostState.showSnackbar(message)
            clearError()
        }
    }
}
