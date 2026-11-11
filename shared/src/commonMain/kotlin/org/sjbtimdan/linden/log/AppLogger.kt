package org.sjbtimdan.linden.log

/**
 * A handled failure worth seeing after release. The stable [wireName] becomes a
 * crash-report key, so one kind of failure produces one issue instead of one
 * issue per message.
 */
enum class AppFailure(val wireName: String) {
    Startup("startup"),
    WriteFailed("write"),
    BackupFailed("backup"),
    RestoreFailed("restore"),
    ExportFailed("export"),
    ImportFailed("import"),
    CaptureWriteFailed("capture_write"),
    CaptureDbOpenFailed("capture_db_open"),
}

/**
 * Reports handled failures so they are visible outside the UI. Implementations
 * must never throw: reporting a failure must not become one.
 */
fun interface AppLogger {
    fun report(failure: AppFailure, error: Throwable)
}

/** Silent default, used by tests and by builds without a reporter. */
object NoOpAppLogger : AppLogger {
    override fun report(failure: AppFailure, error: Throwable) = Unit
}
