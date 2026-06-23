package org.example.project.presentation.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Platform scrollbar — a clearly visible draggable bar on desktop, no-op elsewhere. */
@Composable
expect fun VScrollbar(scrollState: ScrollState, modifier: Modifier, thumbColor: Color)

/** A vertically scrolling column with a visible scrollbar on desktop. */
@Composable
fun ScrollableColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberScrollState()
    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(state).padding(contentPadding),
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
        VScrollbar(
            scrollState = state,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 4.dp, horizontal = 2.dp),
            thumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
