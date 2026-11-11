package org.sjbtimdan.linden.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.log.AppFailure
import org.sjbtimdan.linden.log.AppLogger
import org.sjbtimdan.linden.log.NoOpAppLogger
import org.sjbtimdan.linden.util.DefaultStateFlowStarted
import org.sjbtimdan.linden.util.stateFlow

/**
 * Base for the app's ViewModels: shares a flow as a [StateFlow] owned by the
 * ViewModel scope, so subclasses write `flow.stateFlow(initial)` instead of
 * repeating the `stateIn` wiring. The upstream runs only while a screen (or
 * another flow) collects it, plus a short stop-timeout grace.
 */
abstract class AppViewModel(protected val appLogger: AppLogger = NoOpAppLogger) : ViewModel() {
    protected fun <T> Flow<T>.stateFlow(initial: T, started: SharingStarted = DefaultStateFlowStarted): StateFlow<T> =
        stateFlow(viewModelScope, initial, started)

    /**
     * Runs [block] in [viewModelScope], logging any failure and reporting it through
     * [reportError]. [CancellationException] is rethrown, so cancellation is not
     * reported as a write failure.
     */
    protected fun launchWrite(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                appLogger.report(AppFailure.WriteFailed, e)
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
