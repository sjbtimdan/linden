package org.sjbtimdan.linden.capture

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.data.CAPTURE_RULES_KEY
import org.sjbtimdan.linden.data.CAPTURE_SEEN_KEY
import org.sjbtimdan.linden.data.CAPTURE_TEMPLATES_KEY
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.time.TEST_NOW

private fun capture(index: Long) = PendingCapture(
    packageName = "com.example.bank",
    amountMinor = index,
    currency = Currency.EUR,
    description = "Payment $index",
    type = EntryType.Expense,
    capturedAtMillis = index,
)

private fun entry(categoryId: Long = 1L, accountId: Long = 2L, description: String? = "COOP") = ExpenseEntry(
    id = 0,
    category = Category(categoryId, "Groceries", CategoryType.Expense),
    description = description,
    account = Account(accountId, "Main", Currency.CHF),
    amount = 1250,
    createdAt = TEST_NOW,
)

class CaptureRepositoryTest : StringSpec({

    "rules start empty and round-trip through the settings table" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))
        repository.rules().shouldBeEmpty()

        val rule = CaptureRule("com.example.bank", accountId = 7)
        repository.setRule(rule)

        repository.rules() shouldBe listOf(rule)
        repository.rulesFlow().first() shouldBe listOf(rule)
    }

    "setRule replaces the rule of the same package and keeps others" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))
        repository.setRule(CaptureRule("app.a", accountId = 1))
        repository.setRule(CaptureRule("app.b", accountId = 2))

        repository.setRule(CaptureRule("app.a", accountId = 3))

        repository.rules() shouldBe listOf(CaptureRule("app.b", accountId = 2), CaptureRule("app.a", accountId = 3))
    }

    "setRuleEnabled flips only the matching rule" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))
        repository.setRule(CaptureRule("app.a", accountId = 1))
        repository.setRule(CaptureRule("app.b", accountId = 2))

        repository.setRuleEnabled("app.a", enabled = false)

        repository.rules() shouldBe listOf(
            CaptureRule("app.a", accountId = 1, enabled = false),
            CaptureRule("app.b", accountId = 2),
        )
    }

    "removeRule deletes only the matching rule" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))
        repository.setRule(CaptureRule("app.a", accountId = 1))
        repository.setRule(CaptureRule("app.b", accountId = 2))

        repository.removeRule("app.a")

        repository.rules() shouldBe listOf(CaptureRule("app.b", accountId = 2))
    }

    "a corrupt rules value reads as empty" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))
        database.settingsQueries.insertOrReplace(CAPTURE_RULES_KEY, "{not json")

        repository.rules().shouldBeEmpty()
        repository.rulesFlow().first().shouldBeEmpty()
    }

    "addPending assigns increasing ids and lists the newest first" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))

        val first = repository.addPending(capture(1))
        val second = repository.addPending(capture(2))

        first.id shouldBe 1
        second.id shouldBe 2
        repository.pending() shouldBe listOf(second, first)
        repository.pendingFlow().first() shouldBe listOf(second, first)
    }

    "a pending-inbox write publishes a change for the listener's connection" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))
        val before = CaptureRepository.inboxChanges.value

        repository.addPending(capture(1))

        CaptureRepository.inboxChanges.value shouldBeGreaterThan before
    }

    "addPending caps the inbox and drops the oldest captures" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))

        repeat(MAX_PENDING_CAPTURES + 5) { index -> repository.addPending(capture((index + 1).toLong())) }

        val pending = repository.pending()
        pending.size shouldBe MAX_PENDING_CAPTURES
        pending.first().id shouldBe (MAX_PENDING_CAPTURES + 5).toLong()
        pending.last().id shouldBe 6L
    }

    "removePending removes only the given capture" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))
        repository.addPending(capture(1))
        val second = repository.addPending(capture(2))

        repository.removePending(second.id)

        repository.pending().map { it.id } shouldBe listOf(1L)
    }

    "clearPending empties the inbox" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))
        repository.addPending(capture(1))
        repository.addPending(capture(2))

        repository.clearPending()

        repository.pending().shouldBeEmpty()
    }

    "the seen watermark starts at zero and round-trips" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))

        repository.seenCaptureId() shouldBe 0L
        repository.seenCaptureIdFlow().first() shouldBe 0L

        repository.markSeen(7L)

        repository.seenCaptureId() shouldBe 7L
        repository.seenCaptureIdFlow().first() shouldBe 7L
    }

    "markSeen never moves the watermark backwards" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))
        repository.markSeen(7L)

        repository.markSeen(3L)

        repository.seenCaptureId() shouldBe 7L
    }

    "a corrupt watermark reads as zero" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))
        database.settingsQueries.insertOrReplace(CAPTURE_SEEN_KEY, "not a number")

        repository.seenCaptureId() shouldBe 0L
    }

    "addPending keeps new ids above the acknowledged watermark" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))
        repository.addPending(capture(1))
        repository.addPending(capture(2))
        repository.clearPending()
        repository.markSeen(2L)

        val next = repository.addPending(capture(3))

        next.id shouldBe 3L
        repository.pending() shouldBe listOf(next)
    }

    "completeCapture learns a template and removes the capture" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))
        val pending = repository.addPending(capture(1).copy(description = "You paid CHF 12.50 at COOP"))

        repository.completeCapture(pending.id, entry())

        repository.pending().shouldBeEmpty()
        repository.templates() shouldBe listOf(
            CaptureTemplate("com.example.bank", "you paid chf #.# at coop", EntryType.Expense, 1L, 2L, "COOP"),
        )
        repository.templatesFlow().first() shouldBe repository.templates()
    }

    "completeCapture replaces the template of the same package and skeleton, newest first" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))
        val first = repository.addPending(capture(1).copy(description = "You paid CHF 12.50 at COOP"))
        repository.completeCapture(first.id, entry(categoryId = 1L))
        val second = repository.addPending(capture(2).copy(description = "You paid CHF 8.00 at COOP"))

        repository.completeCapture(second.id, entry(categoryId = 5L))

        repository.templates() shouldBe listOf(
            CaptureTemplate("com.example.bank", "you paid chf #.# at coop", EntryType.Expense, 5L, 2L, "COOP"),
        )
    }

    "completeCapture leaves everything alone when the capture is gone" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))

        repository.completeCapture(99L, entry())

        repository.pending().shouldBeEmpty()
        repository.templates().shouldBeEmpty()
    }

    "completeCapture still removes a capture that teaches nothing" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))
        val pending = repository.addPending(capture(1))
        val transfer = TransferEntry(
            id = 0,
            category = null,
            description = "Moved",
            account = Account(1L, "Main", Currency.CHF),
            amount = 1250,
            createdAt = TEST_NOW,
            toAccount = Account(2L, "Savings", Currency.CHF),
            toAmount = null,
        )

        repository.completeCapture(pending.id, transfer)

        repository.pending().shouldBeEmpty()
        repository.templates().shouldBeEmpty()
    }

    "templates cap and drop the oldest learned" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))

        repeat(MAX_CAPTURE_TEMPLATES + 5) { index ->
            val pending = repository.addPending(
                capture(1).copy(description = "Payment ${"w".repeat(index + 1)}"),
            )
            repository.completeCapture(pending.id, entry())
        }

        val templates = repository.templates()
        templates.size shouldBe MAX_CAPTURE_TEMPLATES
        templates.first().skeleton shouldBe "payment ${"w".repeat(MAX_CAPTURE_TEMPLATES + 5)}"
        templates.last().skeleton shouldBe "payment ${"w".repeat(6)}"
    }

    "removeTemplate deletes only the matching template" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))
        val first = repository.addPending(capture(1).copy(description = "You paid CHF 12.50 at COOP"))
        repository.completeCapture(first.id, entry())
        val second = repository.addPending(
            capture(2).copy(packageName = "other.app", description = "You paid CHF 12.50 at COOP"),
        )
        repository.completeCapture(second.id, entry())

        repository.removeTemplate("com.example.bank", "you paid chf #.# at coop")

        repository.templates() shouldBe listOf(
            CaptureTemplate("other.app", "you paid chf #.# at coop", EntryType.Expense, 1L, 2L, "COOP"),
        )
    }

    "a corrupt templates value reads as empty" {
        val database = lindenDatabase()
        val repository = CaptureRepository(SettingsDao(database.settingsQueries))
        database.settingsQueries.insertOrReplace(CAPTURE_TEMPLATES_KEY, "{not json")

        repository.templates().shouldBeEmpty()
        repository.templatesFlow().first().shouldBeEmpty()
    }
})
