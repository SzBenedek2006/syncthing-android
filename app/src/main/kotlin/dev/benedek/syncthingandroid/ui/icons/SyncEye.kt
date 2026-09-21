@file:Suppress("ObjectPropertyName", "BooleanLiteralArgument")

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

val SyncEye: ImageVector
    get() {
        if (_syncEye != null) {
            return _syncEye!!
        }
        _syncEye = Builder(
            name = "SyncEye",
            defaultWidth = 24.0.dp,
            defaultHeight =
                24.0.dp,
            viewportWidth = 960.0f,
            viewportHeight = 960.0f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(628.29f, 692.68f)
                quadTo(614.63f, 679.02f, 614.63f, 659.51f)
                quadToRelative(0.0f, -19.51f, 13.66f, -33.17f)
                quadToRelative(13.66f, -13.66f, 33.17f, -13.66f)
                quadToRelative(19.51f, 0.0f, 33.17f, 13.66f)
                quadToRelative(13.66f, 13.66f, 13.66f, 33.17f)
                quadToRelative(0.0f, 19.51f, -13.66f, 33.17f)
                quadToRelative(-13.66f, 13.66f, -33.17f, 13.66f)
                quadToRelative(-19.51f, 0.0f, -33.17f, -13.66f)
                close()
                moveTo(661.46f, 519.02f)
                curveToRelative(-47.35f, 0.0f, -91.03f, 12.46f, -131.1f, 37.44f)
                curveTo(490.3f, 581.44f, 461.14f, 615.8f, 442.92f, 659.51f)
                curveToRelative(18.21f, 43.71f, 47.37f, 78.07f, 87.44f, 103.05f)
                curveTo(570.43f, 787.54f, 614.11f, 800.0f, 661.46f, 800.0f)
                curveTo(708.81f, 800.0f, 752.5f, 787.54f, 792.56f, 762.56f)
                curveTo(832.63f, 737.59f, 861.79f, 703.22f, 880.0f, 659.51f)
                curveTo(861.79f, 615.8f, 832.63f, 581.44f, 792.56f, 556.46f)
                curveTo(752.5f, 531.49f, 708.81f, 519.02f, 661.46f, 519.02f)
                close()
                moveTo(661.46f, 581.46f)
                curveToRelative(29.66f, 0.0f, 57.63f, 6.78f, 83.9f, 20.3f)
                curveTo(771.64f, 615.3f, 793.11f, 634.54f, 809.76f, 659.51f)
                curveToRelative(-16.65f, 24.98f, -38.11f, 44.22f, -64.39f, 57.74f)
                curveTo(719.09f, 730.78f, 691.12f, 737.56f, 661.46f, 737.56f)
                curveToRelative(-29.66f, 0.0f, -57.63f, -6.78f, -83.9f, -20.3f)
                curveTo(551.28f, 703.73f, 529.82f, 684.49f, 513.17f, 659.51f)
                curveToRelative(16.65f, -24.98f, 38.11f, -44.22f, 64.39f, -57.74f)
                curveTo(603.84f, 588.24f, 631.8f, 581.46f, 661.46f, 581.46f)
                close()
                moveTo(160.0f, 800.0f)
                lineTo(160.0f, 720.0f)
                lineTo(269.0f, 720.0f)
                quadTo(218.0f, 676.0f, 189.0f, 614.0f)
                quadTo(160.0f, 552.0f, 160.0f, 480.0f)
                quadToRelative(0.0f, -112.0f, 68.0f, -197.5f)
                quadToRelative(68.0f, -85.5f, 172.0f, -112.5f)
                lineToRelative(0.0f, 84.0f)
                quadToRelative(-70.0f, 25.0f, -115.0f, 86.5f)
                quadToRelative(-45.0f, 61.5f, -45.0f, 139.5f)
                quadToRelative(0.0f, 54.0f, 21.5f, 99.5f)
                quadToRelative(21.5f, 45.5f, 58.5f, 78.5f)
                lineTo(320.0f, 560.0f)
                lineTo(400.0f, 560.0f)
                lineTo(400.0f, 800.0f)
                close()
                moveTo(716.0f, 440.0f)
                quadToRelative(-7.0f, -41.0f, -27.0f, -76.0f)
                quadToRelative(-20.0f, -35.0f, -49.0f, -62.0f)
                lineToRelative(0.0f, 98.0f)
                lineTo(560.0f, 400.0f)
                lineToRelative(0.0f, -240.0f)
                lineToRelative(240.0f, 0.0f)
                lineToRelative(0.0f, 80.0f)
                lineTo(691.0f, 240.0f)
                quadToRelative(43.0f, 38.0f, 70.5f, 89.0f)
                quadToRelative(27.5f, 51.0f, 35.5f, 111.0f)
                close()
            }
        }.build()
        return _syncEye!!
    }

private var _syncEye: ImageVector? = null

@Preview
@Composable
private fun Preview() {
    Box(Modifier.padding(12.dp)) {
        Image(SyncEye, null)
    }
}
