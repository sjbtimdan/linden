package org.sjbtimdan.linden.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberTapHaptic(): () -> Unit = remember { {} }
