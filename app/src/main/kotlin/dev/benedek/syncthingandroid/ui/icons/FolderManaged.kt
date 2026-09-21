@file:Suppress("ObjectPropertyName")

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

public val FolderManaged: ImageVector
    get() {
        if (_folderManaged != null) {
            return _folderManaged!!
        }
        _folderManaged = Builder(name = "FolderManaged", defaultWidth = 24.0.dp, defaultHeight =
                24.0.dp, viewportWidth = 960.0f, viewportHeight = 960.0f).apply {
            path(fill = SolidColor(Color(0xFF000000)), stroke = null, strokeLineWidth = 0.0f,
                    strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                    pathFillType = NonZero) {
                moveTo(680.0f, 880.0f)
                lineTo(668.0f, 820.0f)
                quadTo(656.0f, 815.0f, 645.5f, 809.5f)
                quadTo(635.0f, 804.0f, 624.0f, 796.0f)
                lineTo(566.0f, 814.0f)
                lineTo(526.0f, 746.0f)
                lineTo(572.0f, 706.0f)
                quadTo(570.0f, 694.0f, 570.0f, 680.0f)
                quadTo(570.0f, 666.0f, 572.0f, 654.0f)
                lineTo(526.0f, 614.0f)
                lineTo(566.0f, 546.0f)
                lineTo(624.0f, 564.0f)
                quadTo(635.0f, 556.0f, 645.5f, 550.5f)
                quadTo(656.0f, 545.0f, 668.0f, 540.0f)
                lineTo(680.0f, 480.0f)
                lineTo(760.0f, 480.0f)
                lineTo(772.0f, 540.0f)
                quadTo(784.0f, 545.0f, 794.5f, 550.5f)
                quadTo(805.0f, 556.0f, 816.0f, 564.0f)
                lineTo(874.0f, 546.0f)
                lineTo(914.0f, 614.0f)
                lineTo(868.0f, 654.0f)
                quadTo(870.0f, 666.0f, 870.0f, 680.0f)
                quadTo(870.0f, 694.0f, 868.0f, 706.0f)
                lineTo(914.0f, 746.0f)
                lineTo(874.0f, 814.0f)
                lineTo(816.0f, 796.0f)
                quadTo(805.0f, 804.0f, 794.5f, 809.5f)
                quadTo(784.0f, 815.0f, 772.0f, 820.0f)
                lineTo(760.0f, 880.0f)
                lineTo(680.0f, 880.0f)
                close()
                moveTo(776.5f, 736.5f)
                quadTo(800.0f, 713.0f, 800.0f, 680.0f)
                quadTo(800.0f, 647.0f, 776.5f, 623.5f)
                quadTo(753.0f, 600.0f, 720.0f, 600.0f)
                quadTo(687.0f, 600.0f, 663.5f, 623.5f)
                quadTo(640.0f, 647.0f, 640.0f, 680.0f)
                quadTo(640.0f, 713.0f, 663.5f, 736.5f)
                quadTo(687.0f, 760.0f, 720.0f, 760.0f)
                quadTo(753.0f, 760.0f, 776.5f, 736.5f)
                close()
                moveTo(160.0f, 720.0f)
                lineTo(160.0f, 720.0f)
                quadTo(160.0f, 720.0f, 160.0f, 720.0f)
                quadTo(160.0f, 720.0f, 160.0f, 720.0f)
                lineTo(160.0f, 240.0f)
                quadTo(160.0f, 240.0f, 160.0f, 240.0f)
                quadTo(160.0f, 240.0f, 160.0f, 240.0f)
                lineTo(160.0f, 240.0f)
                lineTo(160.0f, 320.0f)
                lineTo(160.0f, 320.0f)
                quadTo(160.0f, 320.0f, 160.0f, 320.0f)
                quadTo(160.0f, 320.0f, 160.0f, 320.0f)
                lineTo(160.0f, 412.0f)
                quadTo(160.0f, 406.0f, 160.0f, 403.0f)
                quadTo(160.0f, 400.0f, 160.0f, 400.0f)
                quadTo(160.0f, 400.0f, 160.0f, 482.5f)
                quadTo(160.0f, 565.0f, 160.0f, 679.0f)
                quadTo(160.0f, 690.0f, 160.0f, 699.5f)
                quadTo(160.0f, 709.0f, 160.0f, 720.0f)
                close()
                moveTo(160.0f, 800.0f)
                quadTo(127.0f, 800.0f, 103.5f, 776.5f)
                quadTo(80.0f, 753.0f, 80.0f, 720.0f)
                lineTo(80.0f, 240.0f)
                quadTo(80.0f, 207.0f, 103.5f, 183.5f)
                quadTo(127.0f, 160.0f, 160.0f, 160.0f)
                lineTo(400.0f, 160.0f)
                lineTo(480.0f, 240.0f)
                lineTo(800.0f, 240.0f)
                quadTo(833.0f, 240.0f, 856.5f, 263.5f)
                quadTo(880.0f, 287.0f, 880.0f, 320.0f)
                lineTo(880.0f, 451.0f)
                quadTo(862.0f, 438.0f, 842.0f, 428.5f)
                quadTo(822.0f, 419.0f, 800.0f, 412.0f)
                lineTo(800.0f, 320.0f)
                quadTo(800.0f, 320.0f, 800.0f, 320.0f)
                quadTo(800.0f, 320.0f, 800.0f, 320.0f)
                lineTo(447.0f, 320.0f)
                lineTo(367.0f, 240.0f)
                lineTo(160.0f, 240.0f)
                quadTo(160.0f, 240.0f, 160.0f, 240.0f)
                quadTo(160.0f, 240.0f, 160.0f, 240.0f)
                lineTo(160.0f, 720.0f)
                quadTo(160.0f, 720.0f, 160.0f, 720.0f)
                quadTo(160.0f, 720.0f, 160.0f, 720.0f)
                lineTo(443.0f, 720.0f)
                quadTo(446.0f, 741.0f, 452.5f, 761.0f)
                quadTo(459.0f, 781.0f, 468.0f, 800.0f)
                lineTo(160.0f, 800.0f)
                close()
            }
        }
        .build()
        return _folderManaged!!
    }

private var _folderManaged: ImageVector? = null

@Preview
@Composable
private fun Preview() {
    Box(Modifier.padding(12.dp)) {
        Image(FolderManaged, null)
    }
}
