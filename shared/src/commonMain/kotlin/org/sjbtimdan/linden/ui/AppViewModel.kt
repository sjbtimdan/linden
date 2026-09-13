package org.sjbtimdan.linden.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.sjbtimdan.linden.util.stateFlow

/**
 * Base for the app's ViewModels: collects a flow eagerly into a [StateFlow]
 * owned by the ViewModel scope, so subclasses write `flow.stateFlow(initial)`
 * instead of repeating the `stateIn` wiring.
 */
abstract class AppViewModel : ViewModel() {
    protected fun <T> Flow<T>.stateFlow(initial: T): StateFlow<T> = stateFlow(viewModelScope, initial)
}
