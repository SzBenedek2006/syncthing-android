package dev.benedek.syncthingandroid.ui.slides

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.ui.LocalIsLandscape
import dev.benedek.syncthingandroid.ui.icons.battery_android_full
import dev.benedek.syncthingandroid.ui.reusable.AdaptiveSlideLayout
import dev.benedek.syncthingandroid.ui.reusable.DenyButton
import dev.benedek.syncthingandroid.ui.reusable.SlideDescription
import dev.benedek.syncthingandroid.ui.reusable.SlideImage
import dev.benedek.syncthingandroid.ui.reusable.SlideTitle
import dev.benedek.syncthingandroid.ui.reusable.TextLayout
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls

@Composable
fun BatterySlide(
	onGrantPermissionClick: () -> Unit,
	isPermissionGranted: Boolean,
	onDenyClick: () -> Unit
	) {
	AdaptiveSlideLayout(
		{
			SlideTitle(stringResource(R.string.dialog_disable_battery_optimization_title))
		},
		{
			SlideDescription(
				stringResource(R.string.dialog_disable_battery_optimization_message),
				"",
				textLayout = TextLayout.Fixed
			)
		},
		Modifier,
		{
			if (Build.VERSION.SDK_INT_FULL > Build.VERSION_CODES_FULL.BAKLAVA) {
				SlideImage(rememberVectorPainter(battery_android_full))
			} else {
				SlideImage(rememberVectorPainter(Icons.Outlined.BatteryStd))
			}
		},
		{
			Button(
				onClick = onGrantPermissionClick,
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
			if (!isPermissionGranted) {
				DenyButton(
					onClick = onDenyClick,
				) {
					Text(
						stringResource(R.string.dont_show_again)
					)
				}
			}
		}
	)
}

@Preview(showBackground = true, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Composable
fun BatterySlidePreview() {
	SyncthingandroidTheme(dynamicColor = ThemeControls.isMonetEnabled) {
		Surface {
			BatterySlide({}, false, {})
		}
	}
}

@Preview(showBackground = true, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES, widthDp = 800, heightDp = 400)
@Composable
fun BatterySlideLandscapePreview() {
	SyncthingandroidTheme(dynamicColor = ThemeControls.isMonetEnabled) {
		CompositionLocalProvider(LocalIsLandscape provides true) {
			Surface {
				BatterySlide({}, false, {})
			}
		}
	}
}
