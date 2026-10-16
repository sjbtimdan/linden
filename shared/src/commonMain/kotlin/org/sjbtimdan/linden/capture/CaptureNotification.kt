package org.sjbtimdan.linden.capture

/**
 * Text content extracted from one posted notification by the platform layer.
 * Android types stay outside so the parser remains pure and common; a null or
 * blank field simply means the notification did not carry it.
 */
data class CaptureNotification(
    val title: String? = null,
    val text: String? = null,
    val bigText: String? = null,
    val subText: String? = null,
)
