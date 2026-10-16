package org.sjbtimdan.linden.ui.capture

import androidx.compose.ui.test.ExperimentalTestApi
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.capture.CaptureRule
import org.sjbtimdan.linden.capture.PendingCapture
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.ui.withCapturedPaymentsViewModel

private fun capture(index: Long, packageName: String = "com.example.bank") = PendingCapture(
    packageName = packageName,
    amountMinor = 2450,
    currency = Currency.CHF,
    description = "Payment $index",
    type = EntryType.Expense,
    capturedAtMillis = index,
)

@OptIn(ExperimentalTestApi::class)
class CapturedPaymentsViewModelTest : StringSpec({

    "items resolve the amount text and the mapped account" {
        withCapturedPaymentsViewModel { repository, accountDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            val main = accountDao.getAll().first().first()
            repository.setRule(CaptureRule("com.example.bank", main.id))
            repository.addPending(capture(1))

            val item = viewModel.items.first { it.isNotEmpty() }.first()

            item.amountText shouldBe "24.50"
            item.accountName shouldBe "Main"
            item.capture.description shouldBe "Payment 1"
        }
    }

    "buildDraft prefills amount, description, type and account" {
        withCapturedPaymentsViewModel { repository, accountDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            val main = accountDao.getAll().first().first()
            repository.setRule(CaptureRule("com.example.bank", main.id))

            val draft = viewModel.buildDraft(
                PendingCapture(
                    packageName = "com.example.bank",
                    amountMinor = 24_500,
                    currency = Currency.CHF,
                    description = "COOP",
                    type = EntryType.Expense,
                    capturedAtMillis = 1L,
                ),
            )

            draft.editing shouldBe null
            draft.type shouldBe EntryType.Expense
            draft.amountText shouldBe "245.00"
            draft.description shouldBe "COOP"
            draft.accountId shouldBe main.id
        }
    }

    "buildDraft falls back to no account when the rule is gone" {
        withCapturedPaymentsViewModel { _, _, viewModel ->
            val draft = viewModel.buildDraft(
                PendingCapture(
                    packageName = "app.gone",
                    amountMinor = 500,
                    description = "",
                    type = EntryType.Income,
                    capturedAtMillis = 1L,
                ),
            )

            draft.accountId.shouldBeNull()
            draft.type shouldBe EntryType.Income
            draft.amountText shouldBe "5.00"
        }
    }

    "an amount without a captured currency uses the mapped account's digits" {
        withCapturedPaymentsViewModel { repository, accountDao, viewModel ->
            accountDao.create("Yen", Currency.JPY)
            val yen = accountDao.getAll().first().first()
            repository.setRule(CaptureRule("app.bank", yen.id))
            repository.addPending(
                PendingCapture(
                    packageName = "app.bank",
                    amountMinor = 120_000,
                    description = "Yen payment",
                    type = EntryType.Expense,
                    capturedAtMillis = 1L,
                ),
            )

            val item = viewModel.items.first { it.isNotEmpty() }.first()

            item.amountText shouldBe "1,200"
        }
    }

    "remove deletes only the given capture" {
        withCapturedPaymentsViewModel { repository, _, viewModel ->
            repository.addPending(capture(1))
            repository.addPending(capture(2))
            viewModel.items.first { it.size == 2 }

            viewModel.remove(1L)

            viewModel.items.first { it.size == 1 } shouldHaveSize 1
            viewModel.items.value.first().capture.id shouldBe 2L
        }
    }

    "refresh keeps the inbox in sync with storage" {
        withCapturedPaymentsViewModel { repository, _, viewModel ->
            repository.addPending(capture(1))
            viewModel.items.first { it.isNotEmpty() }

            repository.clearPending()
            viewModel.refresh()

            viewModel.items.first().shouldBeEmpty()
        }
    }

    "hasUnseen starts false and turns true for a waiting capture" {
        withCapturedPaymentsViewModel { repository, _, viewModel ->
            viewModel.hasUnseen.first() shouldBe false

            repository.addPending(capture(1))

            viewModel.hasUnseen.first { it } shouldBe true
        }
    }

    "acknowledge moves the watermark to the newest capture" {
        withCapturedPaymentsViewModel { repository, _, viewModel ->
            repository.addPending(capture(1))
            repository.addPending(capture(2))
            viewModel.items.first { it.size == 2 }

            viewModel.acknowledge()

            viewModel.hasUnseen.first { !it } shouldBe false
            repository.seenCaptureId() shouldBe 2L
        }
    }

    "a capture arriving after an acknowledgement is unseen again" {
        withCapturedPaymentsViewModel { repository, _, viewModel ->
            repository.addPending(capture(1))
            viewModel.hasUnseen.first { it }
            viewModel.acknowledge()
            viewModel.hasUnseen.first { !it }

            repository.addPending(capture(2))

            viewModel.hasUnseen.first { it } shouldBe true
        }
    }

    "buildDraft carries the capture as its source" {
        withCapturedPaymentsViewModel { _, _, viewModel ->
            val draft = viewModel.buildDraft(
                PendingCapture(
                    id = 9L,
                    packageName = "com.example.bank",
                    amountMinor = 2450,
                    currency = Currency.CHF,
                    description = "COOP",
                    type = EntryType.Expense,
                    capturedAtMillis = 1L,
                ),
            )

            draft.sourceCaptureId shouldBe 9L
        }
    }
})
