package org.sjbtimdan.linden.ui.settings

import app.cash.sqldelight.async.coroutines.awaitAsList
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.sjbtimdan.linden.backup.LindenBackupManager
import org.sjbtimdan.linden.backup.buildBackupZip
import org.sjbtimdan.linden.data.CURRENCY_KEY
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.HIDE_ENTRY_TOTAL_KEY
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.THEME_KEY
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.export.CsvExportManager
import org.sjbtimdan.linden.imports.IvyImporter
import org.sjbtimdan.linden.imports.buildIvyZip
import org.sjbtimdan.linden.imports.minimalIvyJson
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.ui.onTestMain
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream

class SettingsViewModelTest : StringSpec({
    "setThemeMode(LIGHT) updates the database" {
        onTestMain {
            val database = lindenDatabase()
            val dao = SettingsDao(database.settingsQueries)
            val viewModel = SettingsViewModel(
                dao,
                IvyImporter(database),
                backupManager = LindenBackupManager(database),
                csvExporter = CsvExportManager(EntryDao(database.entryQueries)),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
            )

            viewModel.themeMode.value shouldBe ThemeMode.SYSTEM

            viewModel.setThemeMode(ThemeMode.LIGHT)

            dao.getTheme() shouldBe ThemeMode.LIGHT
        }
    }

    "setDefaultCurrency(EUR) updates the database" {
        onTestMain {
            val database = lindenDatabase()
            val dao = SettingsDao(database.settingsQueries)
            val viewModel = SettingsViewModel(
                dao,
                IvyImporter(database),
                backupManager = LindenBackupManager(database),
                csvExporter = CsvExportManager(EntryDao(database.entryQueries)),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
            )

            viewModel.defaultCurrency.value shouldBe Currency.CHF

            viewModel.setDefaultCurrency(Currency.EUR)

            dao.getDefaultCurrency() shouldBe Currency.EUR
        }
    }

    "setHideEntryTotal(true) updates the database" {
        onTestMain {
            val database = lindenDatabase()
            val dao = SettingsDao(database.settingsQueries)
            val viewModel = SettingsViewModel(
                dao,
                IvyImporter(database),
                backupManager = LindenBackupManager(database),
                csvExporter = CsvExportManager(EntryDao(database.entryQueries)),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
            )

            viewModel.hideEntryTotal.value shouldBe false

            viewModel.setHideEntryTotal(true)

            dao.getHideEntryTotal() shouldBe true
        }
    }

    "importIvy imports a valid backup and reports a success result" {
        onTestMain {
            val database = lindenDatabase()
            val viewModel = SettingsViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                importer = IvyImporter(database),
                backupManager = LindenBackupManager(database),
                csvExporter = CsvExportManager(EntryDao(database.entryQueries)),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
            )

            viewModel.importIvy(ByteArrayInputStream(buildIvyZip(minimalIvyJson)))

            val state = withTimeout(5_000) {
                viewModel.importState.first { it is ImportState.Success }
            }

            val result = (state as ImportState.Success).result
            result.accounts shouldBe 3
            result.categories shouldBe 3
            result.transactions shouldBe 5
        }
    }

    "importIvy reports splitTransactions when a transaction's currency conflicts with its account" {
        onTestMain {
            val database = lindenDatabase()
            val viewModel = SettingsViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                importer = IvyImporter(database),
                backupManager = LindenBackupManager(database),
                csvExporter = CsvExportManager(EntryDao(database.entryQueries)),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
            )

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

            val state = withTimeout(5_000) {
                viewModel.importState.first { it is ImportState.Success }
            }

            val result = (state as ImportState.Success).result
            result.accounts shouldBe 2
            result.splitTransactions shouldBe 1
        }
    }

    "importIvy reports an error for an invalid backup and leaves the database untouched" {
        onTestMain {
            val database = lindenDatabase()
            val viewModel = SettingsViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                importer = IvyImporter(database),
                backupManager = LindenBackupManager(database),
                csvExporter = CsvExportManager(EntryDao(database.entryQueries)),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
            )

            viewModel.importIvy(ByteArrayInputStream(buildIvyZip("this is not json")))

            val state = withTimeout(5_000) {
                viewModel.importState.first { it is ImportState.Error }
            }

            (state as ImportState.Error).message shouldContain "not a valid Ivy backup"
            database.accountQueries.selectAll().awaitAsList() shouldBe emptyList()
        }
    }

    "clearImportState resets the import state to Idle" {
        onTestMain {
            val database = lindenDatabase()
            val viewModel = SettingsViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                importer = IvyImporter(database),
                backupManager = LindenBackupManager(database),
                csvExporter = CsvExportManager(EntryDao(database.entryQueries)),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
            )

            viewModel.importIvy(ByteArrayInputStream(buildIvyZip(minimalIvyJson)))
            withTimeout(5_000) {
                viewModel.importState.first { it is ImportState.Success }
            }

            viewModel.clearImportState()

            viewModel.importState.value shouldBe ImportState.Idle
        }
    }

    "backupTo writes a backup that restores into another database" {
        onTestMain {
            val database = lindenDatabase()
            database.accountQueries.insert("Cash", "CHF", 500)
            val viewModel = SettingsViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                importer = IvyImporter(database),
                backupManager = LindenBackupManager(database),
                csvExporter = CsvExportManager(EntryDao(database.entryQueries)),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
            )

            val bytes = ByteArrayOutputStream()
            viewModel.backupTo(bytes)
            withTimeout(5_000) {
                viewModel.backupState.first { it is BackupState.Success }
            }

            val restored = lindenDatabase()
            val result = LindenBackupManager(restored).restoreFrom(ByteArrayInputStream(bytes.toByteArray()))

            result.accounts shouldBe 1
            restored.accountQueries.selectAll().awaitAsList().map { it.name to it.currency }
                .shouldBe(listOf("Cash" to "CHF"))
        }
    }

    "restoreFrom replaces database contents and reloads theme, currency and entry-total visibility" {
        onTestMain {
            val source = lindenDatabase()
            source.settingsQueries.insertOrReplace(THEME_KEY, ThemeMode.DARK.name)
            source.settingsQueries.insertOrReplace(CURRENCY_KEY, Currency.EUR.name)
            source.settingsQueries.insertOrReplace(HIDE_ENTRY_TOTAL_KEY, "true")
            source.accountQueries.insert("Cash", "CHF", 0)
            val bytes = ByteArrayOutputStream().also { LindenBackupManager(source).backupTo(it) }.toByteArray()

            val database = lindenDatabase()
            database.settingsQueries.insertOrReplace(THEME_KEY, ThemeMode.LIGHT.name)
            database.accountQueries.insert("Old", "USD", 0)
            val viewModel = SettingsViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                importer = IvyImporter(database),
                backupManager = LindenBackupManager(database),
                csvExporter = CsvExportManager(EntryDao(database.entryQueries)),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
            )

            viewModel.restoreFrom(ByteArrayInputStream(bytes))
            val state = withTimeout(5_000) {
                viewModel.restoreState.first { it is BackupState.Success }
            }

            (state as BackupState.Success).value.accounts shouldBe 1
            viewModel.themeMode.value shouldBe ThemeMode.DARK
            viewModel.defaultCurrency.value shouldBe Currency.EUR
            viewModel.hideEntryTotal.value shouldBe true
            database.accountQueries.selectAll().awaitAsList().map { it.name } shouldBe listOf("Cash")
        }
    }

    "restoreFrom reports an error for an invalid backup and leaves the database untouched" {
        onTestMain {
            val database = lindenDatabase()
            database.accountQueries.insert("Keep", "CHF", 0)
            val viewModel = SettingsViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                importer = IvyImporter(database),
                backupManager = LindenBackupManager(database),
                csvExporter = CsvExportManager(EntryDao(database.entryQueries)),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
            )

            viewModel.restoreFrom(ByteArrayInputStream("garbage".encodeToByteArray()))
            val state = withTimeout(5_000) {
                viewModel.restoreState.first { it is BackupState.Error }
            }

            (state as BackupState.Error).message shouldContain "not a valid Linden backup"
            database.accountQueries.selectAll().awaitAsList().map { it.name } shouldBe listOf("Keep")
        }
    }

    "restoreFrom rejects an empty backup" {
        onTestMain {
            val database = lindenDatabase()
            database.accountQueries.insert("Keep", "CHF", 0)
            val viewModel = SettingsViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                importer = IvyImporter(database),
                backupManager = LindenBackupManager(database),
                csvExporter = CsvExportManager(EntryDao(database.entryQueries)),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
            )

            viewModel.restoreFrom(ByteArrayInputStream(buildBackupZip("""{"formatVersion": 1}""")))
            val state = withTimeout(5_000) {
                viewModel.restoreState.first { it is BackupState.Error }
            }

            (state as BackupState.Error).message shouldContain "empty"
            database.accountQueries.selectAll().awaitAsList().map { it.name } shouldBe listOf("Keep")
        }
    }

    "clearRestoreState resets the restore state to Idle" {
        onTestMain {
            val database = lindenDatabase()
            val viewModel = SettingsViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                importer = IvyImporter(database),
                backupManager = LindenBackupManager(database),
                csvExporter = CsvExportManager(EntryDao(database.entryQueries)),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
            )

            viewModel.restoreFrom(ByteArrayInputStream("garbage".encodeToByteArray()))
            withTimeout(5_000) {
                viewModel.restoreState.first { it is BackupState.Error }
            }

            viewModel.clearRestoreState()

            viewModel.restoreState.value shouldBe BackupState.Idle
        }
    }

    "clearBackupState resets the backup state to Idle" {
        onTestMain {
            val database = lindenDatabase()
            val viewModel = SettingsViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                importer = IvyImporter(database),
                backupManager = LindenBackupManager(database),
                csvExporter = CsvExportManager(EntryDao(database.entryQueries)),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
            )

            viewModel.backupTo(ByteArrayOutputStream())
            withTimeout(5_000) {
                viewModel.backupState.first { it is BackupState.Success }
            }

            viewModel.clearBackupState()

            viewModel.backupState.value shouldBe BackupState.Idle
        }
    }

    "backupTo reports an error when writing the backup fails" {
        onTestMain {
            val database = lindenDatabase()
            val viewModel = SettingsViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                importer = IvyImporter(database),
                backupManager = LindenBackupManager(database),
                csvExporter = CsvExportManager(EntryDao(database.entryQueries)),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
            )

            val failingStream = object : OutputStream() {
                override fun write(b: Int) {
                    error("stream exploded")
                }
            }
            viewModel.backupTo(failingStream)
            val state = withTimeout(5_000) {
                viewModel.backupState.first { it is BackupState.Error }
            }

            (state as BackupState.Error).message shouldContain "stream exploded"
        }
    }
})
