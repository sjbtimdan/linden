package org.sjbtimdan.linden.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.backup.RestoreResult
import org.sjbtimdan.linden.imports.IvyImportResult
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class BackupRestoreSectionTest : StringSpec({
    "renders the four actions" {
        onTestMain {
            runComposeUiTest {
                setContent { Section() }

                onNodeWithText("Back up database").assertExists()
                onNodeWithText("Export to CSV").assertExists()
                onNodeWithText("Restore from backup").assertExists()
                onNodeWithText("Import from Ivy").assertExists()
            }
        }
    }

    "backup and export report clicks" {
        onTestMain {
            runComposeUiTest {
                var backups = 0
                var exports = 0
                setContent {
                    Section(
                        onBackup = { backups++ },
                        onExport = { exports++ },
                    )
                }

                onNodeWithText("Back up database").performClick()
                onNodeWithText("Export to CSV").performClick()

                backups shouldBe 1
                exports shouldBe 1
            }
        }
    }

    "restore asks for confirmation before invoking the picker" {
        onTestMain {
            runComposeUiTest {
                var restores = 0
                setContent { Section(onRestore = { restores++ }) }

                onNodeWithText("Restore from backup").performClick()
                restores shouldBe 0

                onNodeWithText("Restore").performClick()
                restores shouldBe 1
            }
        }
    }

    "cancelling the restore confirmation does not invoke the picker" {
        onTestMain {
            runComposeUiTest {
                var restores = 0
                setContent { Section(onRestore = { restores++ }) }

                onNodeWithText("Restore from backup").performClick()
                onNodeWithText("Cancel").performClick()

                restores shouldBe 0
            }
        }
    }

    "import asks for confirmation before invoking the picker" {
        onTestMain {
            runComposeUiTest {
                var imports = 0
                setContent { Section(onImport = { imports++ }) }

                onNodeWithText("Import from Ivy").performClick()
                imports shouldBe 0

                onNodeWithText("Import").performClick()
                imports shouldBe 1
            }
        }
    }

    "transfer buttons are disabled while a backup is working" {
        onTestMain {
            runComposeUiTest {
                setContent { Section(backupState = BackupState.Working) }

                onNodeWithText("Back up database").assertIsNotEnabled()
                onNodeWithText("Restore from backup").assertIsNotEnabled()
                onNodeWithText("Backing up…").assertExists()
            }
        }
    }

    "a successful backup row dismisses on click" {
        onTestMain {
            runComposeUiTest {
                var dismissals = 0
                setContent {
                    Section(
                        backupState = BackupState.Success(Unit),
                        onDismissBackup = { dismissals++ },
                    )
                }

                onNodeWithText("Backup saved.").performClick()

                dismissals shouldBe 1
            }
        }
    }
})

@Composable
private fun Section(
    backupState: BackupState<Unit> = BackupState.Idle,
    restoreState: BackupState<RestoreResult> = BackupState.Idle,
    exportState: BackupState<Unit> = BackupState.Idle,
    importState: BackupState<IvyImportResult> = BackupState.Idle,
    onBackup: () -> Unit = {},
    onExport: () -> Unit = {},
    onRestore: () -> Unit = {},
    onImport: () -> Unit = {},
    onDismissBackup: () -> Unit = {},
    onDismissRestore: () -> Unit = {},
    onDismissExport: () -> Unit = {},
    onDismissImport: () -> Unit = {},
) {
    BackupRestoreSection(
        backupState = backupState,
        restoreState = restoreState,
        exportState = exportState,
        importState = importState,
        onBackup = onBackup,
        onExport = onExport,
        onRestore = onRestore,
        onImport = onImport,
        onDismissBackup = onDismissBackup,
        onDismissRestore = onDismissRestore,
        onDismissExport = onDismissExport,
        onDismissImport = onDismissImport,
    )
}
