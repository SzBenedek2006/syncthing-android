package dev.benedek.syncthingandroid.ui.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

public val Settings: ImageVector
    get() {
        if (_settings != null) {
            return _settings!!
        }
        _settings = Builder(name = "Settings", defaultWidth = 24.0.dp, defaultHeight = 24.0.dp,
                viewportWidth = 24.0f, viewportHeight = 24.0f).apply {
            path(fill = SolidColor(Color(0xFF000000)), stroke = null, strokeLineWidth = 0.0f,
                    strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                    pathFillType = NonZero) {
                moveTo(19.43f, 12.98f)
                curveToRelative(0.04f, -0.32f, 0.07f, -0.64f, 0.07f, -0.98f)
                reflectiveCurveToRelative(-0.03f, -0.66f, -0.07f, -0.98f)
                lineToRelative(2.11f, -1.65f)
                curveToRelative(0.19f, -0.15f, 0.24f, -0.42f, 0.12f, -0.64f)
                lineToRelative(-2.0f, -3.46f)
                curveToRelative(-0.12f, -0.22f, -0.39f, -0.3f, -0.61f, -0.22f)
                lineToRelative(-2.49f, 1.0f)
                curveToRelative(-0.52f, -0.4f, -1.08f, -0.73f, -1.69f, -0.98f)
                lineToRelative(-0.38f, -2.65f)
                curveTo(14.46f, 2.18f, 14.25f, 2.0f, 14.0f, 2.0f)
                horizontalLineToRelative(-4.0f)
                curveToRelative(-0.25f, 0.0f, -0.46f, 0.18f, -0.49f, 0.42f)
                lineToRelative(-0.38f, 2.65f)
                curveToRelative(-0.61f, 0.25f, -1.17f, 0.59f, -1.69f, 0.98f)
                lineToRelative(-2.49f, -1.0f)
                curveToRelative(-0.23f, -0.09f, -0.49f, 0.0f, -0.61f, 0.22f)
                lineToRelative(-2.0f, 3.46f)
                curveToRelative(-0.13f, 0.22f, -0.07f, 0.49f, 0.12f, 0.64f)
                lineToRelative(2.11f, 1.65f)
                curveToRelative(-0.04f, 0.32f, -0.07f, 0.65f, -0.07f, 0.98f)
                reflectiveCurveToRelative(0.03f, 0.66f, 0.07f, 0.98f)
                lineToRelative(-2.11f, 1.65f)
                curveToRelative(-0.19f, 0.15f, -0.24f, 0.42f, -0.12f, 0.64f)
                lineToRelative(2.0f, 3.46f)
                curveToRelative(0.12f, 0.22f, 0.39f, 0.3f, 0.61f, 0.22f)
                lineToRelative(2.49f, -1.0f)
                curveToRelative(0.52f, 0.4f, 1.08f, 0.73f, 1.69f, 0.98f)
                lineToRelative(0.38f, 2.65f)
                curveToRelative(0.03f, 0.24f, 0.24f, 0.42f, 0.49f, 0.42f)
                horizontalLineToRelative(4.0f)
                curveToRelative(0.25f, 0.0f, 0.46f, -0.18f, 0.49f, -0.42f)
                lineToRelative(0.38f, -2.65f)
                curveToRelative(0.61f, -0.25f, 1.17f, -0.59f, 1.69f, -0.98f)
                lineToRelative(2.49f, 1.0f)
                curveToRelative(0.23f, 0.09f, 0.49f, 0.0f, 0.61f, -0.22f)
                lineToRelative(2.0f, -3.46f)
                curveToRelative(0.12f, -0.22f, 0.07f, -0.49f, -0.12f, -0.64f)
                lineToRelative(-2.11f, -1.65f)
                close()
                moveTo(12.0f, 15.5f)
                curveToRelative(-1.93f, 0.0f, -3.5f, -1.57f, -3.5f, -3.5f)
                reflectiveCurveToRelative(1.57f, -3.5f, 3.5f, -3.5f)
                reflectiveCurveToRelative(3.5f, 1.57f, 3.5f, 3.5f)
                reflectiveCurveToRelative(-1.57f, 3.5f, -3.5f, 3.5f)
                close()
            }
        }
        .build()
        return _settings!!
    }

private var _settings: ImageVector? = null

@Preview
@Composable
private fun Preview() {
    Box(Modifier.padding(12.dp)) {
        Image(Settings, null)
    }
}
