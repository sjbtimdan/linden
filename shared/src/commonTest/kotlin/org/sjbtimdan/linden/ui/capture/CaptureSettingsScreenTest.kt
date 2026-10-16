package org.sjbtimdan.linden.ui.capture

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.capture.CaptureRule
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.withCaptureSettingsViewModel

private fun access(granted: Boolean, onOpen: () -> Unit = {}): NotificationAccess =
    NotificationAccess(supported = true, granted = granted, openSettings = onOpen)

@OptIn(ExperimentalTestApi::class)
class CaptureSettingsScreenTest : StringSpec({

    "unsupported platforms only explain availability" {
        withCaptureSettingsViewModel { _, _, viewModel ->
            setContent {
                CaptureSettingsScreen(
                    viewModel = viewModel,
                    access = NotificationAccess(supported = false, granted = false, openSettings = {}),
                    capturableApps = emptyList(),
                    onNavigateBack = {},
                )
            }

            onNodeWithText("Notification capture is available in the Android app.").assertExists()
        }
    }

    "enabling goes through the disclosure before opening system settings" {
        var opened = false
        withCaptureSettingsViewModel { _, _, viewModel ->
            setContent {
                CaptureSettingsScreen(
                    viewModel = viewModel,
                    access = access(granted = false, onOpen = { opened = true }),
                    capturableApps = emptyList(),
                    onNavigateBack = {},
                )
            }

            onNodeWithText("Allow notification access").performClick()
            onNodeWithText("Allow notification access?").assertExists()
            onNodeWithText("Continue").performClick()
            opened shouldBe true
        }
    }

    "the app picker adds a rule mapped to the first account" {
        withCaptureSettingsViewModel { repository, accountDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            val main = accountDao.getAll().first().first()
            setContent {
                CaptureSettingsScreen(
                    viewModel = viewModel,
                    access = access(granted = true),
                    capturableApps = listOf(CapturableApp("com.example.bank", "Example Bank")),
                    onNavigateBack = {},
                )
            }

            onNodeWithText("Add app").performClick()
            onNodeWithText("Example Bank").performClick()

            repository.rules() shouldBe listOf(CaptureRule("com.example.bank", main.id))
        }
    }

    "the switch pauses a rule and remove deletes it" {
        withCaptureSettingsViewModel { repository, _, viewModel ->
            repository.setRule(CaptureRule("com.example.bank", accountId = 1L))
            setContent {
                CaptureSettingsScreen(
                    viewModel = viewModel,
                    access = access(granted = true),
                    capturableApps = listOf(CapturableApp("com.example.bank", "Example Bank")),
                    onNavigateBack = {},
                )
            }

            onNode(isToggleable()).performClick()
            repository.rules().first().enabled shouldBe false

            onNodeWithContentDescription("Remove").performClick()
            repository.rules().shouldBeEmpty()
        }
    }
})
