package org.sjbtimdan.linden.ui.entry

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.predictions.QuickEntry
import org.sjbtimdan.linden.predictions.RecurrenceCadence

class EntryTypeVisualsTest : StringSpec({
    "every entry type has a distinct icon" {
        val icons = EntryType.entries.map { it.icon() }
        icons.distinct().size shouldBe icons.size
    }

    "expense, income and transfer icons all differ" {
        EntryType.Expense.icon() shouldNotBe EntryType.Income.icon()
        EntryType.Expense.icon() shouldNotBe EntryType.Transfer.icon()
        EntryType.Income.icon() shouldNotBe EntryType.Transfer.icon()
    }

    "quick entries show a repeat icon exactly when they recur" {
        val weekly = quickEntryIcon(quickEntry(RecurrenceCadence.Weekly))
        weekly.shouldNotBeNull()
        quickEntryIcon(quickEntry(RecurrenceCadence.Monthly)) shouldBe weekly
        quickEntryIcon(quickEntry(null)).shouldBeNull()
    }
})

private val account = Account(1, "Main", Currency.CHF)
private val food = Category(1, "Food", CategoryType.Expense)

private fun quickEntry(cadence: RecurrenceCadence?) = QuickEntry(
    ExpenseEntry(1, food, "Coffee", account, 450),
    cadence,
)
