package org.example.project.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.presentation.theme.Gradients

/** The SG monogram in a gradient tile, optionally followed by the wordmark. */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    showWordmark: Boolean = true,
    tileSize: Int = 40,
    onSurface: Boolean = true,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(tileSize.dp)
                .background(Gradients.brand, RoundedCornerShape((tileSize * 0.3).dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("SG", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (tileSize * 0.4).sp)
        }
        if (showWordmark) {
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "SG Media",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (onSurface) MaterialTheme.colorScheme.onSurface else Color.White,
                )
                Text(
                    "COMMUNICATION",
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}
