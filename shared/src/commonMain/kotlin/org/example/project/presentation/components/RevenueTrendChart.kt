package org.example.project.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.core.Money
import org.example.project.presentation.theme.AgencyPalette

/** One bucket on the trend chart: a short label (e.g. "Jan") and a monetary value. */
data class TrendPoint(val label: String, val value: Money)

/**
 * A smooth revenue area chart drawn with Canvas — no external charting dependency.
 * Plots [points] left→right with a gradient fill under the line, dots on each bucket,
 * and a faint baseline. Y is auto-scaled to the maximum value (with a little headroom).
 */
@Composable
fun RevenueTrendChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = AgencyPalette.Violet,
    height: androidx.compose.ui.unit.Dp = 180.dp,
) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val maxValue = (points.maxOfOrNull { it.value.amountMinor } ?: 0L).coerceAtLeast(1L)

    Column(modifier) {
        Box(Modifier.fillMaxWidth().height(height)) {
            Canvas(Modifier.fillMaxWidth().height(height)) {
                val w = size.width
                val h = size.height
                val padTop = 12f
                val padBottom = 10f
                val plotH = h - padTop - padBottom
                val n = points.size
                if (n == 0) return@Canvas

                // baseline + one mid gridline
                drawLine(gridColor, Offset(0f, padTop), Offset(w, padTop), 1f)
                drawLine(gridColor, Offset(0f, padTop + plotH / 2), Offset(w, padTop + plotH / 2), 1f)
                drawLine(gridColor, Offset(0f, padTop + plotH), Offset(w, padTop + plotH), 1f)

                val stepX = if (n == 1) 0f else w / (n - 1)
                fun pointAt(i: Int): Offset {
                    val x = if (n == 1) w / 2 else stepX * i
                    val frac = points[i].value.amountMinor.toFloat() / maxValue.toFloat()
                    val y = padTop + plotH - (frac * plotH * 0.92f)
                    return Offset(x, y)
                }

                val pts = List(n) { pointAt(it) }

                // area fill
                val area = Path().apply {
                    moveTo(pts.first().x, padTop + plotH)
                    pts.forEach { lineTo(it.x, it.y) }
                    lineTo(pts.last().x, padTop + plotH)
                    close()
                }
                drawPath(
                    area,
                    Brush.verticalGradient(
                        listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f)),
                        startY = padTop, endY = padTop + plotH,
                    ),
                )

                // line
                val line = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(line, lineColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

                // dots
                pts.forEach { p ->
                    drawCircle(Color.White, radius = 4.5f, center = p)
                    drawCircle(lineColor, radius = 4.5f, center = p, style = Stroke(width = 2f))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            points.forEach { p ->
                Text(p.label, color = labelColor, fontSize = 11.sp)
            }
        }
    }
}
