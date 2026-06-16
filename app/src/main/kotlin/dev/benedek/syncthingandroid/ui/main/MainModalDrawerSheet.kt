package dev.benedek.syncthingandroid.ui.main

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.activities.SettingsActivity
import dev.benedek.syncthingandroid.activities.WebGuiActivity
import dev.benedek.syncthingandroid.viewmodel.MainViewModel
import dev.benedek.syncthingandroid.ui.reusable.ComposeBasicLineChart
import dev.benedek.syncthingandroid.ui.reusable.HorizontalDivider
import dev.benedek.syncthingandroid.ui.reusable.OptionTile
import dev.benedek.syncthingandroid.ui.reusable.StatTile
import dev.benedek.syncthingandroid.ui.reusable.topBorderWithCorners
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.ui.theme.extendedColorScheme
import dev.benedek.syncthingandroid.util.ThemeControls
import dev.benedek.syncthingandroid.util.Util
import kotlinx.coroutines.launch

@Composable
fun MainModalDrawerSheet(
	predictiveBackProgress: () -> Float,
	drawerState: () -> DrawerState,
	viewModel: MainViewModel
) {
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	val density = LocalDensity.current
	val containerWidth = LocalWindowInfo.current.containerSize.width


	val contentColor = MaterialTheme.colorScheme.onSurface
	val color = Color.Transparent
	val red = MaterialTheme.extendedColorScheme.red.color
	val green = MaterialTheme.extendedColorScheme.green.color
	val blue = MaterialTheme.extendedColorScheme.blue.color
	val yellow = MaterialTheme.extendedColorScheme.yellow.color


	LaunchedEffect(containerWidth) {
		actualDrawerWidth = if (containerWidth.dp * 0.8f < drawerWidth) {
			if (containerWidth.dp < smallDrawerWidth) containerWidth.dp else maxOf(
				containerWidth.dp * 0.8f,
				smallDrawerWidth
			)
		} else {
			drawerWidth
		}
	}

	ModalDrawerSheet(
		windowInsets = WindowInsets(),
		modifier = Modifier
			.fillMaxHeight()
			.verticalScroll(rememberScrollState())
			.widthIn(max = actualDrawerWidth)
			.graphicsLayer {
				// 1. Slide it away slightly
				translationX = -predictiveBackProgress() * 200f

				// 2. Shrink it slightly (Modern Android look)
				val scale = 1f - (predictiveBackProgress() * 0.05f)
				scaleX = scale
				scaleY = scale

				// 3. Smoothly round the corners more as it pulls away
				shape = RoundedCornerShape(
					topEnd = 16.dp + (predictiveBackProgress() * 16).dp,
					bottomEnd = 16.dp + (predictiveBackProgress() * 16).dp
				)
				clip = true
			}
	) {
		Row(
			Modifier
				.windowInsetsPadding(
					WindowInsets.safeDrawing.only(
						WindowInsetsSides.Top + WindowInsetsSides.Start
					)
				)
				.padding(10.dp, 12.dp),
			Arrangement.Start,
			Alignment.CenterVertically
		) {
			Icon(
				painterResource(R.drawable.ic_monochrome),
				null,
				Modifier.padding(8.dp).size(24.dp),
				MaterialTheme.colorScheme.primary
			)
			Text(
				stringResource(R.string.app_name),
				modifier = Modifier.padding(6.dp),
				style = MaterialTheme.typography.titleLarge
			)
		}

		HorizontalDivider()

		Column(
			Modifier.windowInsetsPadding(
				WindowInsets.safeDrawing.only(WindowInsetsSides.Start)
			),
			Arrangement.spacedBy(16.dp)
		) {
			var tileHeight by remember { mutableStateOf(0.dp) }
			val shape = RectangleShape
			val modifier = Modifier
			val descriptionWeight = FontWeight.Normal
			val titleWeight = FontWeight.Normal

			StatTile(
				modifier = modifier.onSizeChanged { with(density) { tileHeight = it.height.toDp() } },
				title = stringResource(R.string.announce_server),
				titleWeight = titleWeight,
				description = "${viewModel.announceConnected}/${viewModel.announceTotal}",
				descriptionWeight = descriptionWeight,
				descriptionColor = if (viewModel.announceConnected > 0) green else red,
				color = color,
				noIconPadding = true,
				contentColor = contentColor,
				enabled = viewModel.api != null,
				shape = shape,
				chart = {
					ComposeBasicLineChart(
						values = viewModel.announceConnectedHistory
							.let { if (it.size < 2) listOf(0L, 0L) else it }
							.toList(),
						maxValue = viewModel.announceTotal,
						modifier = Modifier.height(tileHeight)
					)
				}
			)
			StatTile(
				modifier = modifier,
				title = stringResource(R.string.ram_usage),
				titleWeight = titleWeight,
				description = Util.readableFileSize(context, viewModel.systemInfo?.sys ?: 0),
				descriptionWeight = descriptionWeight,
				color = color,
				noIconPadding = true,
				contentColor = contentColor,
				enabled = viewModel.api != null,
				shape = shape,
				chart = {
					ComposeBasicLineChart(
						values = viewModel.systemInfoHistory
							.map { it?.sys ?: 0L }
							.let { if (it.size < 2) listOf(0L, 0L) else it },
						modifier = Modifier.weight(0.3f).height(tileHeight)
					)
				}
			)

			StatTile(
				modifier = modifier,
				title = stringResource(R.string.download_title),
				titleWeight = titleWeight,
				description = Util.readableTransferRate(
					context,
					viewModel.deviceStatuses.total?.inBits ?: 0
				),
				descriptionWeight = descriptionWeight,
				color = color,
				noIconPadding = true,
				contentColor = contentColor,
				enabled = viewModel.api != null,
				shape = shape,
				chart = {
					ComposeBasicLineChart(
						values = viewModel.deviceStatusesHistory
							.map { it.total?.inBits ?: 0L }
							.let { if (it.size < 2) listOf(0L, 0L) else it },
						modifier = Modifier.weight(0.3f).height(tileHeight)
					)
				}
			)

			StatTile(
				modifier = modifier,
				title = stringResource(R.string.upload_title),
				titleWeight = titleWeight,
				description = Util.readableTransferRate(
					context,
					viewModel.deviceStatuses.total?.outBits ?: 0
				),
				descriptionWeight = descriptionWeight,
				color = color,
				noIconPadding = true,
				contentColor = contentColor,
				enabled = viewModel.api != null,
				shape = shape,
				chart = {
					ComposeBasicLineChart(
						values = viewModel.deviceStatusesHistory
							.map { it.total?.outBits ?: 0L }
							.let { if (it.size < 2) listOf(0L, 0L) else it },
						modifier = Modifier.weight(0.3f).height(tileHeight)
					)
				}
			)
		}

		HorizontalDivider()
		Spacer(Modifier.weight(1f))

		Surface(
			Modifier
				.padding(top = 16.dp)
				.topBorderWithCorners(
					1.dp,
					MaterialTheme.colorScheme.outlineVariant,
					16.dp
				),
			RoundedCornerShape(0.dp, 16.dp, 16.dp, 0.dp),
			Color.Transparent
		) {
			Column(
				Modifier.windowInsetsPadding(
					WindowInsets.safeDrawing.only(WindowInsetsSides.Start)
				)
			) {
				OptionTile(
					title = stringResource(R.string.web_gui_title),
					leftIconPainter = rememberVectorPainter(Icons.Outlined.Web),
					onClick = {
						scope.launch { drawerState().close() }
						context.startActivity(Intent(context, WebGuiActivity::class.java))
					}
				)
				OptionTile(
					title = stringResource(R.string.show_device_id),
					leftIconPainter = painterResource(R.drawable.ic_qrcode_24dp),
					onClick = {
						scope.launch { drawerState().close() }
						viewModel.showDeviceIdDialog = true
					}
				)
				OptionTile(
					title = stringResource(R.string.settings_title),
					leftIconPainter = rememberVectorPainter(Icons.Outlined.Settings),
					leftIconContentDescription = stringResource(R.string.settings_title),
					onClick = {
						scope.launch { drawerState().close() }
						context.startActivity(Intent(context, SettingsActivity::class.java))
					}
				)
			}
		}

		Surface(
			shape = RoundedCornerShape(0.dp, 16.dp, 16.dp, 0.dp),
		) {
			Column(
				Modifier.windowInsetsPadding(
					WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Bottom)
				)
			) {
				OptionTile(
					title = stringResource(R.string.restart),
					leftIconPainter = painterResource(R.drawable.ic_autorenew_24dp),
					onClick = {
						scope.launch { drawerState().close() }
						viewModel.showRestartDialog = true
					}
				)
				OptionTile(
					title = stringResource(R.string.exit),
					leftIconPainter = rememberVectorPainter(Icons.Outlined.PowerSettingsNew),
					onClick = {
						scope.launch { drawerState().close() }
						viewModel.showExitDialog = true
					}
				)
			}
		}
	}
}

val drawerWidth = 360.dp // M3 spec
val smallDrawerWidth = 240.dp
var actualDrawerWidth by mutableStateOf(drawerWidth)


@Preview(uiMode = ThemeControls.UI_MODE)
@Composable
fun MainModalDrawerSheetPreview() {
	SyncthingandroidTheme(ThemeControls.useDarkMode, ThemeControls.isMonetEnabled) {
		MainModalDrawerSheet({ 0f }, { DrawerState(DrawerValue.Open) }, viewModel<MainViewModel>())
	}
}