package org.sjbtimdan.linden.util

import app.cash.sqldelight.Query
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** How long a shared flow keeps running after its last collector leaves. */
private const val STOP_TIMEOUT_MILLIS = 5_000L

private val ScreenStateSharing = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS)

/**
 * Test-only override for [DefaultStateFlowStarted]; production leaves it null.
 * The unit-test harness sets [SharingStarted.Eagerly] so tests can keep reading
 * `StateFlow.value` without collecting; sharing itself is covered directly by
 * `FlowExtensionsTest`.
 */
internal var stateFlowStartedOverrideForTest: SharingStarted? = null

/**
 * Default sharing for UI state: start on the first collector, stop 5 s after
 * the last one leaves. The grace period lets a rotation or a quick screen
 * switch hand the flow over to the next collector without restarting it.
 */
internal val DefaultStateFlowStarted: SharingStarted
    get() = stateFlowStartedOverrideForTest ?: ScreenStateSharing

/**
 * Shares this flow as a [StateFlow] owned by [scope], seeded with [initial].
 *
 * Callers that need the value to stay current before anyone collects (settings
 * chrome or logic that reads `StateFlow.value` directly) pass
 * [SharingStarted.Eagerly] instead of the default.
 */
fun <T> Flow<T>.stateFlow(
    scope: CoroutineScope,
    initial: T,
    started: SharingStarted = DefaultStateFlowStarted,
): StateFlow<T> = stateIn(scope = scope, started = started, initialValue = initial)

/** Emits this query's rows whenever the result set changes, awaiting the async driver's result. */
fun <T : Any> Query<T>.asListFlow(): Flow<List<T>> = asFlow().map { it.awaitAsList() }

/** Emits this query's rows mapped through [mapper] whenever the result set changes. */
fun <T : Any, R> Query<T>.asListFlow(mapper: (T) -> R): Flow<List<R>> =
    asFlow().map { rows -> rows.awaitAsList().map(mapper) }

/** Emits this query's single row (or null when it yields none) whenever the result set changes. */
fun <T : Any> Query<T>.asOneOrNullFlow(): Flow<T?> = asFlow().map { it.awaitAsOneOrNull() }
