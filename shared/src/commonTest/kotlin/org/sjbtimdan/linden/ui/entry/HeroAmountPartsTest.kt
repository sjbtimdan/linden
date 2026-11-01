package org.sjbtimdan.linden.ui.entry

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class HeroAmountPartsTest : StringSpec({

    "splits an amount into whole units and cents" {
        val parts = heroAmountParts(11_895, 2)

        parts shouldBe HeroAmountParts(whole = "118", fraction = ".95")
        parts.text shouldBe "118.95"
    }

    "keeps the sign of a sub-unit negative amount" {
        heroAmountParts(-56, 2).text shouldBe "-0.56"
    }

    "rounds zero-decimal currencies and drops the fraction" {
        heroAmountParts(1_050, 0) shouldBe HeroAmountParts(whole = "11")
    }

    "splits compact millions and billions with their suffix" {
        heroAmountParts(125_000_000, 2) shouldBe
            HeroAmountParts(whole = "1", fraction = ".25", unitSuffix = "m")
        heroAmountParts(123_456_789_000, 2) shouldBe
            HeroAmountParts(whole = "1", fraction = ".235", unitSuffix = "b")
    }

    "keeps the sign of a compact negative amount" {
        heroAmountParts(-125_000_000, 2) shouldBe
            HeroAmountParts(whole = "-1", fraction = ".25", unitSuffix = "m")
    }

    "joins back into the exact amount the rest of the UI shows" {
        listOf(0L, 450L, 11_895L, -56L, 99_999_999L, 100_000_000L, -125_000_000L).forEach { amount ->
            heroAmountParts(amount, 2).text shouldBe formatAmountCompact(amount, 2)
        }
        listOf(0L, 450L, 1_050L, -1_050L).forEach { amount ->
            heroAmountParts(amount, 0).text shouldBe formatAmount(amount, 0)
        }
    }
})
