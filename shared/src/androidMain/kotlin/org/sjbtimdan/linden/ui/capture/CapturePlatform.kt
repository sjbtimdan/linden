package org.sjbtimdan.linden.ui.capture

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Class name of the listener declared in the app manifest (androidApp module). */
private const val LISTENER_CLASS = "org.sjbtimdan.linden.capture.PaymentCaptureListener"

@Composable
actual fun rememberNotificationAccess(): NotificationAccess {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = isNotificationAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return NotificationAccess(
        supported = true,
        granted = granted,
        openSettings = { openNotificationAccessSettings(context) },
    )
}

private fun isNotificationAccessGranted(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

private fun openNotificationAccessSettings(context: Context) {
    val component = ComponentName(context.packageName, LISTENER_CLASS)
    try {
        context.startActivity(notificationAccessIntent(context, component))
    } catch (_: ActivityNotFoundException) {
        // Some OEM builds lack the per-listener screen; fall back to the app list.
        try {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).withNewTaskIfNeeded(context))
        } catch (_: ActivityNotFoundException) {
            // Nothing else to try; the user can still grant access manually.
        }
    }
}

private fun notificationAccessIntent(context: Context, component: ComponentName): Intent {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
    } else {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }
    return intent.withNewTaskIfNeeded(context)
}

private fun Intent.withNewTaskIfNeeded(context: Context): Intent =
    if (context is Activity) this else addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

@Composable
actual fun rememberCapturableApps(): List<CapturableApp> {
    val context = LocalContext.current
    return produceState(initialValue = emptyList(), context) {
        value = withContext(Dispatchers.IO) { loadCapturableApps(context) }
    }.value
}

/** Launcher apps (queried via the manifest's MAIN/LAUNCHER <queries>), minus Linden itself. */
private fun loadCapturableApps(context: Context): List<CapturableApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return context.packageManager.queryIntentActivities(intent, 0)
        .asSequence()
        .filter { it.activityInfo.packageName != context.packageName }
        .map { CapturableApp(it.activityInfo.packageName, it.loadLabel(context.packageManager).toString()) }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
        .toList()
}
