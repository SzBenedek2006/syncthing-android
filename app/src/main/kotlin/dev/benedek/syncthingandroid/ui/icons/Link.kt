@file:Suppress("ObjectPropertyName")

package dev.benedek.syncthingandroid.ui.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val Link: ImageVector
    get() {
        if (_link != null) {
            return _link!!
        }
        _link =
            ImageVector.Builder(
                name = "link",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1f,
                        stroke = null,
                        strokeAlpha = 1f,
                        strokeLineWidth = 1f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Bevel,
                        strokeLineMiter = 1f,
                        pathFillType = PathFillType.Companion.NonZero,
                    ) {
                        moveTo(11f, 17f)
                        horizontalLineTo(7f)
                        quadTo(4.93f, 17f, 3.46f, 15.54f)
                        reflectiveQuadTo(2f, 12f)
                        quadTo(2f, 9.92f, 3.46f, 8.46f)
                        reflectiveQuadTo(7f, 7f)
                        horizontalLineToRelative(4f)
                        verticalLineTo(9f)
                        horizontalLineTo(7f)
                        quadTo(5.75f, 9f, 4.88f, 9.88f)
                        reflectiveQuadTo(4f, 12f)
                        reflectiveQuadToRelative(0.88f, 2.13f)
                        reflectiveQuadTo(7f, 15f)
                        horizontalLineToRelative(4f)
                        verticalLineToRelative(2f)
                        close()
                        moveTo(8f, 13f)
                        verticalLineTo(11f)
                        horizontalLineToRelative(8f)
                        verticalLineToRelative(2f)
                        horizontalLineTo(8f)
                        close()
                        moveToRelative(5f, 4f)
                        verticalLineTo(15f)
                        horizontalLineToRelative(4f)
                        quadToRelative(1.25f, 0f, 2.13f, -0.88f)
                        reflectiveQuadTo(20f, 12f)
                        reflectiveQuadTo(19.13f, 9.88f)
                        reflectiveQuadTo(17f, 9f)
                        horizontalLineTo(13f)
                        verticalLineTo(7f)
                        horizontalLineToRelative(4f)
                        quadToRelative(2.07f, 0f, 3.54f, 1.46f)
                        reflectiveQuadTo(22f, 12f)
                        reflectiveQuadToRelative(-1.46f, 3.54f)
                        reflectiveQuadTo(17f, 17f)
                        horizontalLineTo(13f)
                        close()
                    }
                }
                .build()
        return _link!!
    }

private var _link: ImageVector? = null

@Preview
@Composable
private fun Preview() {
    Box(Modifier.padding(12.dp)) {
        Image(Link, null)
    }
}
