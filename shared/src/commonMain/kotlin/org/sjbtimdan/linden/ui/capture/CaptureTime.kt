package org.sjbtimdan.linden.ui.capture

import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.capture.PendingCapture
import org.sjbtimdan.linden.ui.entry.formatDate
import org.sjbtimdan.linden.ui.entry.formatTime
import kotlin.time.Instant

/**
 * When the notification was posted. The entry added from a capture is dated
 * from this and not from the confirmation, so a payment read the next morning
 * still lands on the day it happened.
 */
fun captureInstant(capture: PendingCapture): Instant = Instant.fromEpochMilliseconds(capture.capturedAtMillis)

/**
 * The zone the capture was recorded in — the device's zone when the
 * notification arrived, so an entry confirmed after travelling still lands on
 * the day it happened. Older captures carry no zone and fall back to the
 * current one, as does an unreadable id.
 */
fun captureZone(capture: PendingCapture): TimeZone {
    val id = capture.capturedAtZoneId ?: return TimeZone.currentSystemDefault()
    return try {
        TimeZone.of(id)
    } catch (_: IllegalArgumentException) {
        TimeZone.currentSystemDefault()
    }
}

/** A capture's post time for the inbox row, e.g. "Aug 10, 2026 14:30". */
fun formatCapturedAt(instant: Instant, zone: TimeZone): String {
    val date = formatDate(instant, zone)
    return "$date ${formatTime(instant, zone)}"
}
