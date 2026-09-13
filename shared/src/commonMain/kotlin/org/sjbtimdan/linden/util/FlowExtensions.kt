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

/**
 * Collects this flow eagerly into a [StateFlow] owned by [scope], seeded with
 * [initial]. Eager sharing means the upstream starts as soon as the scope is
 * alive, so a screen collecting later never misses a value or shows a stale one.
 */
fun <T> Flow<T>.stateFlow(scope: CoroutineScope, initial: T): StateFlow<T> =
    stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = initial)

/** Emits this query's rows whenever the result set changes, awaiting the async driver's result. */
fun <T : Any> Query<T>.asListFlow(): Flow<List<T>> = asFlow().map { it.awaitAsList() }

/** Emits this query's rows mapped through [mapper] whenever the result set changes. */
fun <T : Any, R> Query<T>.asListFlow(mapper: (T) -> R): Flow<List<R>> =
    asFlow().map { rows -> rows.awaitAsList().map(mapper) }

/** Emits this query's single row (or null when it yields none) whenever the result set changes. */
fun <T : Any> Query<T>.asOneOrNullFlow(): Flow<T?> = asFlow().map { it.awaitAsOneOrNull() }
