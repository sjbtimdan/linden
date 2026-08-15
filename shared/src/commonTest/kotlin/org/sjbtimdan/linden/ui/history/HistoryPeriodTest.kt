package org.sjbtimdan.linden.ui.history

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import org.sjbtimdan.linden.ui.history.HistoryPeriod.All
import org.sjbtimdan.linden.ui.history.HistoryPeriod.Day
import org.sjbtimdan.linden.ui.history.HistoryPeriod.Month
import org.sjbtimdan.linden.ui.history.HistoryPeriod.Week
import org.sjbtimdan.linden.ui.history.HistoryPeriod.Year

class HistoryPeriodTest : StringSpec({
    "day window is the anchor day" {
        Day.windowStart(LocalDate(2026, 8, 13)) shouldBe LocalDate(2026, 8, 13)
        Day.windowEnd(LocalDate(2026, 8, 13)) shouldBe LocalDate(2026, 8, 13)
    }

    "day navigation moves by one day" {
        Day.nextAnchor(LocalDate(2026, 8, 13)) shouldBe LocalDate(2026, 8, 14)
        Day.previousAnchor(LocalDate(2026, 8, 13)) shouldBe LocalDate(2026, 8, 12)
    }

    "day label shows the date" {
        Day.windowLabel(LocalDate(2026, 8, 13)) shouldBe "13 Aug 2026"
    }

    "week window is Monday to Sunday around the anchor" {
        Week.windowStart(LocalDate(2026, 8, 13)) shouldBe LocalDate(2026, 8, 10)
        Week.windowEnd(LocalDate(2026, 8, 13)) shouldBe LocalDate(2026, 8, 16)
    }

    "week window keeps a Monday anchor unchanged" {
        Week.windowStart(LocalDate(2026, 8, 10)) shouldBe LocalDate(2026, 8, 10)
    }

    "week window spans a month boundary" {
        Week.windowStart(LocalDate(2026, 9, 2)) shouldBe LocalDate(2026, 8, 31)
        Week.windowEnd(LocalDate(2026, 9, 2)) shouldBe LocalDate(2026, 9, 6)
    }

    "month window is the calendar month of the anchor" {
        Month.windowStart(LocalDate(2026, 8, 15)) shouldBe LocalDate(2026, 8, 1)
        Month.windowEnd(LocalDate(2026, 8, 15)) shouldBe LocalDate(2026, 8, 31)
    }

    "month window respects month lengths" {
        Month.windowEnd(LocalDate(2026, 4, 10)) shouldBe LocalDate(2026, 4, 30)
        Month.windowEnd(LocalDate(2026, 2, 10)) shouldBe LocalDate(2026, 2, 28)
        Month.windowEnd(LocalDate(2024, 2, 10)) shouldBe LocalDate(2024, 2, 29)
    }

    "year window is the calendar year of the anchor" {
        Year.windowStart(LocalDate(2026, 6, 1)) shouldBe LocalDate(2026, 1, 1)
        Year.windowEnd(LocalDate(2026, 6, 1)) shouldBe LocalDate(2026, 12, 31)
    }

    "All has no window" {
        All.windowStart(LocalDate(2026, 8, 13)).shouldBeNull()
        All.windowEnd(LocalDate(2026, 8, 13)).shouldBeNull()
    }

    "week navigation moves by seven days" {
        Week.nextAnchor(LocalDate(2026, 8, 13)) shouldBe LocalDate(2026, 8, 20)
        Week.previousAnchor(LocalDate(2026, 8, 13)) shouldBe LocalDate(2026, 8, 6)
    }

    "month navigation rolls over year boundaries" {
        Month.nextAnchor(LocalDate(2026, 12, 15)) shouldBe LocalDate(2027, 1, 15)
        Month.previousAnchor(LocalDate(2026, 1, 15)) shouldBe LocalDate(2025, 12, 15)
    }

    "month navigation clamps to month end" {
        Month.nextAnchor(LocalDate(2024, 1, 31)) shouldBe LocalDate(2024, 2, 29)
        Month.previousAnchor(LocalDate(2024, 3, 31)) shouldBe LocalDate(2024, 2, 29)
    }

    "year navigation keeps the calendar day" {
        Year.nextAnchor(LocalDate(2026, 3, 1)) shouldBe LocalDate(2027, 3, 1)
        Year.previousAnchor(LocalDate(2026, 3, 1)) shouldBe LocalDate(2025, 3, 1)
    }

    "All navigation leaves the anchor unchanged" {
        All.nextAnchor(LocalDate(2026, 8, 13)) shouldBe LocalDate(2026, 8, 13)
        All.previousAnchor(LocalDate(2026, 8, 13)) shouldBe LocalDate(2026, 8, 13)
    }

    "week label shows a single month range" {
        Week.windowLabel(LocalDate(2026, 8, 13)) shouldBe "10–16 Aug 2026"
    }

    "week label spans months" {
        Week.windowLabel(LocalDate(2026, 9, 2)) shouldBe "31 Aug – 6 Sep 2026"
    }

    "week label spans years" {
        Week.windowLabel(LocalDate(2026, 12, 31)) shouldBe "28 Dec 2026 – 3 Jan 2027"
    }

    "month and year labels" {
        Month.windowLabel(LocalDate(2026, 8, 13)) shouldBe "Aug 2026"
        Year.windowLabel(LocalDate(2026, 8, 13)) shouldBe "2026"
    }

    "All has no label" {
        All.windowLabel(LocalDate(2026, 8, 13)).shouldBeNull()
    }
})
