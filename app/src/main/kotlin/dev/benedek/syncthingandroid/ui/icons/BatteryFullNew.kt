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
val BatteryFullNew: ImageVector
	get() {
		if (_batteryFullNew != null) {
			return _batteryFullNew!!
		}
		_batteryFullNew =
			ImageVector.Builder(
				name = "BatteryFullNew",
				defaultWidth = 24.dp,
				defaultHeight = 24.dp,
				viewportWidth = 24f,
				viewportHeight = 24f,
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
					pathFillType = PathFillType.Companion.NonZero,
				) {
					moveTo(4f, 18f)
					quadTo(2.75f, 18f, 1.88f, 17.13f)
					reflectiveQuadTo(1f, 15f)
					verticalLineTo(9f)
					quadTo(1f, 7.75f, 1.88f, 6.88f)
					reflectiveQuadTo(4f, 6f)
					horizontalLineTo(17.5f)
					quadToRelative(1.25f, 0f, 2.13f, 0.88f)
					reflectiveQuadTo(20.5f, 9f)
					verticalLineToRelative(6f)
					quadToRelative(0f, 1.25f, -0.88f, 2.13f)
					reflectiveQuadTo(17.5f, 18f)
					horizontalLineTo(4f)
					close()
					moveTo(21.5f, 14.5f)
					verticalLineToRelative(-5f)
					horizontalLineTo(22f)
					quadToRelative(0.43f, 0f, 0.71f, 0.29f)
					reflectiveQuadTo(23f, 10.5f)
					verticalLineToRelative(3f)
					quadToRelative(0f, 0.42f, -0.29f, 0.71f)
					reflectiveQuadTo(22f, 14.5f)
					horizontalLineTo(21.5f)
					close()
				}
			}.build()
		return _batteryFullNew!!
	}

private var _batteryFullNew: ImageVector? = null

@Preview
@Composable
private fun Preview() {
	Box(Modifier.padding(12.dp)) {
		Image(BatteryFullNew, null)
	}
}