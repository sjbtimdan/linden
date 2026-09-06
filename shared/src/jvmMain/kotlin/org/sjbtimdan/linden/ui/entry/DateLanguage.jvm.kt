package org.sjbtimdan.linden.ui.entry

import java.util.Locale

internal actual fun platformLanguageCode(): String = Locale.getDefault().language
