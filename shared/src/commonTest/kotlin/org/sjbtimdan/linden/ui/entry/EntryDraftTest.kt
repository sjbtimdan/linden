package org.sjbtimdan.linden.ui.entry

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry

private val main = Account(1, "Main", Currency.CHF)
private val savingsEur = Account(2, "Savings", Currency.EUR)
private val savingsChf = Account(3, "Savings", Currency.CHF)
private val accounts = listOf(main, savingsEur, savingsChf)
private val groceries = Category(1, "Groceries", CategoryType.Expense)
private val salary = Category(2, "Salary", CategoryType.Income)
private val categories = listOf(groceries, salary)
private val createdAt = Instant.fromEpochMilliseconds(1_000)
private val createdZone = TimeZone.UTC

private fun draft(
    type: EntryType = EntryType.Expense,
    amountText: String = "10.00",
    categoryId: Long? = groceries.id,
    accountId: Long? = main.id,
    toAccountId: Long? = null,
    toAmountText: String = "",
    description: String = "",
    editing: Entry? = null,
): EntryDraft = EntryDraft(
    editing = editing,
    type = type,
    amountText = amountText,
    categoryId = categoryId,
    accountId = accountId,
    toAccountId = toAccountId,
    toAmountText = toAmountText,
    description = description,
    createdAt = createdAt,
    createdZone = createdZone,
)

