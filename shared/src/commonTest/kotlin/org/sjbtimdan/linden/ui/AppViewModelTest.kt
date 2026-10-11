package org.sjbtimdan.linden.ui

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope

private class TestViewModel : AppViewModel() {
    fun write(block: suspend CoroutineScope.() -> Unit) = launchWrite(block)
}

class AppViewModelTest : StringSpec({
    "launchWrite signals a failure without carrying the exception message" {
        onTestMain {
            val viewModel = TestViewModel()
            viewModel.write { throw IllegalStateException("disk full") }
            viewModel.error.value shouldBe true
        }
    }

    "launchWrite signals a message-less failure the same way" {
        onTestMain {
            val viewModel = TestViewModel()
            viewModel.write { throw IllegalStateException() }
            viewModel.error.value shouldBe true
        }
    }

    "clearError consumes the signal so a later failure can be shown again" {
        onTestMain {
            val viewModel = TestViewModel()
            viewModel.write { throw IllegalStateException() }
            viewModel.error.value shouldBe true

            viewModel.clearError()

            viewModel.error.value shouldBe false
        }
    }

    "launchWrite rethrows cancellation instead of reporting it" {
        onTestMain {
            val viewModel = TestViewModel()
            viewModel.write { throw CancellationException("cancelled") }
            viewModel.error.value shouldBe false
        }
    }

    "launchWrite leaves the error unset when the write succeeds" {
        onTestMain {
            val viewModel = TestViewModel()
            var ran = false
            viewModel.write { ran = true }
            ran shouldBe true
            viewModel.error.value shouldBe false
        }
    }
})
