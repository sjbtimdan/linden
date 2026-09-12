package org.sjbtimdan.linden.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Injectable clock so time reads are deterministic in tests.
 * Named AppClock to avoid clashing with kotlin.time.Clock.
 */
interface AppClock {
    fun now(): Instant
    fun todayIn(zone: TimeZone): LocalDate
}

object SystemClock : AppClock {
    override fun now(): Instant = Clock.System.now()
    override fun todayIn(zone: TimeZone): LocalDate = Clock.System.todayIn(zone)
}
