package org.sjbtimdan.linden.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AppLanguageTest : StringSpec({
    "fromTag parses persisted values" {
        AppLanguage.fromTag(null) shouldBe AppLanguage.SYSTEM
        AppLanguage.fromTag("en") shouldBe AppLanguage.ENGLISH
        AppLanguage.fromTag("it") shouldBe AppLanguage.ITALIAN
        AppLanguage.fromTag("zh-CN") shouldBe AppLanguage.CHINESE_SIMPLIFIED
        AppLanguage.fromTag("zh-HK") shouldBe AppLanguage.CHINESE_TRADITIONAL_HK
    }

    "fromTag falls back to SYSTEM for unknown or missing values" {
        AppLanguage.fromTag("de") shouldBe AppLanguage.SYSTEM
        AppLanguage.fromTag("") shouldBe AppLanguage.SYSTEM
        AppLanguage.fromTag("system") shouldBe AppLanguage.SYSTEM
    }

    "every override carries a BCP-47 tag usable as a locale and resource qualifier" {
        AppLanguage.entries.forEach { language ->
            if (language == AppLanguage.SYSTEM) {
                language.tag.shouldBe(null)
            } else {
                language.tag shouldBe AppLanguage.fromTag(language.tag).tag
            }
        }
    }
})
