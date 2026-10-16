package org.sjbtimdan.linden.ui.capture

import androidx.compose.ui.test.ExperimentalTestApi
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.capture.CaptureRule
import org.sjbtimdan.linden.ui.withCaptureSettingsViewModel

@OptIn(ExperimentalTestApi::class)
class CaptureSettingsViewModelTest : StringSpec({

    "rules start empty and follow updates" {
        withCaptureSettingsViewModel { repository, _, viewModel ->
            viewModel.rules.value.shouldBeEmpty()

            viewModel.setRule("com.example.bank", accountId = 5L)

            viewModel.rules.first { it.isNotEmpty() } shouldBe listOf(CaptureRule("com.example.bank", 5L))
            repository.rules() shouldBe listOf(CaptureRule("com.example.bank", 5L))
        }
    }

    "setRuleEnabled and removeRule update only the matching rule" {
        withCaptureSettingsViewModel { _, _, viewModel ->
            viewModel.setRule("app.a", 1L)
            viewModel.setRule("app.b", 2L)

            viewModel.setRuleEnabled("app.a", false)
            viewModel.rules.first { it.any { rule -> !rule.enabled } } shouldBe
                listOf(CaptureRule("app.a", 1L, enabled = false), CaptureRule("app.b", 2L))

            viewModel.removeRule("app.b")

            viewModel.rules.first { it.size == 1 } shouldBe listOf(CaptureRule("app.a", 1L, enabled = false))
        }
    }
})
