package org.sjbtimdan.linden.ui.ledger

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** Formats an instant as "10 Aug 2026, 14:30" in the given zone. */
fun formatDateTime(instant: Instant, zone: TimeZone): String =
    "${formatDate(instant, zone)}, ${formatTime(instant, zone)}"

/** Formats an instant's date as "10 Aug 2026" in the given zone. */
fun formatDate(instant: Instant, zone: TimeZone): String {
    val local = instant.toLocalDateTime(zone)
    return "${local.day} ${MONTHS[local.month.number - 1]} ${local.year}"
}

/** Formats an instant's time as "14:30" in the given zone. */
fun formatTime(instant: Instant, zone: TimeZone): String {
    val local = instant.toLocalDateTime(zone)
    return "${pad(local.hour)}:${pad(local.minute)}"
}

/**
 * Combines a date (UTC-midnight millis, as produced by the Material3 DatePicker) with an
 * existing time-of-day, interpreted in [zone].
 */
fun combineDateAndTime(dateUtcMillis: Long, hour: Int, minute: Int, zone: TimeZone): Instant {
    val date = Instant.fromEpochMilliseconds(dateUtcMillis).toLocalDateTime(TimeZone.UTC).date
    return LocalDateTime(date.year, date.month.number, date.day, hour, minute).toInstant(zone)
}

private fun pad(value: Int): String = value.toString().padStart(2, '0')
