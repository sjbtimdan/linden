package org.sjbtimdan.linden.log

import java.util.concurrent.ConcurrentHashMap

/**
 * A throwable safe to hand to a crash reporter: the failure kind and the
 * original stack frames, but no message and no cause. Handled failures can
 * carry user data in their messages (serialization errors quote JSON, IO errors
 * carry the chosen file name), so only the exception type is kept.
 */
fun sanitizedFailure(failure: AppFailure, error: Throwable): Throwable =
    IllegalStateException("Linden ${failure.wireName} (${error.javaClass.simpleName})").apply {
        stackTrace = error.stackTrace
    }

/** Reports the first occurrence of each failure kind per process, then drops repeats. */
class DeduplicatingAppLogger(private val delegate: AppLogger) : AppLogger {
    private val reported = ConcurrentHashMap.newKeySet<AppFailure>()

    override fun report(failure: AppFailure, error: Throwable) {
        if (reported.add(failure)) delegate.report(failure, error)
    }
}

/** Desktop reporter: the full failure on the console, where there is no privacy concern. */
class ConsoleAppLogger(
    private val sink: (String) -> Unit = { println(it) },
) : AppLogger {
    override fun report(failure: AppFailure, error: Throwable) {
        sink("Linden: ${failure.wireName} failed: ${error.stackTraceToString()}")
    }
}
