package org.sjbtimdan.linden.ui.ledger

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class FutureEntriesNoticeTest : StringSpec({

    "entries label counts one upcoming entry" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    Text(futureEntriesNoticeLabel(LedgerViewMode.Entries, upcoming = 1, bounded = true))
                }
                onNodeWithText("Showing 1 entry after today").assertExists()
            }
        }
    }

    "entries label counts several upcoming entries" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    Text(futureEntriesNoticeLabel(LedgerViewMode.Entries, upcoming = 3, bounded = true))
                }
                onNodeWithText("Showing 3 entries after today").assertExists()
            }
        }
    }

    "entries label counts every future entry without a window" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    Text(futureEntriesNoticeLabel(LedgerViewMode.Entries, upcoming = 2, bounded = false))
                }
                onNodeWithText("Showing 2 entries after today").assertExists()
            }
        }
    }

    "accounts label names balances" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    Text(futureEntriesNoticeLabel(LedgerViewMode.Accounts, upcoming = 2, bounded = true))
                }
                onNodeWithText("Balances include entries after today").assertExists()
            }
        }
    }

    "categories label names totals" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    Text(futureEntriesNoticeLabel(LedgerViewMode.Categories, upcoming = 2, bounded = true))
                }
                onNodeWithText("Totals include entries after today").assertExists()
            }
        }
    }

    "no upcoming entries names the period when there is one" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    Text(futureEntriesNoticeLabel(LedgerViewMode.Entries, upcoming = 0, bounded = true))
                }
                onNodeWithText("No entries after today in this period").assertExists()
            }
        }
    }

    "no upcoming entries stays short without a window" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    Text(futureEntriesNoticeLabel(LedgerViewMode.Accounts, upcoming = 0, bounded = false))
                }
                onNodeWithText("No entries after today").assertExists()
            }
        }
    }
})
