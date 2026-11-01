package org.sjbtimdan.linden.ui.entry

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Locale

class HeroAmountPartsJvmTest : StringSpec({

    "uses the locale decimal separator" {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)

            val parts = heroAmountParts(11_895, 2)

            parts shouldBe HeroAmountParts(whole = "118", fraction = ",95")
            parts.text shouldBe formatAmount(11_895, 2)
        } finally {
            Locale.setDefault(original)
        }
    }
})
