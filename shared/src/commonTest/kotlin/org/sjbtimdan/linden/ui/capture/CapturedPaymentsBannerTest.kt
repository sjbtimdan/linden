package org.sjbtimdan.linden.ui.capture

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.capture.CaptureRule
import org.sjbtimdan.linden.capture.PendingCapture
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.ui.withCapturedPaymentsViewModel

private fun pending() = PendingCapture(
    packageName = "com.example.bank",
    amountMinor = 2450,
    currency = Currency.CHF,
    description = "COOP",
    type = EntryType.Expense,
    capturedAtMillis = 1L,
)

@OptIn(ExperimentalTestApi::class)
class CapturedPaymentsBannerTest : StringSpec({

    "an empty inbox renders nothing" {
        withCapturedPaymentsViewModel { _, _, _, viewModel ->
            setContent {
                CapturedPaymentsBanner(viewModel = viewModel, onAdd = {})
            }

            onNodeWithTag("capturedPaymentsCard").assertDoesNotExist()
            onNodeWithTag("capturedPaymentsBar").assertDoesNotExist()
        }
    }

    "an unseen capture opens the card with its row" {
        withCapturedPaymentsViewModel { repository, accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF)
            val main = accountDao.getAll().first().first()
            repository.setRule(CaptureRule("com.example.bank", main.id))
            repository.addPending(pending())
            setContent {
                CapturedPaymentsBanner(viewModel = viewModel, onAdd = {})
            }

            onNodeWithTag("capturedPaymentsCard").assertExists()
            onNodeWithText("COOP").assertExists()
            onNodeWithText("24.50").assertExists()
            onNodeWithText("com.example.bank · Main").assertExists()
        }
    }

    "Add reports the capture to add" {
        withCapturedPaymentsViewModel { repository, _, _, viewModel ->
            repository.addPending(pending())
            var added: PendingCapture? = null
            setContent {
                CapturedPaymentsBanner(viewModel = viewModel, onAdd = { added = it })
            }

            onNodeWithText("Add").performClick()

            added?.amountMinor shouldBe 2450
        }
    }

    "Dismiss removes the capture from the inbox" {
        withCapturedPaymentsViewModel { repository, _, _, viewModel ->
            repository.addPending(pending())
            setContent {
                CapturedPaymentsBanner(viewModel = viewModel, onAdd = {})
            }

            onNodeWithText("Dismiss").performClick()

            repository.pending().shouldBeEmpty()
        }
    }

    "collapsing acknowledges the inbox and leaves the bar" {
        withCapturedPaymentsViewModel { repository, _, _, viewModel ->
            repository.addPending(pending())
            viewModel.hasUnseen.first { it }
            setContent {
                CapturedPaymentsBanner(viewModel = viewModel, onAdd = {})
            }

            onNodeWithContentDescription("Collapse").performClick()

            viewModel.hasUnseen.first { !it } shouldBe false
            repository.seenCaptureId() shouldBe 1L
            onNodeWithTag("capturedPaymentsCard").assertDoesNotExist()
            onNodeWithText("1 payment to review").assertExists()
        }
    }

    "tapping the bar opens the card again" {
        withCapturedPaymentsViewModel { repository, _, _, viewModel ->
            repository.addPending(pending())
            viewModel.hasUnseen.first { it }
            viewModel.acknowledge()
            viewModel.hasUnseen.first { !it }
            setContent {
                CapturedPaymentsBanner(viewModel = viewModel, onAdd = {})
            }

            onNodeWithText("1 payment to review").performClick()

            onNodeWithTag("capturedPaymentsCard").assertExists()
        }
    }

    "the collapsed bar counts the waiting captures" {
        withCapturedPaymentsViewModel { repository, _, _, viewModel ->
            repository.addPending(pending())
            repository.addPending(pending())
            viewModel.hasUnseen.first { it }
            viewModel.acknowledge()
            viewModel.hasUnseen.first { !it }
            setContent {
                CapturedPaymentsBanner(viewModel = viewModel, onAdd = {})
            }

            onNodeWithText("2 payments to review").assertExists()
        }
    }

    "a capture arriving after a dismissal opens the card again" {
        withCapturedPaymentsViewModel { repository, _, _, viewModel ->
            repository.addPending(pending())
            viewModel.hasUnseen.first { it }
            viewModel.acknowledge()
            viewModel.hasUnseen.first { !it }
            setContent {
                CapturedPaymentsBanner(viewModel = viewModel, onAdd = {})
            }
            onNodeWithTag("capturedPaymentsBar").assertExists()

            repository.addPending(pending())

            viewModel.hasUnseen.first { it } shouldBe true
            onNodeWithTag("capturedPaymentsCard").assertExists()
        }
    }
})
