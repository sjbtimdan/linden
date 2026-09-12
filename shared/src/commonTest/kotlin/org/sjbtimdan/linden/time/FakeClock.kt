package org.sjbtimdan.linden.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/** Fixed instant used across tests: 2026-01-20 00:00 UTC. */
val TEST_NOW: Instant = Instant.fromEpochMilliseconds(1_768_867_200_000)

/** Mutable fake clock; [todayIn] ignores the zone. */
class FakeClock(
    var now: Instant = TEST_NOW,
    var today: LocalDate = LocalDate(2026, 1, 20),
) : AppClock {
    override fun now(): Instant = now
    override fun todayIn(zone: TimeZone): LocalDate = today
}
