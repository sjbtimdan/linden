package org.sjbtimdan.linden.ui.capture

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
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

    "a waiting capture stays minimized until the bar is tapped" {
        withCapturedPaymentsViewModel { repository, accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF)
            val main = accountDao.getAll().first().first()
            repository.setRule(CaptureRule("com.example.bank", main.id))
            repository.addPending(pending())
            viewModel.hasUnseen.first { it }

            setContent {
                CapturedPaymentsBanner(viewModel = viewModel, onAdd = {})
            }

            onNodeWithTag("capturedPaymentsBar").assertExists()
            onNodeWithText("1 payment to review").assertExists()
            onNodeWithTag("capturedPaymentsCard").assertDoesNotExist()

            onNodeWithText("1 payment to review").performClick()

            onNodeWithTag("capturedPaymentsCard").assertExists()
            onNodeWithText("COOP").assertExists()
            onNodeWithText("24.50").assertExists()
            onNodeWithText("com.example.bank · Main").assertExists()
        }
    }

    "Add reports the capture, clears it and leaves no bar behind" {
        withCapturedPaymentsViewModel { repository, _, _, viewModel ->
            repository.addPending(pending())
            var added: PendingCapture? = null

            setContent {
                CapturedPaymentsBanner(viewModel = viewModel, onAdd = { added = it })
            }

            onNodeWithText("1 payment to review").performClick()
            onNodeWithText("Add").performClick()

            added?.amountMinor shouldBe 2450
            onNodeWithTag("capturedPaymentsCard").assertDoesNotExist()
            onNodeWithTag("capturedPaymentsBar").assertDoesNotExist()
            // Still stored for the save that learns the template; only the UI is cleared.
            repository.pending().size shouldBe 1
        }
    }

    "adding one of several captures clears it and minimizes the rest" {
        withCapturedPaymentsViewModel { repository, _, _, viewModel ->
            repository.addPending(pending())
            repository.addPending(pending())
            viewModel.items.first { it.size == 2 }
            var added: PendingCapture? = null

            setContent {
                CapturedPaymentsBanner(viewModel = viewModel, onAdd = { added = it })
            }

            onNodeWithText("2 payments to review").performClick()
            onNodeWithTag("capturedPaymentAdd-2").performClick()

            added?.id shouldBe 2L
            onNodeWithTag("capturedPaymentsCard").assertDoesNotExist()
            onNodeWithTag("capturedPaymentsBar").assertExists()
            onNodeWithText("1 payment to review").assertExists()

            onNodeWithText("1 payment to review").performClick()
            onAllNodesWithText("COOP").assertCountEquals(1)
        }
    }

    "Dismiss removes the capture from the inbox" {
        withCapturedPaymentsViewModel { repository, _, _, viewModel ->
            repository.addPending(pending())

            setContent {
                CapturedPaymentsBanner(viewModel = viewModel, onAdd = {})
            }

            onNodeWithText("1 payment to review").performClick()
            onNodeWithText("Dismiss").performClick()

            repository.pending().shouldBeEmpty()
            onNodeWithTag("capturedPaymentsBar").assertDoesNotExist()
        }
    }

    "collapsing acknowledges the inbox and leaves the bar" {
        withCapturedPaymentsViewModel { repository, _, _, viewModel ->
            repository.addPending(pending())
            viewModel.hasUnseen.first { it }

            setContent {
                CapturedPaymentsBanner(viewModel = viewModel, onAdd = {})
            }

            onNodeWithText("1 payment to review").performClick()
            onNodeWithContentDescription("Collapse").performClick()

            viewModel.hasUnseen.first { !it } shouldBe false
            repository.seenCaptureId() shouldBe 1L
            onNodeWithTag("capturedPaymentsCard").assertDoesNotExist()
            onNodeWithText("1 payment to review").assertExists()
        }
    }

    "the bar counts multiple waiting captures" {
        withCapturedPaymentsViewModel { repository, _, _, viewModel ->
            repository.addPending(pending())
            repository.addPending(pending())
            viewModel.items.first { it.size == 2 }

            setContent {
                CapturedPaymentsBanner(viewModel = viewModel, onAdd = {})
            }

            onNodeWithText("2 payments to review").assertExists()
            onNodeWithTag("capturedPaymentsCard").assertDoesNotExist()

            onNodeWithText("2 payments to review").performClick()
            onAllNodesWithText("COOP").assertCountEquals(2)
        }
    }

    "a capture arriving after a dismissal updates the bar without opening the card" {
        withCapturedPaymentsViewModel { repository, _, _, viewModel ->
            repository.addPending(pending())
            viewModel.hasUnseen.first { it }

            setContent {
                CapturedPaymentsBanner(viewModel = viewModel, onAdd = {})
            }

            onNodeWithText("1 payment to review").performClick()
            onNodeWithContentDescription("Collapse").performClick()
            onNodeWithTag("capturedPaymentsBar").assertExists()

            repository.addPending(pending())
            viewModel.hasUnseen.first { it } shouldBe true

            onNodeWithTag("capturedPaymentsBar").assertExists()
            onNodeWithText("2 payments to review").assertExists()
            onNodeWithTag("capturedPaymentsCard").assertDoesNotExist()
        }
    }
})
