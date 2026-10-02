package org.sjbtimdan.linden

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * Guards the locale string files against the English default
 * (values/strings.xml): every locale must define exactly the default's string
 * and plural keys with the same format-placeholder counts, so a missing key or
 * a dropped argument fails the build instead of showing English or crashing at
 * format time. Chinese and Indonesian (per CLDR) only ever use the "other"
 * plural quantity. Android resolves Indonesian through the legacy "in"
 * language code, so values-in mirrors values-id.
 */
class LocaleFileParityTest : StringSpec({

    val root = File("src/commonMain/composeResources").takeIf { it.isDirectory }
        ?: File("shared/src/commonMain/composeResources")

    fun stringsOf(file: File): Map<String, String> {
        val raw = file.readText()
        val pattern = Regex("<string name=\"([^\"]+)\">(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
        return pattern.findAll(raw).associate { it.groupValues[1] to it.groupValues[2] }
    }

    fun pluralsOf(file: File): Map<String, Map<String, String>> {
        val raw = file.readText()
        val plurals = Regex("<plurals name=\"([^\"]+)\">(.*?)</plurals>", RegexOption.DOT_MATCHES_ALL)
        val items = Regex("<item quantity=\"(\\w+)\">(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
        return plurals.findAll(raw).associate { match ->
            match.groupValues[1] to items.findAll(match.groupValues[2]).associate {
                it.groupValues[1] to it.groupValues[2]
            }
        }
    }

    val defaultFile = File(root, "values/strings.xml")
    val defaultStrings = stringsOf(defaultFile)
    val defaultPlurals = pluralsOf(defaultFile)
    val placeholder = Regex("%\\d+\\$[sd]")

    val otherOnlyPluralLocales = setOf("values-id", "values-in", "values-zh-rCN", "values-zh-rHK", "values-zh")
    val locales = listOf(
        "values-it",
        "values-fr",
        "values-de",
        "values-hi",
        "values-id",
        "values-in",
        "values-zh-rCN",
        "values-zh-rHK",
        "values-zh",
    )

    locales.forEach { locale ->
        "($locale) defines exactly the default string keys with matching placeholders" {
            val strings = stringsOf(File(root, "$locale/strings.xml"))
            strings.keys shouldBe defaultStrings.keys
            strings.forEach { (key, value) ->
                placeholder.findAll(value).count() shouldBe
                    placeholder.findAll(defaultStrings.getValue(key)).count()
            }
        }

        "($locale) defines exactly the default plural keys with matching quantities and placeholders" {
            val plurals = pluralsOf(File(root, "$locale/strings.xml"))
            plurals.keys shouldBe defaultPlurals.keys
            plurals.forEach { (key, items) ->
                val defaultItems = defaultPlurals.getValue(key)
                if (locale in otherOnlyPluralLocales) {
                    // zh and id only ever need the "other" quantity (CLDR).
                    items.keys shouldBe setOf("other")
                } else {
                    items.keys shouldBe defaultItems.keys
                }
                items.forEach { (quantity, value) ->
                    placeholder.findAll(value).count() shouldBe
                        placeholder.findAll(defaultItems.getValue(quantity)).count()
                }
            }
        }
    }

    "the script-less zh fallback mirrors Simplified Chinese" {
        val fallback = stringsOf(File(root, "values-zh/strings.xml"))
        val simplified = stringsOf(File(root, "values-zh-rCN/strings.xml"))
        fallback shouldBe simplified
    }

    "the legacy in fallback mirrors Indonesian" {
        val fallback = stringsOf(File(root, "values-in/strings.xml"))
        val indonesian = stringsOf(File(root, "values-id/strings.xml"))
        fallback shouldBe indonesian
    }
})
