package org.sjbtimdan.linden.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_operation_failed
import org.sjbtimdan.linden.resources.common_unknown_error

/**
 * Shows the ViewModel's latest failed-write error on [hostState] and consumes
 * it, so a later failure can be shown again. Place once per screen that hosts
 * CRUD, next to its [androidx.compose.material3.SnackbarHost].
 */
@Composable
fun AppViewModel.ErrorSnackbar(hostState: SnackbarHostState) {
    val error by error.collectAsState()
    val unknownError = stringResource(Res.string.common_unknown_error)
    val message = error?.let { stringResource(Res.string.common_operation_failed, it.ifEmpty { unknownError }) }
    LaunchedEffect(error) {
        if (error != null && message != null) {
            hostState.showSnackbar(message)
            clearError()
        }
    }
}
