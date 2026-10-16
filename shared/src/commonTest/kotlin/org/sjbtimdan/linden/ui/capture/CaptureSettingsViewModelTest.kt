package org.sjbtimdan.linden.ui.capture

import androidx.compose.ui.test.ExperimentalTestApi
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.capture.CaptureRule
import org.sjbtimdan.linden.capture.CaptureTemplate
import org.sjbtimdan.linden.capture.PendingCapture
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.time.TEST_NOW
import org.sjbtimdan.linden.ui.withCaptureSettingsViewModel

@OptIn(ExperimentalTestApi::class)
class CaptureSettingsViewModelTest : StringSpec({

    "rules start empty and follow updates" {
        withCaptureSettingsViewModel { repository, _, _, viewModel ->
            viewModel.rules.value.shouldBeEmpty()

            viewModel.setRule("com.example.bank", accountId = 5L)

            viewModel.rules.first { it.isNotEmpty() } shouldBe listOf(CaptureRule("com.example.bank", 5L))
            repository.rules() shouldBe listOf(CaptureRule("com.example.bank", 5L))
        }
    }

    "setRuleEnabled and removeRule update only the matching rule" {
        withCaptureSettingsViewModel { _, _, _, viewModel ->
            viewModel.setRule("app.a", 1L)
            viewModel.setRule("app.b", 2L)

            viewModel.setRuleEnabled("app.a", false)
            viewModel.rules.first { it.any { rule -> !rule.enabled } } shouldBe
                listOf(CaptureRule("app.a", 1L, enabled = false), CaptureRule("app.b", 2L))

            viewModel.removeRule("app.b")

            viewModel.rules.first { it.size == 1 } shouldBe listOf(CaptureRule("app.a", 1L, enabled = false))
        }
    }

    "templates follow learned writes and removeTemplate forgets one" {
        withCaptureSettingsViewModel { repository, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            categoryDao.create("Groceries", CategoryType.Expense)
            val main = accountDao.getAll().first().first()
            val groceries = categoryDao.getAll().first().first()
            repository.addPending(
                PendingCapture(
                    packageName = "com.example.bank",
                    amountMinor = 1250,
                    currency = Currency.CHF,
                    description = "You paid CHF 12.50 at COOP",
                    type = EntryType.Expense,
                    capturedAtMillis = 1L,
                ),
            )
            repository.completeCapture(
                1L,
                ExpenseEntry(0, groceries, "COOP", main, 1250, createdAt = TEST_NOW, createdZone = TimeZone.UTC),
            )

            viewModel.templates.first { it.isNotEmpty() } shouldBe listOf(
                CaptureTemplate(
                    packageName = "com.example.bank",
                    skeleton = "you paid chf #.# at coop",
                    type = EntryType.Expense,
                    categoryId = groceries.id,
                    accountId = main.id,
                    description = "COOP",
                ),
            )

            viewModel.removeTemplate("com.example.bank", "you paid chf #.# at coop")

            viewModel.templates.first().shouldBeEmpty()
            repository.templates().shouldBeEmpty()
        }
    }
})
