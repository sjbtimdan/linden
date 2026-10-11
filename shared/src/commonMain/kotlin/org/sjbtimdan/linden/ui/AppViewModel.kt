package org.sjbtimdan.linden.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.util.stateFlow

/**
 * Base for the app's ViewModels: collects a flow eagerly into a [StateFlow]
 * owned by the ViewModel scope, so subclasses write `flow.stateFlow(initial)`
 * instead of repeating the `stateIn` wiring.
 */
abstract class AppViewModel : ViewModel() {
    protected fun <T> Flow<T>.stateFlow(initial: T): StateFlow<T> = stateFlow(viewModelScope, initial)

    /**
     * Runs [block] in [viewModelScope], reporting any failure through [reportError].
     * [CancellationException] is rethrown, so cancellation is not reported as a write
     * failure.
     */
    protected fun launchWrite(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                reportError()
            }
        }
    }

    private val _error = MutableStateFlow(false)

    /** True while a failed write awaits surfacing; the screen shows a localized message. */
    val error: StateFlow<Boolean> = _error.asStateFlow()

    /** Consumes the error so a later failure can be shown again. */
    fun clearError() {
        _error.value = false
    }

    /** Records a failed write; the failure carries no copy, the UI resolves it. */
    protected fun reportError() {
        _error.value = true
    }
}
