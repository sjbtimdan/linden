package org.sjbtimdan.linden.ui.ledger

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FutureEntriesNoticeTest : StringSpec({
    "entries label counts one upcoming entry" {
        futureEntriesNoticeLabel(LedgerViewMode.Entries, upcoming = 1, bounded = true) shouldBe
            "Showing 1 entry after today"
    }

    "entries label counts several upcoming entries" {
        futureEntriesNoticeLabel(LedgerViewMode.Entries, upcoming = 3, bounded = true) shouldBe
            "Showing 3 entries after today"
    }

    "entries label counts every future entry without a window" {
        futureEntriesNoticeLabel(LedgerViewMode.Entries, upcoming = 2, bounded = false) shouldBe
            "Showing 2 entries after today"
    }

    "accounts label names balances" {
        futureEntriesNoticeLabel(LedgerViewMode.Accounts, upcoming = 2, bounded = true) shouldBe
            "Balances include entries after today"
    }

    "categories label names totals" {
        futureEntriesNoticeLabel(LedgerViewMode.Categories, upcoming = 2, bounded = true) shouldBe
            "Totals include entries after today"
    }

    "no upcoming entries names the period when there is one" {
        futureEntriesNoticeLabel(LedgerViewMode.Entries, upcoming = 0, bounded = true) shouldBe
            "No entries after today in this period"
    }

    "no upcoming entries stays short without a window" {
        futureEntriesNoticeLabel(LedgerViewMode.Accounts, upcoming = 0, bounded = false) shouldBe
            "No entries after today"
    }
})
