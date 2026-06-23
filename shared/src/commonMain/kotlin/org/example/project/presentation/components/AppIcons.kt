package org.example.project.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Minimal, dependency-free line icons drawn with Canvas, tinted by the current content color. */
enum class AppIcon { DASHBOARD, CLIENTS, INVOICES, CATALOG, SETTINGS, ADD, SEARCH, BELL, SUN, MOON, TREND }

@Composable
fun AppIconView(icon: AppIcon, modifier: Modifier = Modifier.size(22.dp), tint: Color = LocalContentColor.current) {
    Canvas(modifier) { drawAppIcon(icon, tint, size.minDimension * 0.085f) }
}

private fun DrawScope.drawAppIcon(icon: AppIcon, color: Color, strokeWidth: Float) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        drawLine(color, Offset(w * x1, h * y1), Offset(w * x2, h * y2), strokeWidth, StrokeCap.Round)
    fun roundRect(x: Float, y: Float, rw: Float, rh: Float, r: Float) =
        drawRoundRect(color, Offset(w * x, h * y), Size(w * rw, h * rh), CornerRadius(w * r), style = stroke)

    when (icon) {
        AppIcon.DASHBOARD -> {
            roundRect(0.16f, 0.16f, 0.28f, 0.28f, 0.06f)
            roundRect(0.56f, 0.16f, 0.28f, 0.28f, 0.06f)
            roundRect(0.16f, 0.56f, 0.28f, 0.28f, 0.06f)
            roundRect(0.56f, 0.56f, 0.28f, 0.28f, 0.06f)
        }
        AppIcon.CLIENTS -> {
            drawCircle(color, radius = w * 0.15f, center = Offset(w * 0.5f, h * 0.34f), style = stroke)
            drawArc(
                color = color, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(w * 0.22f, h * 0.5f), size = Size(w * 0.56f, h * 0.42f), style = stroke,
            )
        }
        AppIcon.INVOICES -> {
            roundRect(0.27f, 0.14f, 0.46f, 0.72f, 0.07f)
            line(0.37f, 0.38f, 0.63f, 0.38f)
            line(0.37f, 0.52f, 0.63f, 0.52f)
            line(0.37f, 0.66f, 0.55f, 0.66f)
        }
        AppIcon.CATALOG -> {
            roundRect(0.2f, 0.28f, 0.6f, 0.5f, 0.07f)
            line(0.2f, 0.45f, 0.8f, 0.45f)
            line(0.5f, 0.28f, 0.5f, 0.45f)
        }
        AppIcon.SETTINGS -> {
            line(0.2f, 0.38f, 0.8f, 0.38f)
            line(0.2f, 0.64f, 0.8f, 0.64f)
            drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.64f, h * 0.38f), style = stroke)
            drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.36f, h * 0.64f), style = stroke)
        }
        AppIcon.ADD -> {
            line(0.5f, 0.24f, 0.5f, 0.76f)
            line(0.24f, 0.5f, 0.76f, 0.5f)
        }
        AppIcon.SEARCH -> {
            drawCircle(color, radius = w * 0.22f, center = Offset(w * 0.44f, h * 0.44f), style = stroke)
            line(0.6f, 0.6f, 0.8f, 0.8f)
        }
        AppIcon.BELL -> {
            // dome
            drawArc(
                color = color, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(w * 0.28f, h * 0.2f), size = Size(w * 0.44f, h * 0.5f), style = stroke,
            )
            line(0.28f, 0.45f, 0.28f, 0.66f)
            line(0.72f, 0.45f, 0.72f, 0.66f)
            line(0.2f, 0.66f, 0.8f, 0.66f)
            // clapper
            drawArc(
                color = color, startAngle = 0f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(w * 0.42f, h * 0.7f), size = Size(w * 0.16f, h * 0.12f), style = stroke,
            )
            // top nub
            line(0.5f, 0.14f, 0.5f, 0.2f)
        }
        AppIcon.SUN -> {
            drawCircle(color, radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
            line(0.5f, 0.1f, 0.5f, 0.22f); line(0.5f, 0.78f, 0.5f, 0.9f)
            line(0.1f, 0.5f, 0.22f, 0.5f); line(0.78f, 0.5f, 0.9f, 0.5f)
            line(0.21f, 0.21f, 0.3f, 0.3f); line(0.7f, 0.7f, 0.79f, 0.79f)
            line(0.79f, 0.21f, 0.7f, 0.3f); line(0.3f, 0.7f, 0.21f, 0.79f)
        }
        AppIcon.MOON -> {
            // crescent: outer circle minus an offset arc, approximated with two arcs
            drawArc(
                color = color, startAngle = 60f, sweepAngle = 300f, useCenter = false,
                topLeft = Offset(w * 0.24f, h * 0.18f), size = Size(w * 0.52f, h * 0.64f), style = stroke,
            )
            drawArc(
                color = color, startAngle = 250f, sweepAngle = 150f, useCenter = false,
                topLeft = Offset(w * 0.42f, h * 0.16f), size = Size(w * 0.42f, h * 0.5f), style = stroke,
            )
        }
        AppIcon.TREND -> {
            line(0.16f, 0.66f, 0.4f, 0.44f)
            line(0.4f, 0.44f, 0.56f, 0.56f)
            line(0.56f, 0.56f, 0.84f, 0.28f)
            line(0.66f, 0.28f, 0.84f, 0.28f)
            line(0.84f, 0.28f, 0.84f, 0.46f)
        }
    }
}
