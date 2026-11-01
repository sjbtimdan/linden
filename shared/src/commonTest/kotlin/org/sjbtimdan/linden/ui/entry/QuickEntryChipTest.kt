package org.sjbtimdan.linden.ui.entry

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.predictions.QuickEntry
import kotlin.time.Instant

class QuickEntryChipTest : StringSpec({
    val main = Account(1, "Main", Currency.CHF)
    val savings = Account(2, "Savings", Currency.CHF)
    val credit = Account(3, "Credit", Currency.CHF)
    val cash = Account(4, "Cash", Currency.JPY)
    val food = Category(1, "Food", CategoryType.Expense)
    val now = Instant.fromEpochMilliseconds(1_800_000_000_000L)

    fun expense(id: Long, description: String, account: Account = main, amount: Long = 450) = ExpenseEntry(
        id = id,
        category = food,
        description = description,
        account = account,
        amount = amount,
        createdAt = now,
        createdZone = TimeZone.UTC,
    )

    fun transfer(id: Long, toAccount: Account, description: String = "Move money", amount: Long = 10_000) =
        TransferEntry(
            id = id,
            category = null,
            description = description,
            account = main,
            amount = amount,
            createdAt = now,
            createdZone = TimeZone.UTC,
            toAccount = toAccount,
            toAmount = null,
        )

    fun chips(vararg entries: QuickEntry) = quickEntryChips(entries.toList()).map { it.label }

    "labels a quick entry with its description and amount" {
        chips(QuickEntry(expense(1, "Coffee"), null)).shouldContainExactly("Coffee · 4.50")
    }

    "keeps different amounts as distinct base labels without an account suffix" {
        chips(
            QuickEntry(expense(1, "Coffee", amount = 450), null),
            QuickEntry(expense(2, "Coffee", amount = 520), null),
        ).shouldContainExactly("Coffee · 4.50", "Coffee · 5.20")
    }

    "adds the account when two chips share a description and amount" {
        chips(
            QuickEntry(expense(1, "Coffee", main, 450), null),
            QuickEntry(expense(2, "Coffee", savings, 450), null),
        ).shouldContainExactly("Coffee · 4.50 · Main", "Coffee · 4.50 · Savings")
    }

    "adds the transfer destination when two transfers share a description and amount" {
        chips(
            QuickEntry(transfer(1, savings), null),
            QuickEntry(transfer(2, credit), null),
        ).shouldContainExactly(
            "Move money · 100.00 · Main → Savings",
            "Move money · 100.00 · Main → Credit",
        )
    }

    "uses the account currency's decimal digits" {
        chips(QuickEntry(expense(1, "Lunch", cash, 1_500_000), null)).shouldContainExactly("Lunch · 15,000")
    }

    "an empty list produces no chips" {
        quickEntryChips(emptyList()).shouldContainExactly(emptyList())
    }
})
