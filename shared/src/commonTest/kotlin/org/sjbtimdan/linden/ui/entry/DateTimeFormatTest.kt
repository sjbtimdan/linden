package org.sjbtimdan.linden.ui.entry

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

class DateTimeFormatTest : StringSpec({
    "formats an instant as date and time in the given zone" {
        val instant = Instant.parse("2026-08-10T14:30:00Z")
        formatDateTime(instant, TimeZone.UTC) shouldBe "10 Aug 2026, 14:30"
    }

    "formats an instant in a non-UTC zone" {
        val instant = Instant.parse("2026-08-10T14:30:00Z")
        // Berlin is UTC+2 in August (CEST)
        formatDateTime(instant, TimeZone.of("Europe/Berlin")) shouldBe "10 Aug 2026, 16:30"
    }

    "pads single-digit hours and minutes" {
        val instant = Instant.parse("2026-01-05T09:05:00Z")
        formatTime(instant, TimeZone.UTC) shouldBe "09:05"
    }

    "formats a date separately" {
        val instant = Instant.parse("2026-12-25T00:00:00Z")
        formatDate(instant, TimeZone.UTC) shouldBe "25 Dec 2026"
    }

    "combines a date with a time in the given zone" {
        val utcMillis = Instant.parse("2026-08-10T00:00:00Z").toEpochMilliseconds()
        val result = combineDateAndTime(utcMillis, 9, 5, TimeZone.UTC)
        result shouldBe Instant.parse("2026-08-10T09:05:00Z")
    }

    "interprets the combined time in the given zone" {
        val utcMillis = Instant.parse("2026-08-10T00:00:00Z").toEpochMilliseconds()
        val result = combineDateAndTime(utcMillis, 9, 5, TimeZone.of("Europe/Berlin"))
        // Berlin is UTC+2 in August (CEST)
        result shouldBe Instant.parse("2026-08-10T07:05:00Z")
    }
})
