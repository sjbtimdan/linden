package org.sjbtimdan.linden.ui.ledger

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.ui.entry.EntryDraft
import kotlin.time.Instant

class LedgerEntryDialogControllerTest : StringSpec({
    "open loads an editable draft" {
        withController { controller, _, drafts ->
            val entry = expenseEntry(id = 7)
            controller.open(entry)

            drafts.value?.editing shouldBe entry
            drafts.value?.amountText shouldBe "12.50"
        }
    }

    "changeType drops a category the target type cannot use" {
        withController { controller, _, drafts ->
            drafts.value = EntryDraft.forEdit(expenseEntry())

            controller.changeType(EntryType.Income)

            drafts.value?.type shouldBe EntryType.Income
            drafts.value?.categoryId shouldBe null
        }
    }

    "duplicate turns an edit into a new draft dated now" {
        withController { controller, _, drafts ->
            drafts.value = EntryDraft.forEdit(
                expenseEntry(createdAt = Instant.parse("2026-01-10T12:00:00Z")),
            )
            val now = Instant.parse("2026-02-01T09:30:00Z")

            controller.duplicate(now = now, zone = TimeZone.UTC)

            drafts.value?.editing shouldBe null
            drafts.value?.createdAt shouldBe now
            drafts.value?.amountText shouldBe "12.50"
        }
    }

    "save creates a new entry and closes the dialog" {
        withController { controller, entryDao, drafts ->
            drafts.value = EntryDraft.forNew(clock = FakeClock()).copy(
                amountText = "12.50",
                categoryId = groceries.id,
                accountId = account.id,
            )

            controller.save() shouldBe true

            entryDao.getAll().first().single().amount shouldBe 1_250
            drafts.value shouldBe null
        }
    }

    "save updates an edited entry" {
        withController { controller, entryDao, drafts ->
            entryDao.create(expenseEntry(id = 0))
            val stored = entryDao.getAll().first().single()
            drafts.value = EntryDraft.forEdit(stored).copy(amountText = "9.99")

            controller.save() shouldBe true

            entryDao.getAll().first().single().amount shouldBe 999
        }
    }

    "save rejects an invalid draft and keeps it open" {
        withController { controller, entryDao, drafts ->
            drafts.value = EntryDraft.forNew(clock = FakeClock()).copy(
                amountText = "",
                categoryId = groceries.id,
                accountId = account.id,
            )

            controller.save() shouldBe false

            drafts.value.shouldNotBeNull()
            entryDao.getAll().first().shouldBeEmpty()
        }
    }

    "delete removes the entry and undo restores it" {
        withController { controller, entryDao, drafts ->
            entryDao.create(expenseEntry(id = 0))
            val stored = entryDao.getAll().first().single()
            drafts.value = EntryDraft.forEdit(stored)

            controller.delete()

            entryDao.getAll().first().shouldBeEmpty()
            controller.lastDeleted.value shouldBe stored

            controller.undoLastDeleted()

            entryDao.getAll().first().single().amount shouldBe stored.amount
            controller.lastDeleted.value shouldBe null
        }
    }

    "dismiss clears the draft" {
        withController { controller, _, drafts ->
            drafts.value = EntryDraft.forEdit(expenseEntry())

            controller.dismiss()

            drafts.value shouldBe null
        }
    }
})

private fun withController(
    block: suspend (LedgerEntryDialogController, EntryDao, MutableStateFlow<EntryDraft?>) -> Unit,
) {
    runBlocking {
        val database = lindenDatabase()
        AccountDao(database.accountQueries).create(account.name, account.currency)
        CategoryDao(database.categoryQueries).create(groceries.name, groceries.type)
        val entryDao = EntryDao(database.entryQueries)
        val drafts = MutableStateFlow<EntryDraft?>(null)
        val controller = LedgerEntryDialogController(
            entryDao = entryDao,
            draftState = drafts,
            accounts = MutableStateFlow(listOf(account)),
            categories = MutableStateFlow(listOf(groceries, salary)),
            savingState = MutableStateFlow(false),
            clock = FakeClock(),
            zone = TimeZone.UTC,
            write = { writeBlock -> runBlocking { writeBlock() } },
        )
        block(controller, entryDao, drafts)
    }
}

private val account = Account(1, "Main", Currency.CHF)
private val groceries = Category(1, "Groceries", CategoryType.Expense)
private val salary = Category(2, "Salary", CategoryType.Income)

private fun expenseEntry(id: Long = 7, createdAt: Instant = Instant.parse("2026-01-10T12:00:00Z")): ExpenseEntry =
    ExpenseEntry(
        id = id,
        category = groceries,
        description = "Coffee",
        account = account,
        amount = 1_250,
        createdAt = createdAt,
        createdZone = TimeZone.UTC,
    )
