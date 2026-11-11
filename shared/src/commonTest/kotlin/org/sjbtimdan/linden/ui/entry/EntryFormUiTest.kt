package org.sjbtimdan.linden.ui.entry

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import kotlin.time.Instant

private val now = Instant.parse("2026-08-10T14:30:00Z")
private val main = Account(1, "Main", Currency.CHF)
private val euro = Account(2, "Euro", Currency.EUR)
private val secondEuro = Account(3, "Second euro", Currency.EUR)
private val groceries = Category(1, "Groceries", CategoryType.Expense)
private val salary = Category(2, "Salary", CategoryType.Income)
private val mixed = Category(3, "Mixed", CategoryType.Both)

private fun draft(
    type: EntryType = EntryType.Expense,
    accountId: Long? = main.id,
    toAccountId: Long? = null,
    categoryId: Long? = groceries.id,
): EntryDraft = EntryDraft(
    editing = null,
    type = type,
    amountText = "",
    categoryId = categoryId,
    accountId = accountId,
    toAccountId = toAccountId,
    toAmountText = "",
    description = "",
    createdAt = now,
    createdZone = TimeZone.UTC,
)

class EntryFormUiTest : StringSpec({

    "offers only the categories the draft type may use" {
        val ui = entryFormUi(
            state = draft(),
            accounts = listOf(main),
            categories = listOf(groceries, salary, mixed),
            suggestions = EntryFormSuggestions(),
        )

        ui.visibleCategories shouldBe listOf(groceries, mixed)
    }

    "resolves the selected category and accounts from the draft" {
        val ui = entryFormUi(
            state = draft(accountId = main.id, toAccountId = euro.id, categoryId = mixed.id),
            accounts = listOf(main, euro),
            categories = listOf(mixed),
            suggestions = EntryFormSuggestions(),
        )

        ui.fromAccount shouldBe main
        ui.toAccount shouldBe euro
        ui.selectedCategory shouldBe mixed
    }

    "excludes the source account from transfer destinations" {
        val ui = entryFormUi(
            state = draft(accountId = main.id),
            accounts = listOf(main, euro),
            categories = emptyList(),
            suggestions = EntryFormSuggestions(),
        )

        ui.toAccountOptions shouldBe listOf(euro)
    }

    "flags the received amount only for a cross-currency transfer" {
        entryFormUi(
            state = draft(type = EntryType.Transfer, toAccountId = euro.id),
            accounts = listOf(main, euro),
            categories = emptyList(),
            suggestions = EntryFormSuggestions(),
        ).showReceivedAmount shouldBe true

        entryFormUi(
            state = draft(type = EntryType.Transfer, toAccountId = secondEuro.id),
            accounts = listOf(euro, secondEuro),
            categories = emptyList(),
            suggestions = EntryFormSuggestions(),
        ).showReceivedAmount shouldBe false

        entryFormUi(
            state = draft(type = EntryType.Expense, toAccountId = euro.id),
            accounts = listOf(main, euro),
            categories = listOf(groceries),
            suggestions = EntryFormSuggestions(),
        ).showReceivedAmount shouldBe false
    }

    "keeps predicted options that exist and drops the current selection" {
        val ui = entryFormUi(
            state = draft(accountId = main.id, categoryId = groceries.id),
            accounts = listOf(main, euro),
            categories = listOf(groceries, mixed),
            suggestions = EntryFormSuggestions(
                categoryIds = listOf(groceries.id, mixed.id, 99L),
                accountIds = listOf(main.id, euro.id, 42L),
            ),
        )

        ui.predictedCategoryOptions shouldBe listOf(mixed)
        ui.predictedAccountOptions shouldBe listOf(euro)
    }
})
