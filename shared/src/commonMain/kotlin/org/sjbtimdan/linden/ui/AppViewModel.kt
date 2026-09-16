package org.sjbtimdan.linden.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.sjbtimdan.linden.util.stateFlow

/**
 * Base for the app's ViewModels: collects a flow eagerly into a [StateFlow]
 * owned by the ViewModel scope, so subclasses write `flow.stateFlow(initial)`
 * instead of repeating the `stateIn` wiring.
 */
abstract class AppViewModel : ViewModel() {
    protected fun <T> Flow<T>.stateFlow(initial: T): StateFlow<T> = stateFlow(viewModelScope, initial)

    private val _error = MutableStateFlow<String?>(null)

    /** The last failed write's exception message, or null once consumed. */
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Consumes the error so a later failure can be shown again. */
    fun clearError() {
        _error.value = null
    }

    /** Records a failed write for the screen to surface; [message] is the exception's message, if any. */
    protected fun reportError(message: String?) {
        // An empty string stands for "failed without a message" so the error is
        // still surfaced (the UI falls back to a generic copy).
        _error.value = message ?: ""
    }
}
