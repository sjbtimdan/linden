package org.sjbtimdan.linden.capture

import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType

/**
 * The learned template matching [capture], or null when none does: same
 * package and the same skeleton of the parsed description. Matching by
 * skeleton means the notification's amount, date or reference may differ; only
 * the wording must be identical, so an unrelated notification of the same app
 * can never apply another template.
 */
fun matchCaptureTemplate(capture: PendingCapture, templates: List<CaptureTemplate>): CaptureTemplate? {
    val skeleton = captureSkeleton(capture.description)
    if (!isUsableCaptureSkeleton(skeleton)) return null
    return templates.firstOrNull { it.packageName == capture.packageName && it.skeleton == skeleton }
}

/**
 * The template the saved [entry] teaches for [capture], or null when nothing
 * can be learned: transfers carry no category to remember, and a description
 * with too little wording is not a usable match key. The user's description is
 * kept — including null, which restores the notification's own text — so the
 * next prefill repeats exactly what they did once.
 */
fun captureTemplateFor(capture: PendingCapture, entry: Entry): CaptureTemplate? {
    if (entry.type == EntryType.Transfer) return null
    val category = entry.category ?: return null
    val skeleton = captureSkeleton(capture.description)
    if (!isUsableCaptureSkeleton(skeleton)) return null
    return CaptureTemplate(
        packageName = capture.packageName,
        skeleton = skeleton,
        type = entry.type,
        categoryId = category.id,
        accountId = entry.account.id,
        description = entry.description?.trim()?.ifEmpty { null },
    )
}
