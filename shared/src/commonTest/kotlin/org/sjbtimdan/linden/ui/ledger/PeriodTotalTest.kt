package org.sjbtimdan.linden.ui.ledger

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
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

    "accountNetInDefaultMinor counts a transfer out as negative" {
        val entries = listOf(
            IncomeEntry(0, groceries, null, chf, 500),
            TransferEntry(0, null, null, chf, 300, toAccount = usd, toAmount = null),
        )
        accountNetInDefaultMinor(chf, entries, Currency.CHF, emptyList()) shouldBe 200L
    }

    "accountNetInDefaultMinor counts a transfer in as positive" {
        val entries = listOf(
            ExpenseEntry(0, groceries, null, chf, 100),
            TransferEntry(0, null, null, usd, 300, toAccount = chf, toAmount = null),
        )
        accountNetInDefaultMinor(chf, entries, Currency.CHF, emptyList()) shouldBe 200L
    }

    "accountNetInDefaultMinor converts a foreign received amount at its own rate" {
        // The CHF account receives 1,000 CHF for the 800 USD it was sent.
        val entries = listOf(
            TransferEntry(0, null, null, usd, 800, toAccount = chf, toAmount = 1_000),
        )
        accountNetInDefaultMinor(chf, entries, Currency.CHF, chfUsdRate) shouldBe 1_000L
    }

    "accountNetInDefaultMinor is null when the account's currency has no stored rate" {
        // The USD account's delta is in USD, which cannot convert without a CHF->USD rate.
        val entries = listOf(
            TransferEntry(0, null, null, chf, 100, toAccount = usd, toAmount = 80),
        )
        accountNetInDefaultMinor(usd, entries, Currency.CHF, emptyList()) shouldBe null
    }

    "accountNetInDefaultMinor is zero for an account with no matching entries" {
        accountNetInDefaultMinor(eur, emptyList(), Currency.CHF, emptyList()) shouldBe 0L
    }

    "default-currency entries do not need a stored rate" {
        periodTotalMinor(
            listOf(ExpenseEntry(0, groceries, null, chf, 450)),
            Currency.CHF,
            emptyList(),
        ) shouldBe -450L
    }

    "net converts income and expense per currency group, not entry by entry" {
        val entries = listOf(
            IncomeEntry(0, groceries, null, usd, 2),
            ExpenseEntry(0, groceries, null, usd, 1),
        )
        // Per sign: 2 USD -> 3 CHF and -1 USD -> -1 CHF, net 2 CHF.
        // Converting the net (2 - 1) / 0.8 = 1.25 would round to 1 CHF and break
        // the reconciliation with the income and expense totals.
        periodTotalMinor(entries, Currency.CHF, chfUsdRate) shouldBe 2L
    }

    "net equals the income total plus the expense total" {
        val all = listOf(
            IncomeEntry(0, groceries, null, usd, 2),
            ExpenseEntry(0, groceries, null, usd, 1),
            IncomeEntry(0, groceries, null, chf, 5),
        )
        val incomeOnly = all.filter { it.type == EntryType.Income }
        val expenseOnly = all.filter { it.type == EntryType.Expense }

        val net = periodTotalMinor(all, Currency.CHF, chfUsdRate)
        val incomeTotal = periodTotalMinor(incomeOnly, Currency.CHF, chfUsdRate)
        val expenseTotal = periodTotalMinor(expenseOnly, Currency.CHF, chfUsdRate)

        net shouldBe incomeTotal!! + expenseTotal!!
    }

    "sumInDefaultMinor totals same-currency groups without rates" {
        sumInDefaultMinor(
            listOf(Currency.CHF to 450L, Currency.CHF to -1_200L),
            Currency.CHF,
            emptyList(),
        ) shouldBe -750L
    }

    "sumInDefaultMinor converts foreign groups via the stored rate" {
        sumInDefaultMinor(
            listOf(Currency.CHF to 1_000L, Currency.USD to 800L),
            Currency.CHF,
            chfUsdRate,
        ) shouldBe 2_000L
    }

    "sumInDefaultMinor is null when a foreign group has no rate" {
        sumInDefaultMinor(
            listOf(Currency.CHF to 100L, Currency.EUR to 200L),
            Currency.CHF,
            chfUsdRate,
        ) shouldBe null
    }

    "sumInDefaultMinor of no groups is zero" {
        sumInDefaultMinor(emptyList(), Currency.CHF, emptyList()) shouldBe 0L
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
