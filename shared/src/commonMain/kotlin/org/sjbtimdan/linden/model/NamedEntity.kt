package org.sjbtimdan.linden.model

/** An entity with a stable id and a display name that is unique per type. */
interface NamedEntity {
    val id: Long
    val name: String
}

/**
 * The trimmed [name], or null when it is blank or already used by a different
 * entity (case-insensitive). [excludingId] lets an update keep its own name.
 */
fun <T : NamedEntity> uniqueName(items: List<T>, name: String, excludingId: Long? = null): String? {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return null
    if (items.any { it.id != excludingId && it.name.equals(trimmed, ignoreCase = true) }) return null
    return trimmed
}
