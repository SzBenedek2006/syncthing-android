package dev.benedek.syncthingandroid.ui.reusable

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

fun Modifier.preventClicksWhenExiting(): Modifier = composed {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                // https://developer.android.com/guide/components/activities/activity-lifecycle#onresume
                if (!lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) {
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
}

fun Modifier.topBorder(strokeWidth: Dp, color: Color) = this.drawBehind {
    val strokeWidthPx = strokeWidth.toPx()
    // Offset by half the stroke width so it draws perfectly inside the bounds
    val y = strokeWidthPx / 2f

    drawLine(
        color = color,
        start = Offset(x = 0f, y = y),
        end = Offset(x = size.width, y = y),
        strokeWidth = strokeWidthPx
    )
}

/**
 * For the ModalNavigationDrawer()
 * */
fun Modifier.topBorderWithCorners(
    strokeWidth: Dp,
    color: Color,
    cornerRadius: Dp
) = this.drawWithContent {
    drawContent() // Draw the Surface background first

    val strokePx = strokeWidth.toPx()
    val radiusPx = cornerRadius.toPx()

    val path = Path().apply {
        // 1. Start at the top-left (which is square/0.dp in your shape)
        moveTo(0f, strokePx / 2)

        // 2. Draw the straight line across the top until the curve starts
        // We stop short of the right edge by the amount of the radius
        lineTo(size.width - radiusPx, strokePx / 2)

        // 3. Draw the arc for the top-right rounded corner
        // This draws 90 degrees of a circle to follow your 16.dp radius
        arcTo(
            rect = Rect(
                left = size.width - (radiusPx * 2),
                top = strokePx / 2,
                right = size.width,
                bottom = (radiusPx * 2)
            ),
            startAngleDegrees = 270f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokePx, cap = StrokeCap.Round)
    )
}