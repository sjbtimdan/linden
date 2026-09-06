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

    "fromSystemLanguage maps supported systems onto their app language" {
        AppLanguage.fromSystemLanguage("it") shouldBe AppLanguage.ITALIAN
        AppLanguage.fromSystemLanguage("it-IT") shouldBe AppLanguage.ITALIAN
        AppLanguage.fromSystemLanguage("it-CH") shouldBe AppLanguage.ITALIAN
        AppLanguage.fromSystemLanguage("zh-CN") shouldBe AppLanguage.CHINESE_SIMPLIFIED
        AppLanguage.fromSystemLanguage("zh-Hans-CN") shouldBe AppLanguage.CHINESE_SIMPLIFIED
        AppLanguage.fromSystemLanguage("zh-SG") shouldBe AppLanguage.CHINESE_SIMPLIFIED
        AppLanguage.fromSystemLanguage("zh") shouldBe AppLanguage.CHINESE_SIMPLIFIED
        AppLanguage.fromSystemLanguage("zh-HK") shouldBe AppLanguage.CHINESE_TRADITIONAL_HK
        AppLanguage.fromSystemLanguage("zh-Hant-HK") shouldBe AppLanguage.CHINESE_TRADITIONAL_HK
        AppLanguage.fromSystemLanguage("zh-Hant-TW") shouldBe AppLanguage.CHINESE_TRADITIONAL_HK
        AppLanguage.fromSystemLanguage("zh-MO") shouldBe AppLanguage.CHINESE_TRADITIONAL_HK
    }

    "fromSystemLanguage falls back to English for unsupported systems" {
        AppLanguage.fromSystemLanguage(null) shouldBe AppLanguage.ENGLISH
        AppLanguage.fromSystemLanguage("") shouldBe AppLanguage.ENGLISH
        AppLanguage.fromSystemLanguage("de-DE") shouldBe AppLanguage.ENGLISH
        AppLanguage.fromSystemLanguage("fr-CH") shouldBe AppLanguage.ENGLISH
        AppLanguage.fromSystemLanguage("en-US") shouldBe AppLanguage.ENGLISH
        AppLanguage.fromSystemLanguage("en-GB") shouldBe AppLanguage.ENGLISH
    }

    "resolve turns SYSTEM into the resolved language and keeps pins" {
        AppLanguage.resolve(AppLanguage.SYSTEM, "de-DE") shouldBe AppLanguage.ENGLISH
        AppLanguage.resolve(AppLanguage.SYSTEM, "zh-HK") shouldBe AppLanguage.CHINESE_TRADITIONAL_HK
        AppLanguage.resolve(AppLanguage.ITALIAN, "de-DE") shouldBe AppLanguage.ITALIAN
        AppLanguage.resolve(AppLanguage.ENGLISH, "zh-CN") shouldBe AppLanguage.ENGLISH
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
