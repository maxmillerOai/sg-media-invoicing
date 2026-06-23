package org.example.project.presentation.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
actual fun VScrollbar(scrollState: ScrollState, modifier: Modifier, thumbColor: Color) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState),
        modifier = modifier,
        style = ScrollbarStyle(
            minimalHeight = 28.dp,
            thickness = 10.dp,
            shape = RoundedCornerShape(5.dp),
            hoverDurationMillis = 200,
            unhoverColor = thumbColor.copy(alpha = 0.40f),
            hoverColor = thumbColor.copy(alpha = 0.75f),
        ),
    )
}
