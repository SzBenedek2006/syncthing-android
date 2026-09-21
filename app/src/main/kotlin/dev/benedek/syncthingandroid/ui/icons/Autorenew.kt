@file:Suppress("ObjectPropertyName", "BooleanLiteralArgument")

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
val Autorenew: ImageVector
	get() {
		if (_autorenew != null) {
			return _autorenew!!
		}
		_autorenew = ImageVector.Builder(
			name = "Autorenew",
			defaultWidth = 24.dp,
			defaultHeight = 24.dp,
			viewportWidth = 24f,
			viewportHeight = 24f
		).apply {
			path(
				fill = SolidColor(Color.Black),
				fillAlpha = 1f,
				stroke = null,
				strokeAlpha = 1f,
				strokeLineWidth = 1f,
				strokeLineCap = StrokeCap.Butt,
				strokeLineJoin = StrokeJoin.Bevel,
				strokeLineMiter = 1f,
				pathFillType = PathFillType.NonZero,
			) {
				moveTo(5.1f, 16.05f)
				quadTo(4.55f, 15.1f, 4.28f, 14.1f)
				reflectiveQuadTo(4f, 12.05f)
				quadTo(4f, 8.7f, 6.33f, 6.35f)
				reflectiveQuadTo(12f, 4f)
				horizontalLineToRelative(0.18f)
				lineTo(10.58f, 2.4f)
				lineTo(11.98f, 1f)
				lineToRelative(4f, 4f)
				lineToRelative(-4f, 4f)
				lineTo(10.58f, 7.6f)
				lineTo(12.18f, 6f)
				horizontalLineTo(12f)
				quadTo(9.5f, 6f, 7.75f, 7.76f)
				reflectiveQuadTo(6f, 12.05f)
				quadToRelative(0f, 0.65f, 0.15f, 1.28f)
				reflectiveQuadTo(6.6f, 14.55f)
				lineToRelative(-1.5f, 1.5f)
				close()
				moveTo(12.03f, 23f)
				lineToRelative(-4f, -4f)
				lineToRelative(4f, -4f)
				lineToRelative(1.4f, 1.4f)
				lineTo(11.83f, 18f)
				horizontalLineTo(12f)
				quadToRelative(2.5f, 0f, 4.25f, -1.76f)
				reflectiveQuadTo(18f, 11.95f)
				quadTo(18f, 11.3f, 17.85f, 10.68f)
				reflectiveQuadTo(17.4f, 9.45f)
				lineToRelative(1.5f, -1.5f)
				quadTo(19.45f, 8.9f, 19.73f, 9.9f)
				reflectiveQuadTo(20f, 11.95f)
				quadToRelative(0f, 3.35f, -2.32f, 5.7f)
				reflectiveQuadTo(12f, 20f)
				horizontalLineTo(11.83f)
				lineToRelative(1.6f, 1.6f)
				lineTo(12.03f, 23f)
				close()
			}
		}.build()
		return _autorenew!!
	}

private var _autorenew: ImageVector? = null

@Preview
@Composable
private fun Preview() {
	Box(Modifier.padding(12.dp)) {
		Image(Autorenew, null)
	}
}
