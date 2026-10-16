package org.sjbtimdan.linden.ui.capture

import androidx.compose.runtime.Composable

/** Notification capture is Android-only: desktop reports it as unsupported. */
@Composable
actual fun rememberNotificationAccess(): NotificationAccess =
    NotificationAccess(supported = false, granted = false, openSettings = {})

@Composable
actual fun rememberCapturableApps(): List<CapturableApp> = emptyList()
