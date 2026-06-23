package org.example.project.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlinx.datetime.LocalDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A stylish, accurate analog clock drawn with Canvas. Hands are derived from [time]
 * (which the caller updates once per second), with the minute/hour hands advancing
 * smoothly between ticks.
 */
@Composable
fun AnalogClock(
    time: LocalDateTime,
    modifier: Modifier = Modifier,
    dialColor: Color = Color.White,
    handColor: Color = Color.White,
    accent: Color = Color(0xFF00C2FF),
) {
    Canvas(modifier) {
        val r = size.minDimension / 2f * 0.96f
        val c = Offset(size.width / 2f, size.height / 2f)

        // Face: subtle fill + ring.
        drawCircle(color = dialColor.copy(alpha = 0.06f), radius = r, center = c)
        drawCircle(color = dialColor.copy(alpha = 0.85f), radius = r, center = c, style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.045f))

        // Tick marks (60), every 5th longer/bolder.
        for (i in 0 until 60) {
            val ang = i * 6f * (PI.toFloat() / 180f)
            val major = i % 5 == 0
            val inner = if (major) r * 0.78f else r * 0.86f
            val outer = r * 0.92f
            val sinA = sin(ang); val cosA = cos(ang)
            drawLine(
                color = dialColor.copy(alpha = if (major) 0.9f else 0.4f),
                start = Offset(c.x + sinA * inner, c.y - cosA * inner),
                end = Offset(c.x + sinA * outer, c.y - cosA * outer),
                strokeWidth = if (major) r * 0.035f else r * 0.02f,
                cap = StrokeCap.Round,
            )
        }

        fun hand(degrees: Float, length: Float, width: Float, color: Color, tail: Float = 0.12f) {
            val a = degrees * (PI.toFloat() / 180f)
            val sinA = sin(a); val cosA = cos(a)
            drawLine(
                color = color,
                start = Offset(c.x - sinA * r * tail, c.y + cosA * r * tail),
                end = Offset(c.x + sinA * length, c.y - cosA * length),
                strokeWidth = width,
                cap = StrokeCap.Round,
            )
        }

        val sec = time.second
        val min = time.minute + sec / 60f
        val hour = (time.hour % 12) + min / 60f

        hand(hour * 30f, r * 0.50f, r * 0.065f, handColor)        // hour
        hand(min * 6f, r * 0.74f, r * 0.045f, handColor)          // minute
        hand(sec * 6f, r * 0.84f, r * 0.022f, accent)             // second

        // Center cap.
        drawCircle(color = handColor, radius = r * 0.06f, center = c)
        drawCircle(color = accent, radius = r * 0.032f, center = c)
    }
}
