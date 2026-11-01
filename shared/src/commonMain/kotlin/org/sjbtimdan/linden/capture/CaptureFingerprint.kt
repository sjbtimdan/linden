package org.sjbtimdan.linden.capture

/**
 * Identity of one posted notification version: the platform key plus a hash of
 * the text Linden reads. A re-delivery or an update that leaves the text
 * unchanged keeps the fingerprint; a new payment reusing the same key does not.
 */
fun captureFingerprint(notificationKey: String, content: CaptureNotification): String =
    "$notificationKey|${content.hashCode()}"
