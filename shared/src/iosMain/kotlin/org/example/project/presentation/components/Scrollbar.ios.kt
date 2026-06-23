package org.example.project.presentation.components

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun VScrollbar(scrollState: ScrollState, modifier: Modifier, thumbColor: Color) {
    // iOS uses native scroll indicators.
}
