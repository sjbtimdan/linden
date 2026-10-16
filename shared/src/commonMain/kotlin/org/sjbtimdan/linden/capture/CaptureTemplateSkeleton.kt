package org.sjbtimdan.linden.capture

private const val MAX_SKELETON_LENGTH = 140

/** A skeleton needs at least this many letters to identify a template. */
private const val MIN_SKELETON_LETTERS = 4

/**
 * The match key of a notification text: every run of digits collapses to a
 * single '#', whitespace collapses to single spaces, and the result is
 * lowercased and capped. Two notifications of the same template differ only in
 * their numbers — amount, date, reference, card tail — so they share one
 * skeleton, while different wording or merchants never do.
 */
fun captureSkeleton(text: String): String {
    val skeleton = StringBuilder()
    var lastWasDigit = false
    var lastWasWhitespace = false
    for (char in text) {
        when {
            char.isDigit() -> {
                if (!lastWasDigit) skeleton.append('#')
                lastWasDigit = true
                lastWasWhitespace = false
            }

            char.isWhitespace() -> {
                if (!lastWasWhitespace) skeleton.append(' ')
                lastWasWhitespace = true
                lastWasDigit = false
            }

            else -> {
                skeleton.append(char.lowercaseChar())
                lastWasDigit = false
                lastWasWhitespace = false
            }
        }
    }
    return skeleton.toString().trim().take(MAX_SKELETON_LENGTH).trimEnd()
}

/**
 * A skeleton with too little wording cannot identify a template: templates are
 * neither learned nor matched from it, so a notification whose text masks down
 * to almost nothing can never collide with an unrelated one.
 */
internal fun isUsableCaptureSkeleton(skeleton: String): Boolean =
    skeleton.count { it.isLetter() } >= MIN_SKELETON_LETTERS