class EntryDraftTest : StringSpec({

    // isValid

    "expense and income drafts are valid with amount, account, and category" {
        draft().isValid(accounts) shouldBe true
        draft(type = EntryType.Income, categoryId = salary.id).isValid(accounts) shouldBe true
    }

    "missing or non-positive amounts are invalid" {
        draft(amountText = "").isValid(accounts) shouldBe false
        draft(amountText = "abc").isValid(accounts) shouldBe false
        draft(amountText = "0").isValid(accounts) shouldBe false
        draft(amountText = "-5").isValid(accounts) shouldBe false
    }

    "expense and income drafts require an account and a category" {
        draft(accountId = null).isValid(accounts) shouldBe false
        draft(categoryId = null).isValid(accounts) shouldBe false
        draft(type = EntryType.Income, categoryId = null).isValid(accounts) shouldBe false
    }

    "same-currency transfers are valid without a received amount" {
        draft(
            type = EntryType.Transfer,
            toAccountId = savingsChf.id,
            toAmountText = "",
        ).isValid(accounts) shouldBe true
    }

    "cross-currency transfers require a positive received amount" {
        val base = draft(type = EntryType.Transfer, toAccountId = savingsEur.id)
        base.copy(toAmountText = "").isValid(accounts) shouldBe false
        base.copy(toAmountText = "0").isValid(accounts) shouldBe false
        base.copy(toAmountText = "9.50").isValid(accounts) shouldBe true
    }

    "a transfer to the same account is invalid" {
        draft(type = EntryType.Transfer, toAccountId = main.id, toAmountText = "9.50")
            .isValid(accounts) shouldBe false
    }

    "transfers require both accounts to exist" {
        draft(type = EntryType.Transfer, toAccountId = null).isValid(accounts) shouldBe false
        draft(type = EntryType.Transfer, toAccountId = 99).isValid(accounts) shouldBe false
        draft(type = EntryType.Transfer, accountId = 99, toAccountId = savingsChf.id)
            .isValid(accounts) shouldBe false
    }

    // toEntry

    "expense drafts convert to an expense entry" {
        val entry = draft(amountText = "4.50", description = "  Coffee  ").toEntry(accounts, categories)
        entry shouldBe ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = createdAt, createdZone = createdZone)
    }

    "income drafts convert to an income entry" {
        val entry = draft(type = EntryType.Income, categoryId = salary.id, description = "Bonus")
            .toEntry(accounts, categories)
        entry shouldBe IncomeEntry(0, salary, "Bonus", main, 1_000, createdAt = createdAt, createdZone = createdZone)
    }

    "editing drafts keep the edited entry's id" {
        val editing = ExpenseEntry(7, groceries, "Old", main, 100)
        val entry = draft(editing = editing).toEntry(accounts, categories)
        entry shouldBe ExpenseEntry(7, groceries, null, main, 1_000, createdAt = createdAt, createdZone = createdZone)
    }

    "blank descriptions convert to null" {
        val entry = draft(description = "   ").toEntry(accounts, categories)
        entry?.description.shouldBeNull()
    }

    "conversion fails when the account is missing" {
        draft(accountId = 99).toEntry(accounts, categories).shouldBeNull()
    }

    "conversion fails when the category is missing" {
        draft(categoryId = 99).toEntry(accounts, categories).shouldBeNull()
    }

    "same-currency transfers store a null received amount" {
        val entry = draft(type = EntryType.Transfer, toAccountId = savingsChf.id)
            .toEntry(accounts, categories)
        entry shouldBe TransferEntry(
            0, null, null, main, 1_000,
            createdAt = createdAt, createdZone = createdZone,
            toAccount = savingsChf, toAmount = null,
        )
    }

    "cross-currency transfers store the received amount" {
        val entry = draft(
            type = EntryType.Transfer,
            toAccountId = savingsEur.id,
            toAmountText = "9.50",
        ).toEntry(accounts, categories)
        entry shouldBe TransferEntry(
            0, null, null, main, 1_000,
            createdAt = createdAt, createdZone = createdZone,
            toAccount = savingsEur, toAmount = 950,
        )
    }

    "conversion fails when the transfer target is missing" {
        draft(type = EntryType.Transfer, toAccountId = 99).toEntry(accounts, categories).shouldBeNull()
    }

    // forNew

    "new drafts start empty" {
        val state = EntryDraft.forNew()
        state.editing shouldBe null
        state.type shouldBe EntryType.Expense
        state.amountText shouldBe ""
        state.categoryId shouldBe null
        state.accountId shouldBe null
        state.toAccountId shouldBe null
        state.toAmountText shouldBe ""
        state.description shouldBe ""
    }

    "new drafts prefill category, account, and description from the previous same-type entry" {
        val previous = ExpenseEntry(7, groceries, "Coffee", main, 450)
        val state = EntryDraft.forNew(EntryType.Expense, previous)
        state.categoryId shouldBe groceries.id
        state.accountId shouldBe main.id
        state.description shouldBe "Coffee"

        val income = IncomeEntry(8, salary, "Salary", main, 50_000)
        val incomeState = EntryDraft.forNew(EntryType.Income, income)
        incomeState.categoryId shouldBe salary.id
        incomeState.accountId shouldBe main.id
        incomeState.description shouldBe "Salary"
    }

    "new transfer drafts prefill accounts but not the category" {
        val previous = TransferEntry(9, null, "Move money", main, 10_000, toAccount = savingsEur, toAmount = 9_500)
        val state = EntryDraft.forNew(EntryType.Transfer, previous)
        state.categoryId shouldBe null
        state.accountId shouldBe main.id
        state.toAccountId shouldBe savingsEur.id
        state.description shouldBe "Move money"
    }

    "new drafts ignore a previous entry of a different type" {
        val previous = ExpenseEntry(7, groceries, "Coffee", main, 450)
        val state = EntryDraft.forNew(EntryType.Income, previous)
        state.categoryId shouldBe null
        state.accountId shouldBe null
        state.description shouldBe ""
    }

    // forEdit

    "editing an expense entry round-trips" {
        val entry = ExpenseEntry(7, groceries, "Coffee", main, 450, createdAt = createdAt, createdZone = createdZone)
        val state = EntryDraft.forEdit(entry)
        state.editing shouldBe entry
        state.type shouldBe EntryType.Expense
        state.amountText shouldBe formatAmount(450)
        state.categoryId shouldBe groceries.id
        state.accountId shouldBe main.id
        state.toAccountId shouldBe null
        state.toAmountText shouldBe ""
        state.description shouldBe "Coffee"
    }

    "editing an income entry round-trips" {
        val entry = IncomeEntry(8, salary, "Salary", main, 50_000)
        val state = EntryDraft.forEdit(entry)
        state.type shouldBe EntryType.Income
        state.amountText shouldBe formatAmount(50_000)
        state.categoryId shouldBe salary.id
        state.accountId shouldBe main.id
        state.description shouldBe "Salary"
    }

    "editing a same-currency transfer shows the sent amount as received" {
        val entry = TransferEntry(9, null, null, main, 10_000, toAccount = savingsChf, toAmount = null)
        val state = EntryDraft.forEdit(entry)
        state.type shouldBe EntryType.Transfer
        state.amountText shouldBe formatAmount(10_000)
        state.toAccountId shouldBe savingsChf.id
        state.toAmountText shouldBe formatAmount(10_000)
    }

    "editing a cross-currency transfer shows the received amount" {
        val entry = TransferEntry(9, null, "Top up", main, 10_000, toAccount = savingsEur, toAmount = 9_500)
        val state = EntryDraft.forEdit(entry)
        state.toAccountId shouldBe savingsEur.id
        state.toAmountText shouldBe formatAmount(9_500)
    }
})
