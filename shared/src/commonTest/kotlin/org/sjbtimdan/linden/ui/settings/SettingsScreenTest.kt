package org.sjbtimdan.linden.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.sjbtimdan.linden.BuildInfo
import org.sjbtimdan.linden.backup.LindenBackupManager
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.imports.buildIvyZip
import org.sjbtimdan.linden.imports.minimalIvyJson
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.ui.withSettingsViewModel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest : StringSpec({
    "clicking Light segment sets theme" {
        withSettingsViewModel(initialTheme = ThemeMode.SYSTEM) { viewModel ->
            setContent { SettingsScreen(viewModel) }
            onNodeWithText("System").assertIsSelected()
            onNodeWithText("Light").performClick()
            onNodeWithText("Light").assertIsSelected()

            viewModel.themeMode.value shouldBe ThemeMode.LIGHT
        }
    }

    "shows the build version footer" {
        withSettingsViewModel { viewModel ->
            setContent { SettingsScreen(viewModel) }
            val dirty = if (BuildInfo.GIT_DIRTY) " (dirty)" else ""
            onNodeWithText(
                "Linden v${BuildInfo.VERSION} (${BuildInfo.GIT_COMMIT}$dirty)",
            ).assertExists()
        }
    }

    "clicking EUR chip sets default currency" {
        withSettingsViewModel(initialCurrency = Currency.CHF) { viewModel ->
            setContent { SettingsScreen(viewModel) }
            onNodeWithText("CHF").assertIsSelected()
            onNodeWithText("EUR").performClick()
            onNodeWithText("EUR").assertIsSelected()

            viewModel.defaultCurrency.value shouldBe Currency.EUR
        }
    }

    "toggling the hide total switch updates the setting" {
        withSettingsViewModel { viewModel ->
            setContent { SettingsScreen(viewModel) }
            onNode(isToggleable()).assertIsOff()
            onNode(isToggleable()).performClick()
            onNode(isToggleable()).assertIsOn()

            viewModel.hideEntryTotal.value shouldBe true
        }
    }

    "clicking Categories row triggers navigation" {
        withSettingsViewModel { viewModel ->
            var navigatedToCategories = false

            setContent {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToCategories = { navigatedToCategories = true },
                )
            }
            onNodeWithText("Categories").performClick()
            navigatedToCategories shouldBe true
        }
    }

    "clicking Accounts row triggers navigation" {
        withSettingsViewModel { viewModel ->
            var navigatedToAccounts = false

            setContent {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToAccounts = { navigatedToAccounts = true },
                )
            }
            onNodeWithText("Accounts").performClick()
            navigatedToAccounts shouldBe true
        }
    }

    "clicking Import in the confirmation dialog triggers the file picker" {
        withSettingsViewModel { viewModel ->
            var pickerInvoked = false

            setContent {
                SettingsScreen(
                    viewModel = viewModel,
                    pickImportFile = { pickerInvoked = true },
                )
            }

            onNodeWithText("Import from Ivy").performClick()
            onNodeWithText(
                "This will replace all your current accounts, categories and transactions. Continue?",
            ).assertExists()
            onNodeWithText("Import").performClick()

            pickerInvoked shouldBe true
        }
    }

    "clicking Cancel in the confirmation dialog does not trigger the file picker" {
        withSettingsViewModel { viewModel ->
            var pickerInvoked = false

            setContent {
                SettingsScreen(
                    viewModel = viewModel,
                    pickImportFile = { pickerInvoked = true },
                )
            }

            onNodeWithText("Import from Ivy").performClick()
            onNodeWithText("Cancel").performClick()

            pickerInvoked shouldBe false
            onAllNodesWithText(
                "This will replace all your current accounts, categories and transactions. Continue?",
            ).assertCountEquals(0)
        }
    }

    "clicking Currency rates row triggers navigation" {
        withSettingsViewModel { viewModel ->
            var navigatedToRates = false

            setContent {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToRates = { navigatedToRates = true },
                )
            }
            onNodeWithText("Currency rates").performClick()
            navigatedToRates shouldBe true
        }
    }

    "import success shows a note when transactions were routed to split accounts" {
        withSettingsViewModel { viewModel ->
            setContent { SettingsScreen(viewModel) }

            val json = """
                {
                  "accounts": [
                    {"id": "a1", "name": "HSBC", "currency": "USD"}
                  ],
                  "categories": [
                    {"id": "c1", "name": "Food"}
                  ],
                  "transactions": [
                    {
                      "id": "t1",
                      "type": "EXPENSE",
                      "amount": 10.0,
                      "accountId": "a1",
                      "categoryId": "c1",
                      "currency": "EUR",
                      "title": "EUR purchase",
                      "dateTime": 946684800000
                    }
                  ]
                }
            """.trimIndent()

            viewModel.importIvy(ByteArrayInputStream(buildIvyZip(json)))
            withTimeout(5_000) { viewModel.importState.first { it is ImportState.Success } }

            onNodeWithText(
                "Note: 1 transaction(s) were in a different currency than their account " +
                    "and were imported into new \"IVY: …\" accounts.",
                substring = true,
            ).assertExists()
        }
    }

    "import success without conflicts shows no split-account note" {
        withSettingsViewModel { viewModel ->
            setContent { SettingsScreen(viewModel) }

            viewModel.importIvy(ByteArrayInputStream(buildIvyZip(minimalIvyJson)))
            withTimeout(5_000) { viewModel.importState.first { it is ImportState.Success } }

            onNodeWithText("Imported 3 accounts, 3 categories, 5 transactions").assertExists()
            onAllNodesWithText("different currency", substring = true).assertCountEquals(0)
        }
    }

    "clicking Back up database triggers the backup picker" {
        withSettingsViewModel { viewModel ->
            var pickerInvoked = false

            setContent {
                SettingsScreen(
                    viewModel = viewModel,
                    pickBackupFile = { pickerInvoked = true },
                )
            }

            onNodeWithText("Back up database").performClick()

            pickerInvoked shouldBe true
        }
    }

    "clicking Restore in the confirmation dialog triggers the restore picker" {
        withSettingsViewModel { viewModel ->
            var pickerInvoked = false

            setContent {
                SettingsScreen(
                    viewModel = viewModel,
                    pickRestoreFile = { pickerInvoked = true },
                )
            }

            onNodeWithText("Restore from backup").performClick()
            onNodeWithText(
                "This will replace all your current accounts, categories, transactions and settings. Continue?",
            ).assertExists()
            onNodeWithText("Restore").performClick()

            pickerInvoked shouldBe true
        }
    }

    "clicking Cancel in the restore confirmation dialog does not trigger the restore picker" {
        withSettingsViewModel { viewModel ->
            var pickerInvoked = false

            setContent {
                SettingsScreen(
                    viewModel = viewModel,
                    pickRestoreFile = { pickerInvoked = true },
                )
            }

            onNodeWithText("Restore from backup").performClick()
            onNodeWithText("Cancel").performClick()

            pickerInvoked shouldBe false
            onAllNodesWithText(
                "This will replace all your current accounts, categories, transactions and settings. Continue?",
            ).assertCountEquals(0)
        }
    }

    "restore success shows the restored counts" {
        withSettingsViewModel { viewModel ->
            setContent { SettingsScreen(viewModel) }

            val source = lindenDatabase()
            source.accountQueries.insert("Cash", "CHF", 0)
            val bytes = ByteArrayOutputStream().also { LindenBackupManager(source).backupTo(it) }.toByteArray()
            viewModel.restoreFrom(ByteArrayInputStream(bytes))
            withTimeout(5_000) { viewModel.restoreState.first { it is BackupState.Success } }

            onNodeWithText("Restored 1 accounts, 0 categories, 0 entries").assertExists()
        }
    }

    "backup success shows a confirmation row" {
        withSettingsViewModel { viewModel ->
            setContent { SettingsScreen(viewModel) }

            viewModel.backupTo(ByteArrayOutputStream())
            withTimeout(5_000) { viewModel.backupState.first { it is BackupState.Success } }

            onNodeWithText("Backup saved.").assertExists()
        }
    }

    "restore error shows the failure message" {
        withSettingsViewModel { viewModel ->
            setContent { SettingsScreen(viewModel) }

            viewModel.restoreFrom(ByteArrayInputStream("garbage".encodeToByteArray()))
            withTimeout(5_000) { viewModel.restoreState.first { it is BackupState.Error } }

            onNodeWithText("Restore failed:", substring = true).assertExists()
        }
    }
})
