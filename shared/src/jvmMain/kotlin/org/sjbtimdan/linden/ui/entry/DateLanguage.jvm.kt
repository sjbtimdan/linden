package org.sjbtimdan.linden.ui.entry

import java.util.Locale

internal actual fun platformLocaleTag(): String = Locale.getDefault().toLanguageTag()
