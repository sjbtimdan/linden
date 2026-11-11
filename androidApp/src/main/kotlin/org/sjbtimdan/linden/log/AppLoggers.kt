package org.sjbtimdan.linden.log

import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.sjbtimdan.linden.BuildInfo

/**
 * One reporter for the process. The activity and the capture listener share
 * this instance so the dedupe set covers both; each failure kind reaches
 * Crashlytics at most once per app run.
 */
val appLogger: AppLogger = DeduplicatingAppLogger(CrashlyticsAppLogger())

private class CrashlyticsAppLogger : AppLogger {
    override fun report(failure: AppFailure, error: Throwable) {
        try {
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("failure", failure.wireName)
                setCustomKey("version", BuildInfo.VERSION)
                setCustomKey("commit", BuildInfo.GIT_COMMIT)
                recordException(sanitizedFailure(failure, error))
            }
        } catch (_: Exception) {
            // Reporting must never become a failure of its own (e.g. Firebase not initialised).
        }
    }
}
