package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class MissingRequirementTest : StringSpec({

    // The English copy each requirement resolves to. Locks the enum → resource
    // mapping: a renamed or rewired key fails here instead of showing the wrong
    // hint in the UI. The default (English) resource file must keep matching.
    val expectedCopy = mapOf(
        MissingRequirement.AMOUNT to "Enter an amount",
        MissingRequirement.ACCOUNT to "Choose an account",
        MissingRequirement.SOURCE_ACCOUNT to "Choose where the money comes from",
        MissingRequirement.CATEGORY to "Choose a category",
        MissingRequirement.DESTINATION_ACCOUNT to "Choose where the money goes",
        MissingRequirement.DIFFERENT_DESTINATION to "Choose a different destination account",
        MissingRequirement.RECEIVED_AMOUNT to "Enter the received amount",
    )

    "every requirement resolves to its copy" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    Column {
                        MissingRequirement.entries.forEach { requirement ->
                            Text(requirement.text())
                        }
                    }
                }
                expectedCopy.forEach { (requirement, copy) ->
                    onNodeWithText(copy)
                        .assertExists("MissingRequirement.$requirement must resolve to its copy")
                }
            }
        }
    }
})
