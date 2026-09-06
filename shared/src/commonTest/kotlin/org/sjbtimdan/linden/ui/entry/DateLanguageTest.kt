package org.sjbtimdan.linden.ui.entry

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DateLanguageTest : StringSpec({

    "maps language codes onto a date language" {
        dateLanguage(null) shouldBe DateLanguage.English
        dateLanguage("en") shouldBe DateLanguage.English
        dateLanguage("de") shouldBe DateLanguage.English
        dateLanguage("fr-CH") shouldBe DateLanguage.English
        dateLanguage("it") shouldBe DateLanguage.Italian
        dateLanguage("IT") shouldBe DateLanguage.Italian
        dateLanguage("it-CH") shouldBe DateLanguage.Italian
        dateLanguage("zh") shouldBe DateLanguage.Chinese
        dateLanguage("zh-CN") shouldBe DateLanguage.Chinese
        dateLanguage("zh-Hans-CN") shouldBe DateLanguage.Chinese
        dateLanguage("zh-TW") shouldBe DateLanguage.Chinese
        dateLanguage("zh-Hant-HK") shouldBe DateLanguage.Chinese
    }

    "keeps the English month table" {
        (1..12).map(DateLanguage.English::monthShort) shouldBe listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        )
    }

    "keeps the Italian month table" {
        (1..12).map(DateLanguage.Italian::monthShort) shouldBe listOf(
            "gen", "feb", "mar", "apr", "mag", "giu",
            "lug", "ago", "set", "ott", "nov", "dic",
        )
    }

    "numbers the Chinese months" {
        DateLanguage.Chinese.monthShort(1) shouldBe "1月"
        DateLanguage.Chinese.monthShort(8) shouldBe "8月"
        DateLanguage.Chinese.monthShort(12) shouldBe "12月"
    }

    "date text follows the language layout" {
        DateLanguage.English.dateText(13, 8, 2026) shouldBe "Aug 13, 2026"
        DateLanguage.Italian.dateText(13, 8, 2026) shouldBe "13 ago 2026"
        DateLanguage.Chinese.dateText(13, 8, 2026) shouldBe "2026年8月13日"
    }

    "month-year text follows the language layout" {
        DateLanguage.English.monthYearText(8, 2026) shouldBe "Aug 2026"
        DateLanguage.Italian.monthYearText(8, 2026) shouldBe "ago 2026"
        DateLanguage.Chinese.monthYearText(8, 2026) shouldBe "2026年8月"
    }
})
