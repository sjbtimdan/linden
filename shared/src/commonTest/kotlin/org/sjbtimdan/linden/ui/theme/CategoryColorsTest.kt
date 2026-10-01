package org.sjbtimdan.linden.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val MIN_ICON_CONTRAST = 3.0f

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

    "accent content keeps icons legible on every palette color" {
        (CategoryPalette + DarkCategoryPalette).forEach { accent ->
            (contrastRatio(accent, accentContentColor(accent)) >= MIN_ICON_CONTRAST) shouldBe true
        }
    }

    "accent content is white on dark accents and ink on lightened accents" {
        accentContentColor(CategoryPalette.first()) shouldBe Color.White
        accentContentColor(DarkCategoryPalette.first()) shouldBe Color(0xFF1A1C19)
    }
})

/** WCAG contrast ratio between two opaque colors. */
private fun contrastRatio(a: Color, b: Color): Float {
    val brighter = maxOf(a.luminance(), b.luminance())
    val darker = minOf(a.luminance(), b.luminance())
    return (brighter + 0.05f) / (darker + 0.05f)
}
