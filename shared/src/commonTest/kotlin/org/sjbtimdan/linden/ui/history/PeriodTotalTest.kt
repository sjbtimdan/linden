package org.sjbtimdan.linden.ui.history

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry

class PeriodTotalTest : StringSpec({
    val chf = Account(1, "CHF account", Currency.CHF)
    val usd = Account(2, "USD account", Currency.USD)
    val eur = Account(3, "EUR account", Currency.EUR)
    val groceries = Category(1, "Groceries", CategoryType.Expense)
    val chfUsdRate = listOf(FxRate(Currency.CHF, Currency.USD, 0.8, "2026-08-13"))

    "empty entries total zero" {
        periodTotalMinor(emptyList(), Currency.CHF, emptyList()) shouldBe 0L
    }

    "same-currency entries need no rates" {
        val entries = listOf(
            ExpenseEntry(0, groceries, null, chf, 450),
            IncomeEntry(0, groceries, null, chf, 1_200),
        )
        periodTotalMinor(entries, Currency.CHF, emptyList()) shouldBe 750L
    }

    "expenses count negative and income positive" {
        val entries = listOf(
            ExpenseEntry(0, groceries, null, chf, 450),
            IncomeEntry(0, groceries, null, chf, 1_200),
            ExpenseEntry(0, groceries, null, chf, 100),
        )
        periodTotalMinor(entries, Currency.CHF, emptyList()) shouldBe 650L
    }

    "foreign currency entries convert via the stored rate" {
        val entries = listOf(ExpenseEntry(0, groceries, null, usd, 800))
        // 1 CHF = 0.8 USD, so 800 USD = 1,000 CHF
        periodTotalMinor(entries, Currency.CHF, chfUsdRate) shouldBe -1_000L
    }

    "missing rate for any foreign currency yields null" {
        periodTotalMinor(
            listOf(ExpenseEntry(0, groceries, null, eur, 800)),
            Currency.CHF,
            chfUsdRate,
        ) shouldBe null
    }

    "missing rate for only some entries yields null" {
        periodTotalMinor(
            listOf(
                ExpenseEntry(0, groceries, null, chf, 100),
                ExpenseEntry(0, groceries, null, eur, 200),
            ),
            Currency.CHF,
            chfUsdRate,
        ) shouldBe null
    }

    "transfers are excluded from the total" {
        val entries = listOf(
            IncomeEntry(0, groceries, null, chf, 500),
            TransferEntry(0, null, null, chf, 300, toAccount = usd, toAmount = null),
            TransferEntry(0, null, null, usd, 700, toAccount = chf, toAmount = null),
        )
        periodTotalMinor(entries, Currency.CHF, emptyList()) shouldBe 500L
    }

    "rates for a different base currency are ignored" {
        val wrongBase = listOf(FxRate(Currency.USD, Currency.CHF, 1.25, "2026-08-13"))
        periodTotalMinor(
            listOf(ExpenseEntry(0, groceries, null, usd, 800)),
            Currency.CHF,
            wrongBase,
        ) shouldBe null
    }

    "default-currency entries do not need a stored rate" {
        periodTotalMinor(
            listOf(ExpenseEntry(0, groceries, null, chf, 450)),
            Currency.CHF,
            emptyList(),
        ) shouldBe -450L
    }

    "formatTotal prefixes a minus sign and appends the symbol" {
        val formatted = formatTotal(-1_230, Currency.CHF)
        formatted.startsWith("− ") shouldBe true
        formatted.endsWith(" CHF") shouldBe true
    }

    "formatTotal prefixes a plus sign for positive totals" {
        val formatted = formatTotal(1_230, Currency.CHF)
        formatted.startsWith("+ ") shouldBe true
        formatted.endsWith(" CHF") shouldBe true
    }

    "formatTotal has no sign for zero" {
        val formatted = formatTotal(0, Currency.CHF)
        formatted.startsWith("+ ") shouldBe false
        formatted.startsWith("− ") shouldBe false
        formatted.endsWith(" CHF") shouldBe true
    }
})
