package org.sjbtimdan.linden.ui.entry

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.ui.insights.PaceConfidence
import org.sjbtimdan.linden.ui.insights.PaceForecast
import kotlin.time.Instant

private val main = Account(1, "Main", Currency.CHF)
private val usd = Account(2, "USD", Currency.USD)
private val food = Category(1, "Food", CategoryType.Expense)

private fun forecast(
    spent: Long = 0,
    typical: Long = 2_000,
    daysRemaining: Int = 10,
    confidence: PaceConfidence = PaceConfidence.High,
    missing: Set<Currency> = emptySet(),
) = PaceForecast(
    monthStart = LocalDate(2026, 1, 1),
    dayOfMonth = 20,
    daysInMonth = 31,
    daysRemaining = daysRemaining,
    spentMinor = spent,
    projectedMinor = spent,
    likelyLowMinor = spent,
    likelyHighMinor = spent,
    typicalMonthMinor = typical,
    dailyAllowanceMinor = 0,
    historyMonths = 6,
    confidence = confidence,
    missingCurrencies = missing,
)

private fun draft(
    type: EntryType = EntryType.Expense,
    amountText: String = "5.00",
    accountId: Long? = main.id,
    editing: Entry? = null,
) = EntryDraft(
    editing = editing,
    type = type,
    amountText = amountText,
    categoryId = food.id,
    accountId = accountId,
    toAccountId = null,
    toAmountText = "",
    description = "",
    createdAt = Instant.fromEpochMilliseconds(0),
    createdZone = TimeZone.UTC,
)

private fun expenseEntry() = ExpenseEntry(
    id = 1,
    category = food,
    description = null,
    account = main,
    amount = 500,
    createdAt = Instant.fromEpochMilliseconds(0),
    createdZone = TimeZone.UTC,
)

class PaceAfterThisTest : StringSpec({

    "subtracts the draft amount from the remaining typical month" {
        val result = paceAfterThisFor(
            forecast = forecast(spent = 500, typical = 2_000, daysRemaining = 10),
            draft = draft(amountText = "5.00"),
            accounts = listOf(main),
            defaultCurrency = Currency.CHF,
            rates = emptyList(),
        )

        result shouldBe PaceAfterThis(dailyAllowanceMinor = 100, currency = Currency.CHF)
    }

    "converts the draft amount into the default currency" {
        val result = paceAfterThisFor(
            forecast = forecast(spent = 500, typical = 2_000, daysRemaining = 10),
            draft = draft(amountText = "10.00", accountId = usd.id),
            accounts = listOf(main, usd),
            defaultCurrency = Currency.CHF,
            rates = listOf(FxRate(Currency.CHF, Currency.USD, 2.0, "2026-01-01")),
        )

        result shouldBe PaceAfterThis(dailyAllowanceMinor = 100, currency = Currency.CHF)
    }

    "ignores income and transfer drafts" {
        val income = paceAfterThisFor(
            forecast = forecast(),
            draft = draft(type = EntryType.Income),
            accounts = listOf(main),
            defaultCurrency = Currency.CHF,
            rates = emptyList(),
        )
        val transfer = paceAfterThisFor(
            forecast = forecast(),
            draft = draft(type = EntryType.Transfer),
            accounts = listOf(main),
            defaultCurrency = Currency.CHF,
            rates = emptyList(),
        )

        income shouldBe null
        transfer shouldBe null
    }

    "ignores an edit, whose amount is already in the month" {
        val result = paceAfterThisFor(
            forecast = forecast(),
            draft = draft(editing = expenseEntry()),
            accounts = listOf(main),
            defaultCurrency = Currency.CHF,
            rates = emptyList(),
        )

        result shouldBe null
    }

    "ignores a draft without a known account or rate" {
        val unknownAccount = paceAfterThisFor(
            forecast = forecast(),
            draft = draft(accountId = 99),
            accounts = listOf(main),
            defaultCurrency = Currency.CHF,
            rates = emptyList(),
        )
        val missingRate = paceAfterThisFor(
            forecast = forecast(),
            draft = draft(accountId = usd.id),
            accounts = listOf(main, usd),
            defaultCurrency = Currency.CHF,
            rates = emptyList(),
        )

        unknownAccount shouldBe null
        missingRate shouldBe null
    }

    "ignores a low-confidence or incomplete forecast" {
        val lowConfidence = paceAfterThisFor(
            forecast = forecast(confidence = PaceConfidence.Low),
            draft = draft(),
            accounts = listOf(main),
            defaultCurrency = Currency.CHF,
            rates = emptyList(),
        )
        val incomplete = paceAfterThisFor(
            forecast = forecast(missing = setOf(Currency.USD)),
            draft = draft(),
            accounts = listOf(main),
            defaultCurrency = Currency.CHF,
            rates = emptyList(),
        )

        lowConfidence shouldBe null
        incomplete shouldBe null
    }

    "floors the allowance at zero once the usual total is exceeded" {
        val result = paceAfterThisFor(
            forecast = forecast(spent = 2_000, typical = 2_000),
            draft = draft(amountText = "5.00"),
            accounts = listOf(main),
            defaultCurrency = Currency.CHF,
            rates = emptyList(),
        )

        result shouldBe PaceAfterThis(dailyAllowanceMinor = 0, currency = Currency.CHF)
    }
})
