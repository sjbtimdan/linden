package org.sjbtimdan.linden.ui.theme

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class CategoryColorsTest : StringSpec({
    "same name maps to the same color and index" {
        categoryColor("Groceries") shouldBe categoryColor("Groceries")
        categoryColorIndex("Groceries") shouldBe categoryColorIndex("Groceries")
    }

    "index is always within the palette, even for negative hashes" {
        listOf("Groceries", "Transport", "Salary", "Über-Änderungen", "a", "z", "", "x y z").forEach {
            (categoryColorIndex(it) in 0 until CategoryPalette.size) shouldBe true
        }
    }

    "palette offers a range of distinct accents" {
        CategoryPalette.distinct().size shouldBe CategoryPalette.size
        CategoryPalette.size shouldBe 8
    }

    "dark palette mirrors the light palette" {
        DarkCategoryPalette.size shouldBe CategoryPalette.size
        DarkCategoryPalette.distinct().size shouldBe DarkCategoryPalette.size
    }

    "different category names get different accents" {
        categoryColorIndex("Groceries") shouldNotBe categoryColorIndex("Transport")
        categoryColorIndex("Transport") shouldNotBe categoryColorIndex("Salary")
    }

    "categoryColor returns a palette color" {
        CategoryPalette shouldContain categoryColor("Groceries")
    }
})
