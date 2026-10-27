package org.sjbtimdan.linden.ui.capture

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.capture.PendingCapture
import org.sjbtimdan.linden.model.EntryType
import kotlin.time.Instant

private val CAPTURED_AT = Instant.parse("2026-08-10T14:30:00Z")

private fun capture(millis: Long = CAPTURED_AT.toEpochMilliseconds(), zoneId: String? = null) = PendingCapture(
    packageName = "com.example.bank",
    amountMinor = 2450,
    type = EntryType.Expense,
    capturedAtMillis = millis,
    capturedAtZoneId = zoneId,
)

class CaptureTimeTest : StringSpec({

    "captureInstant reads the notification post time" {
        captureInstant(capture()) shouldBe CAPTURED_AT
        captureInstant(capture(millis = 0L)) shouldBe Instant.fromEpochMilliseconds(0L)
    }

    "captureZone keeps the recorded zone" {
        captureZone(capture(zoneId = "Asia/Tokyo")) shouldBe TimeZone.of("Asia/Tokyo")
    }

    "captureZone falls back to the current zone for a capture without one" {
        captureZone(capture(zoneId = null)) shouldBe TimeZone.currentSystemDefault()
    }

    "captureZone falls back to the current zone for an unreadable id" {
        captureZone(capture(zoneId = "Not/AZone")) shouldBe TimeZone.currentSystemDefault()
    }

    "formatCapturedAt prints the date and time in the capture's zone" {
        formatCapturedAt(CAPTURED_AT, TimeZone.UTC) shouldBe "Aug 10, 2026 14:30"
    }

    "formatCapturedAt shifts with the zone, so the payment's day survives a flight" {
        // 23:30 UTC is already the next morning in Tokyo.
        formatCapturedAt(Instant.parse("2026-08-10T23:30:00Z"), TimeZone.of("Asia/Tokyo")) shouldBe
            "Aug 11, 2026 08:30"
    }
})
