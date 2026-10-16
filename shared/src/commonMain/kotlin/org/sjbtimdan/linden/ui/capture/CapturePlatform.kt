package org.sjbtimdan.linden.ui.capture

import androidx.compose.runtime.Composable

/** One launcher app the user can allow payment capture for. */
data class CapturableApp(val packageName: String, val label: String)

/**
 * State of Linden's notification listener in system settings. [supported] is
 * false on platforms without notification access (desktop). [granted] is
 * re-checked when the host resumes, because the grant happens outside the app;
 * [openSettings] opens the system toggle to grant or revoke it.
 */
data class NotificationAccess(
    val supported: Boolean,
    val granted: Boolean,
    val openSettings: () -> Unit,
)

@Composable
expect fun rememberNotificationAccess(): NotificationAccess

/** Launcher apps offered in the capture allowlist; empty where unsupported. */
@Composable
expect fun rememberCapturableApps(): List<CapturableApp>
