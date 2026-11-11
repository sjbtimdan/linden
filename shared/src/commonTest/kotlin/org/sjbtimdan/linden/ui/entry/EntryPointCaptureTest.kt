package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.capture.CaptureRepository
import org.sjbtimdan.linden.capture.PendingCapture
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.data.seed
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.time.TEST_NOW
import org.sjbtimdan.linden.ui.capture.CapturedPaymentsViewModel
import org.sjbtimdan.linden.ui.withEntryPoint

@OptIn(ExperimentalTestApi::class)
class EntryPointCaptureTest : StringSpec({
    "a draft loaded from a capture shows the missing-category hint right away" {
        withEntryPoint(clock = FakeClock()) { accountDao, categoryDao, viewModel ->
            val (main, _) = seed(accountDao, categoryDao)
            viewModel.loadDraft(
                EntryDraft.forNew(EntryType.Expense, clock = FakeClock()).copy(
                    amountText = "24.50",
                    description = "COOP",
                    accountId = main.id,
                    sourceCaptureId = 7L,
                ),
            )

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            waitForText("Choose a category")
            onNodeWithText("Choose a category").assertIsDisplayed()
            onNodeWithText("Add").assertIsNotEnabled()
        }
    }

    "a captured payment alert shows on the entry screen and prefills the draft" {
        withEntryPoint(clock = FakeClock()) { accountDao, categoryDao, viewModel ->
            val captureRepository = CaptureRepository(SettingsDao(lindenDatabase().settingsQueries))
            val captureViewModel = CapturedPaymentsViewModel(
                captureRepository,
                accountDao,
                categoryDao,
                FakeClock(),
            )
            captureRepository.addPending(
                PendingCapture(
                    packageName = "com.example.bank",
                    amountMinor = 2450,
                    currency = Currency.CHF,
                    description = "COOP",
                    type = EntryType.Expense,
                    capturedAtMillis = 1L,
                ),
            )

            setContent {
                EntryPoint(viewModel = viewModel, captureViewModel = captureViewModel)
            }

            onNodeWithTag("capturedPaymentsBar").assertIsDisplayed()
            onNodeWithText("1 payment to review").performClick()
            onNodeWithTag("capturedPaymentsCard").assertIsDisplayed()
            onNodeWithText("COOP").assertIsDisplayed()

            onNodeWithTag("capturedPaymentAdd-1").performClick()

            viewModel.draft.value?.sourceCaptureId shouldBe 1L
            viewModel.draft.value?.amountText shouldBe "24.50"
            viewModel.draft.value?.description shouldBe "COOP"
        }
    }

    "saving a captured draft dismisses its alert" {
        lateinit var captureRepository: CaptureRepository
        withEntryPoint(
            clock = FakeClock(),
            onCaptureConsumed = { id, entry -> captureRepository.completeCapture(id, entry) },
        ) { _, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            captureRepository = CaptureRepository(SettingsDao(lindenDatabase().settingsQueries))
            val captureViewModel = CapturedPaymentsViewModel(
                captureRepository,
                accountDao,
                categoryDao,
                FakeClock(),
            )
            val pending = captureRepository.addPending(
                PendingCapture(
                    packageName = "com.example.bank",
                    amountMinor = 2450,
                    currency = Currency.CHF,
                    description = "COOP",
                    type = EntryType.Expense,
                    capturedAtMillis = 1L,
                ),
            )
            viewModel.loadDraft(
                EntryDraft.forNew(EntryType.Expense, clock = FakeClock()).copy(
                    amountText = "24.50",
                    description = "COOP",
                    accountId = main.id,
                    categoryId = groceries.id,
                    sourceCaptureId = pending.id,
                ),
            )

            setContent {
                EntryPoint(viewModel = viewModel, captureViewModel = captureViewModel)
            }
            onNodeWithTag("capturedPaymentsBar").assertIsDisplayed()

            onNodeWithTag("saveEntry").performClick()
            waitForIdle()

            captureRepository.pending().shouldBeEmpty()
            onNodeWithTag("capturedPaymentsBar").assertDoesNotExist()
            onNodeWithTag("capturedPaymentsCard").assertDoesNotExist()
        }
    }

    "saving an entry added from the alert dismisses it" {
        lateinit var captureRepository: CaptureRepository
        withEntryPoint(
            clock = FakeClock(),
            onCaptureConsumed = { id, entry -> captureRepository.completeCapture(id, entry) },
        ) { _, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            captureRepository = CaptureRepository(SettingsDao(lindenDatabase().settingsQueries))
            val captureViewModel = CapturedPaymentsViewModel(
                captureRepository,
                accountDao,
                categoryDao,
                FakeClock(),
            )
            // Teach a template for this wording so the alert's Add prefills a
            // complete draft, exactly as a repeat notification would.
            val learned = captureRepository.addPending(
                PendingCapture(
                    packageName = "com.example.bank",
                    amountMinor = 2450,
                    currency = Currency.CHF,
                    description = "COOP",
                    type = EntryType.Expense,
                    capturedAtMillis = 1L,
                ),
            )
            captureRepository.completeCapture(
                learned.id,
                ExpenseEntry(0, groceries, "COOP", main, 2450, createdAt = TEST_NOW),
            )
            captureRepository.templatesFlow().first { it.isNotEmpty() }
            val pending = captureRepository.addPending(
                PendingCapture(
                    packageName = "com.example.bank",
                    amountMinor = 2450,
                    currency = Currency.CHF,
                    description = "COOP",
                    type = EntryType.Expense,
                    capturedAtMillis = 2L,
                ),
            )

            setContent {
                EntryPoint(viewModel = viewModel, captureViewModel = captureViewModel)
            }
            onNodeWithText("1 payment to review").performClick()
            onNodeWithTag("capturedPaymentsCard").assertIsDisplayed()

            onNodeWithTag("capturedPaymentAdd-${pending.id}").performClick()
            onNodeWithTag("saveEntry").performClick()
            waitForIdle()

            captureRepository.pending().shouldBeEmpty()
            onNodeWithTag("capturedPaymentsBar").assertDoesNotExist()
            onNodeWithTag("capturedPaymentsCard").assertDoesNotExist()
        }
    }
})
