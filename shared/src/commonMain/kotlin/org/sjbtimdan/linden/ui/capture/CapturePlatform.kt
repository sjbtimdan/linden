package org.sjbtimdan.linden.ui.capture

import androidx.compose.runtime.Composable

/**
 * State of Linden's notification listener in system settings. [supported] is
 * false on platforms without notification access (desktop). [granted] is
 * re-checked when the host resumes, because the grant happens outside the app;
 * [openSettings] opens the system toggle to grant or revoke it; [checkNow]
 * restarts the listener so it re-scans the notifications still in the shade.
 */
data class NotificationAccess(
    val supported: Boolean,
    val granted: Boolean,
    val openSettings: () -> Unit,
    val checkNow: () -> Unit = {},
)

@Composable
expect fun rememberNotificationAccess(): NotificationAccess

/** Launcher apps offered in the capture allowlist; empty where unsupported. */
@Composable
expect fun rememberCapturableApps(): List<CapturableApp>
