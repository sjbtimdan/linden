package org.sjbtimdan.linden.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.input.ImeAction
import io.kotest.core.spec.style.StringSpec

@OptIn(ExperimentalTestApi::class)
class SearchFieldTest : StringSpec({
    "the keyboard offers a search action" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    SearchField(value = "", onValueChange = {})
                }

                onNode(hasSetTextAction()).assert(hasImeAction(ImeAction.Search))
                onNode(hasSetTextAction()).performImeAction()
            }
        }
    }
})
