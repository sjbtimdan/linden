package org.sjbtimdan.linden.ui.ledger

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class LedgerViewModeTest : StringSpec({
    "moving to a later mode slides forward" {
        viewModeTravel(LedgerViewMode.Entries, LedgerViewMode.Accounts) shouldBe 1
        viewModeTravel(LedgerViewMode.Accounts, LedgerViewMode.Categories) shouldBe 1
    }

    "moving back to an earlier mode slides backward" {
        viewModeTravel(LedgerViewMode.Categories, LedgerViewMode.Accounts) shouldBe -1
        viewModeTravel(LedgerViewMode.Accounts, LedgerViewMode.Entries) shouldBe -1
    }

    "staying on the same mode does not slide" {
        LedgerViewMode.entries.forEach { mode ->
            viewModeTravel(mode, mode) shouldBe 0
        }
    }
})
