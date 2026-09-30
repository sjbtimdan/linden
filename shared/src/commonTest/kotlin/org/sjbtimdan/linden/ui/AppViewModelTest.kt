package org.sjbtimdan.linden.ui

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope

private class TestViewModel : AppViewModel() {
    fun write(block: suspend CoroutineScope.() -> Unit) = launchWrite(block)
}

class AppViewModelTest : StringSpec({
    "launchWrite reports the failure message" {
        onTestMain {
            val viewModel = TestViewModel()
            viewModel.write { throw IllegalStateException("disk full") }
            viewModel.error.value shouldBe "disk full"
        }
    }

    "launchWrite reports a message-less failure as an empty string" {
        onTestMain {
            val viewModel = TestViewModel()
            viewModel.write { throw IllegalStateException() }
            viewModel.error.value shouldBe ""
        }
    }

    "launchWrite rethrows cancellation instead of reporting it" {
        onTestMain {
            val viewModel = TestViewModel()
            viewModel.write { throw CancellationException("cancelled") }
            viewModel.error.value shouldBe null
        }
    }

    "launchWrite leaves the error unset when the write succeeds" {
        onTestMain {
            val viewModel = TestViewModel()
            var ran = false
            viewModel.write { ran = true }
            ran shouldBe true
            viewModel.error.value shouldBe null
        }
    }
})
