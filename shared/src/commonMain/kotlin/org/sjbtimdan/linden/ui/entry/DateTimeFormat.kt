package org.sjbtimdan.linden.ui.entry

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Formats an instant as a localized date plus "HH:mm" in [zone], e.g.
 * "Aug 10, 2026, 14:30" for English. The date half follows [DateLanguage].
 */
fun formatDateTime(instant: Instant, zone: TimeZone): String =
    "${formatDate(instant, zone)}, ${formatTime(instant, zone)}"

/** Formats an instant's date in [zone] per the active [DateLanguage], e.g. "Aug 10, 2026". */
fun formatDate(instant: Instant, zone: TimeZone): String = formatDate(instant, zone, dateLanguage(platformLocaleTag()))

/** Language-explicit variant of [formatDate], for callers that already resolved the language. */
internal fun formatDate(instant: Instant, zone: TimeZone, language: DateLanguage): String {
    val local = instant.toLocalDateTime(zone)
    return language.dateText(local.day, local.month.number, local.year)
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
