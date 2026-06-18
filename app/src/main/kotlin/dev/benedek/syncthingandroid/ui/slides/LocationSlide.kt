package dev.benedek.syncthingandroid.ui.slides

import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.ui.LocalIsLandscape
import dev.benedek.syncthingandroid.ui.reusable.AdaptiveSlideLayout
import dev.benedek.syncthingandroid.ui.reusable.SlideDescription
import dev.benedek.syncthingandroid.ui.reusable.SlideImage
import dev.benedek.syncthingandroid.ui.reusable.SlideTitle
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls

@Composable
fun LocationSlide(
	onButtonClick: () -> Unit,
	isPermissionGranted: Boolean
) {
	AdaptiveSlideLayout(
		{
			SlideTitle(stringResource(R.string.location_permission_title))
		},
		{
			SlideDescription(
				stringResource(R.string.location_permission_desc)
			)
		},
		Modifier,
		{
			SlideImage(painterResource(R.drawable.ic_location))
		},
		{
			Button(
				onClick = onButtonClick,
				enabled = !isPermissionGranted
			) {
				Text(
					if (!isPermissionGranted) {
						stringResource(R.string.grant_permission)
					} else {
						stringResource(R.string.permission_granted)
					}
				)
			}
		}
	)
}

@Preview(showBackground = true, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Composable
fun LocationSlidePreview() {
	SyncthingandroidTheme(dynamicColor = ThemeControls.isMonetEnabled) {
		Surface {
			LocationSlide({}, false)
		}
	}
}

@Preview(showBackground = true, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES, widthDp = 800, heightDp = 400)
@Composable
fun LocationSlideLandscapePreview() {
	SyncthingandroidTheme(dynamicColor = ThemeControls.isMonetEnabled) {
		CompositionLocalProvider(LocalIsLandscape provides true) {
			Surface {
				LocationSlide({}, false)
			}
		}
	}
}
