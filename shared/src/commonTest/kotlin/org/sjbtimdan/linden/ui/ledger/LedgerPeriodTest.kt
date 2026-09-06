package org.sjbtimdan.linden.ui.ledger

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import org.sjbtimdan.linden.ui.entry.DateLanguage
import org.sjbtimdan.linden.ui.ledger.LedgerPeriod.All
import org.sjbtimdan.linden.ui.ledger.LedgerPeriod.Day
import org.sjbtimdan.linden.ui.ledger.LedgerPeriod.Month
import org.sjbtimdan.linden.ui.ledger.LedgerPeriod.Week
import org.sjbtimdan.linden.ui.ledger.LedgerPeriod.Year

class LedgerPeriodTest : StringSpec({
    "day window is the anchor day" {
        Day.windowStart(LocalDate(2026, 8, 13)) shouldBe LocalDate(2026, 8, 13)
        Day.windowEnd(LocalDate(2026, 8, 13)) shouldBe LocalDate(2026, 8, 13)
    }

    "day navigation moves by one day" {
        Day.nextAnchor(LocalDate(2026, 8, 13)) shouldBe LocalDate(2026, 8, 14)
        Day.previousAnchor(LocalDate(2026, 8, 13)) shouldBe LocalDate(2026, 8, 12)
    }

    "day label shows the localized date" {
        Day.windowLabel(LocalDate(2026, 8, 13), DateLanguage.English) shouldBe "Aug 13, 2026"
        Day.windowLabel(LocalDate(2026, 8, 13), DateLanguage.Italian) shouldBe "13 ago 2026"
        Day.windowLabel(LocalDate(2026, 8, 13), DateLanguage.Chinese) shouldBe "2026年8月13日"
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

    "week label shows a single month range in English" {
        Week.windowLabel(LocalDate(2026, 8, 13), DateLanguage.English) shouldBe "Aug 10–16, 2026"
    }

    "week label spans months in English" {
        Week.windowLabel(LocalDate(2026, 9, 2), DateLanguage.English) shouldBe "Aug 31 – Sep 6, 2026"
    }

    "week label spans years in English" {
        Week.windowLabel(LocalDate(2026, 12, 31), DateLanguage.English) shouldBe "Dec 28, 2026 – Jan 3, 2027"
    }

    "week label shows a single month range in Italian" {
        Week.windowLabel(LocalDate(2026, 8, 13), DateLanguage.Italian) shouldBe "10–16 ago 2026"
    }

    "week label spans months in Italian" {
        Week.windowLabel(LocalDate(2026, 9, 2), DateLanguage.Italian) shouldBe "31 ago – 6 set 2026"
    }

    "week label spans years in Italian" {
        Week.windowLabel(LocalDate(2026, 12, 31), DateLanguage.Italian) shouldBe "28 dic 2026 – 3 gen 2027"
    }

    "week label shows a single month range in Chinese" {
        Week.windowLabel(LocalDate(2026, 8, 13), DateLanguage.Chinese) shouldBe "2026年8月10日–16日"
    }

    "week label spans months in Chinese" {
        Week.windowLabel(LocalDate(2026, 9, 2), DateLanguage.Chinese) shouldBe "2026年8月31日 – 9月6日"
    }

    "week label spans years in Chinese" {
        Week.windowLabel(LocalDate(2026, 12, 31), DateLanguage.Chinese) shouldBe "2026年12月28日 – 2027年1月3日"
    }

    "month and year labels follow the language" {
        Month.windowLabel(LocalDate(2026, 8, 13), DateLanguage.English) shouldBe "Aug 2026"
        Month.windowLabel(LocalDate(2026, 8, 13), DateLanguage.Italian) shouldBe "ago 2026"
        Month.windowLabel(LocalDate(2026, 8, 13), DateLanguage.Chinese) shouldBe "2026年8月"
        Year.windowLabel(LocalDate(2026, 8, 13), DateLanguage.English) shouldBe "2026"
        Year.windowLabel(LocalDate(2026, 8, 13), DateLanguage.Chinese) shouldBe "2026"
    }

    "All has no label" {
        All.windowLabel(LocalDate(2026, 8, 13), DateLanguage.English).shouldBeNull()
    }

    "All includes every date" {
        All.includes(LocalDate(2026, 8, 13), LocalDate(2026, 8, 13)) shouldBe true
        All.includes(LocalDate(2020, 1, 1), LocalDate(2026, 8, 13)) shouldBe true
    }

    "day includes only its own day" {
        Day.includes(LocalDate(2026, 8, 13), LocalDate(2026, 8, 13)) shouldBe true
        Day.includes(LocalDate(2026, 8, 14), LocalDate(2026, 8, 13)) shouldBe false
        Day.includes(LocalDate(2026, 8, 12), LocalDate(2026, 8, 13)) shouldBe false
    }

    "week includes dates up to its end" {
        Week.includes(LocalDate(2026, 8, 13), LocalDate(2026, 8, 13)) shouldBe true
        Week.includes(LocalDate(2026, 8, 16), LocalDate(2026, 8, 13)) shouldBe true
        Week.includes(LocalDate(2026, 8, 17), LocalDate(2026, 8, 13)) shouldBe false
    }

    "month includes dates up to its end" {
        Month.includes(LocalDate(2026, 8, 15), LocalDate(2026, 8, 15)) shouldBe true
        Month.includes(LocalDate(2026, 8, 31), LocalDate(2026, 8, 15)) shouldBe true
        Month.includes(LocalDate(2026, 9, 1), LocalDate(2026, 8, 15)) shouldBe false
    }

    "year includes dates up to its end" {
        Year.includes(LocalDate(2026, 6, 1), LocalDate(2026, 6, 1)) shouldBe true
        Year.includes(LocalDate(2026, 12, 31), LocalDate(2026, 6, 1)) shouldBe true
        Year.includes(LocalDate(2027, 1, 1), LocalDate(2026, 6, 1)) shouldBe false
    }
})
