package org.sjbtimdan.linden

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.sjbtimdan.linden.capture.PaymentCaptureListener
import org.sjbtimdan.linden.data.DatabaseDriverFactory
import org.sjbtimdan.linden.log.appLogger

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppRoot(logger = appLogger) {
                val context = applicationContext
                withContext(Dispatchers.IO) {
                    createAppDependencies(DatabaseDriverFactory(context).createDriver(), appLogger)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Android can stop delivering notification callbacks while the app is away.
        PaymentCaptureListener.recover(applicationContext)
    }
}
