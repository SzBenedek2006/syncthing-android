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

public val TagTextOutline: ImageVector
    get() {
        if (_tagTextOutline != null) {
            return _tagTextOutline!!
        }
        _tagTextOutline = Builder(name = "TagTextOutline", defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp, viewportWidth = 27.810644f, viewportHeight =
                27.862204f).apply {
            path(fill = SolidColor(Color(0xFF000000)), stroke = null, strokeLineWidth = 0.35277775f,
                    strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                    pathFillType = NonZero) {
                moveToRelative(14.4639f, 27.6967f)
                curveToRelative(-0.388f, -0.1638f, -3.8012f, -3.4082f, -7.5847f, -7.2097f)
                lineToRelative(-6.8792f, -6.9118f)
                verticalLineToRelative(-5.984f)
                curveToRelative(0.0f, -5.7569f, 0.0305f, -6.0145f, 0.8035f, -6.7876f)
                curveToRelative(0.7731f, -0.7731f, 1.0306f, -0.8036f, 6.7899f, -0.8036f)
                horizontalLineToRelative(5.9863f)
                lineToRelative(7.1154f, 7.1154f)
                curveToRelative(6.2495f, 6.2495f, 7.1154f, 7.2511f, 7.1154f, 8.2301f)
                curveToRelative(0.0f, 0.9706f, -0.7261f, 1.8365f, -5.615f, 6.6972f)
                curveToRelative(-3.0883f, 3.0704f, -5.9325f, 5.6656f, -6.3206f, 5.7671f)
                curveToRelative(-0.388f, 0.1015f, -1.023f, 0.0506f, -1.4111f, -0.1132f)
                close()
                moveTo(20.4628f, 20.283f)
                lineTo(25.394f, 15.3424f)
                lineTo(19.3117f, 9.2601f)
                lineTo(13.2294f, 3.1778f)
                lineTo(8.2049f, 8.2023f)
                lineTo(3.1804f, 13.2268f)
                lineTo(9.1711f, 19.2252f)
                curveToRelative(3.2948f, 3.2991f, 6.0739f, 5.9984f, 6.1756f, 5.9984f)
                curveToRelative(0.1018f, 0.0f, 2.404f, -2.2233f, 5.1162f, -4.9406f)
                close()
                moveTo(10.6537f, 15.7693f)
                lineTo(7.9181f, 13.0176f)
                lineTo(8.822f, 12.1684f)
                lineTo(9.726f, 11.3192f)
                lineTo(12.4423f, 14.0355f)
                lineTo(15.1586f, 16.7518f)
                lineTo(14.2739f, 17.6364f)
                lineTo(13.3893f, 18.5211f)
                close()
                moveTo(15.2398f, 13.2998f)
                lineTo(11.4437f, 9.4918f)
                lineTo(12.3492f, 8.6412f)
                lineTo(13.2546f, 7.7906f)
                lineTo(17.0292f, 11.5652f)
                lineTo(20.8038f, 15.3398f)
                lineTo(19.9199f, 16.2238f)
                lineTo(19.036f, 17.1077f)
                close()
                moveTo(6.3248f, 6.3248f)
                curveToRelative(0.3049f, -0.3049f, 0.5544f, -1.0079f, 0.5544f, -1.5623f)
                curveToRelative(0.0f, -0.5544f, -0.2495f, -1.2574f, -0.5544f, -1.5623f)
                curveToRelative(-0.3049f, -0.3049f, -1.0079f, -0.5544f, -1.5623f, -0.5544f)
                curveToRelative(-0.5544f, 0.0f, -1.2574f, 0.2495f, -1.5623f, 0.5544f)
                curveToRelative(-0.3049f, 0.3049f, -0.5544f, 1.0079f, -0.5544f, 1.5623f)
                curveToRelative(0.0f, 0.5544f, 0.2495f, 1.2574f, 0.5544f, 1.5623f)
                curveToRelative(0.3049f, 0.3049f, 1.0079f, 0.5544f, 1.5623f, 0.5544f)
                curveToRelative(0.5544f, 0.0f, 1.2574f, -0.2495f, 1.5623f, -0.5544f)
                close()
            }
        }
        .build()
        return _tagTextOutline!!
    }

private var _tagTextOutline: ImageVector? = null

@Preview
@Composable
private fun Preview() {
    Box(Modifier.padding(12.dp)) {
        Image(TagTextOutline, null)
    }
}
