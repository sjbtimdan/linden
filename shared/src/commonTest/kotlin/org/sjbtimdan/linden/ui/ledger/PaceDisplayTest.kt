package org.sjbtimdan.linden.ui.ledger

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PaceDisplayTest : StringSpec({

    "formats a positive delta with a plus sign" {
        formatPaceDeltaPercent(projected = 1_100, typical = 1_000) shouldBe "+10%"
    }

    "formats a negative delta with a minus sign" {
        formatPaceDeltaPercent(projected = 900, typical = 1_000) shouldBe "−10%"
    }

    "formats an unchanged projection as zero" {
        formatPaceDeltaPercent(projected = 1_000, typical = 1_000) shouldBe "0%"
    }

    "rounds the delta to the nearest percent" {
        formatPaceDeltaPercent(projected = 1_006, typical = 1_000) shouldBe "+1%"
        formatPaceDeltaPercent(projected = 994, typical = 1_000) shouldBe "−1%"
    }

    "returns null without a typical month to compare against" {
        formatPaceDeltaPercent(projected = 1_000, typical = 0) shouldBe null
    }
})
