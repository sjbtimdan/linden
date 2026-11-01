package org.sjbtimdan.linden.ui.capture

/** One launcher app the user can allow payment capture for. */
data class CapturableApp(val packageName: String, val label: String)

/**
 * A pickable launcher app, or null when it cannot be listed: Linden itself, a
 * missing package name, or a label the platform cannot resolve.
 */
internal fun capturableApp(packageName: String?, rawLabel: CharSequence?, ownPackage: String): CapturableApp? {
    if (packageName == null || packageName == ownPackage) return null
    val label = rawLabel?.toString()?.takeIf { it.isNotBlank() } ?: return null
    return CapturableApp(packageName, label)
}
