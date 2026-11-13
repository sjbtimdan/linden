package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import io.kotest.core.spec.style.StringSpec
import kotlinx.datetime.LocalDate
import org.sjbtimdan.linden.data.seed
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.time.TEST_NOW
import org.sjbtimdan.linden.ui.withLedgerViewModel

/**
 * The period toolbar must keep the period label, the show-future toggle and the
 * period total all visible at phone widths. Regresses the case where the toolbar
 * was crowded by the Insights shortcut and clipped the toggle (then the label).
 */
@OptIn(ExperimentalTestApi::class)
class LedgerScreenNarrowWidthTest : StringSpec({
    "period label, show-future toggle and total all fit on the current month at a phone width" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 11, 10)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = TEST_NOW))
            viewModel.setPeriod(LedgerPeriod.Month)
            viewModel.goToToday()

            setContent {
                Box(Modifier.requiredSize(width = 360.dp, height = 800.dp)) {
                    LedgerScreen(viewModel = viewModel)
                }
            }

            onNodeWithText("Nov 2026").assertIsDisplayed()
            onNodeWithTag("showFutureToggle").assertIsDisplayed()
            onNodeWithTag("totalLabel").assertIsDisplayed()
        }
    }

    "period label, show-future toggle and total all fit on All at a phone width" {
        withLedgerViewModel(clock = FakeClock(today = LocalDate(2026, 11, 10))) { accountDao, categoryDao, viewModel ->
            viewModel.setPeriod(LedgerPeriod.All)

            setContent {
                Box(Modifier.requiredSize(width = 360.dp, height = 800.dp)) {
                    LedgerScreen(viewModel = viewModel)
                }
            }

            onNodeWithTag("showFutureToggle").assertIsDisplayed()
            onNodeWithTag("totalLabel").assertIsDisplayed()
        }
    }
})
