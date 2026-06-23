package org.example.project.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Circular "SG" monogram — a stand-in for the official SG Media logo.
 * Replace by dropping the real asset at composeResources/drawable/sg_logo.png.
 */
@Composable
fun SgLogo(sizeDp: Int = 64, color: Color = Color(0xFF12132A), modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = size.minDimension * 0.06f
            drawArc(
                color = color,
                startAngle = 28f,
                sweepAngle = 304f,
                useCenter = false,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
        }
        Text(
            "SG",
            color = color,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            fontSize = (sizeDp * 0.42).sp,
        )
    }
}
