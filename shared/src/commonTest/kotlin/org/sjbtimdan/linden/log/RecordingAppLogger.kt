package org.sjbtimdan.linden.log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Captures reported failures so tests can assert on the kind. */
class RecordingAppLogger : AppLogger {
    private val _reports = MutableStateFlow<List<AppFailure>>(emptyList())

    val reports: List<AppFailure> get() = _reports.value

    override fun report(failure: AppFailure, error: Throwable) {
        _reports.update { it + failure }
    }
}
