package org.sjbtimdan.linden.capture

import kotlinx.serialization.Serializable

/**
 * A user-approved app whose notifications may be parsed, mapped to the Linden
 * account its amounts belong to. Stored as JSON in the settings table, so no
 * schema change is needed; [enabled] lets a rule be paused without losing the
 * account mapping.
 */
@Serializable
data class CaptureRule(
    val packageName: String,
    val accountId: Long,
    val enabled: Boolean = true,
)
