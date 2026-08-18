package dev.benedek.syncthingandroid.ui

import android.content.Intent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.activities.DeviceActivity
import dev.benedek.syncthingandroid.activities.FolderPickerActivity
import dev.benedek.syncthingandroid.activities.MainActivity
import dev.benedek.syncthingandroid.activities.QRScannerActivity
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.ui.reusable.AppScaffold
import dev.benedek.syncthingandroid.ui.reusable.AppTextField
import dev.benedek.syncthingandroid.ui.reusable.DeleteDialog
import dev.benedek.syncthingandroid.ui.reusable.HorizontalDivider
import dev.benedek.syncthingandroid.ui.reusable.OptionTile
import dev.benedek.syncthingandroid.ui.reusable.SingleSelectDialog
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.Compression
import dev.benedek.syncthingandroid.util.FileUtils
import dev.benedek.syncthingandroid.util.ThemeControls
import dev.benedek.syncthingandroid.viewmodel.DeviceViewModel

@Composable
fun DeviceScreen(
	viewModel: DeviceViewModel,
	onFinish: () -> Unit = {}
) {
	val context = LocalContext.current
	val focusManager = LocalFocusManager.current

	class StubLauncher {
		fun launch(vararg arg: Any?) {
			TODO()
		}
	}
	val qrScannerLauncher = StubLauncher()
	val directoryPicker = StubLauncher()


	AppScaffold(
		topAppBarTitle =
			if (viewModel.isCreateMode) stringResource(R.string.add_device)
			else stringResource(R.string.edit_device),
		topActionOnClick = { viewModel.onDone(context, onFinish) },
		topActionActive = viewModel.isValidDevice,
		topNavigationOnClick = { viewModel.onCancel(onFinish) },
		modifier = Modifier.pointerInput(Unit) {
			detectTapGestures(onTap = {
				focusManager.clearFocus()
			})
		}
	) { paddingValues ->
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.verticalScroll(rememberScrollState())
				.padding(paddingValues),
		) {
			HorizontalDivider()
			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier.height(IntrinsicSize.Min).fillMaxWidth()
			) {
				AppTextField(
					modifier = Modifier.weight(1f),
					label = stringResource(R.string.device_id),
					leadingIconPainter = rememberVectorPainter(Icons.Outlined.VpnKey),
					value = viewModel.device.deviceID ?: "",
					onValueChange = { viewModel.onIdChange(it) },
					keyboardOptions = KeyboardOptions(
						capitalization = KeyboardCapitalization.Characters,
						keyboardType = KeyboardType.Text
					),
					readOnly = false //isCreateMode
				)

				if (viewModel.isCreateMode) {
					val context = LocalContext.current
					IconButton(
						onClick = { qrScannerLauncher.launch(QRScannerActivity.intent(context)) }
					) {
						Icon(Icons.Outlined.QrCodeScanner, stringResource(R.string.scan_qr_code_description))
					}
				}
			}
			HorizontalDivider()
			AppTextField(
				label = stringResource(R.string.device_name),
				leadingIconPainter = rememberVectorPainter(Icons.AutoMirrored.Outlined.Label),
				value = viewModel.device.name,
				placeholder = viewModel.device.displayName,
				onValueChange = { viewModel.onNameChange(it) }
			)
			HorizontalDivider()
			AppTextField(
				label = stringResource(R.string.addresses),
				placeholder = "Use tcp://ip:port, tcp://host:port format or dynamic", // FIXME: TRANSLATE
				leadingIconPainter = rememberVectorPainter(Icons.Outlined.Link),
				value = viewModel.addresses,
				onValueChange = { viewModel.onAddressChange(it) },
				keyboardOptions = KeyboardOptions(
					capitalization = KeyboardCapitalization.None,
					keyboardType = KeyboardType.Text
				)
			)
			HorizontalDivider()
			OptionTile(
				title = stringResource(R.string.compression),
				description = Compression.fromValue(context, viewModel.device.compression).getTitle(context),
				leftIconPainter = rememberVectorPainter(Icons.Outlined.FolderZip),
				onClick = { viewModel.showCompressionDialog = true }
			)
			HorizontalDivider()
			OptionTile(
				title = stringResource(R.string.introducer),
				leftIconPainter = rememberVectorPainter(Icons.Outlined.Devices),
				checked = viewModel.device.introducer,
				onCheckedChange = { viewModel.onIntroducerChange(it) }
			)
			HorizontalDivider()
			OptionTile(
				title = stringResource(R.string.pause_device),
				leftIconPainter = rememberVectorPainter(Icons.Outlined.Pause),
				checked = viewModel.device.paused,
				onCheckedChange = { viewModel.onPauseChange(it) }
			)
			HorizontalDivider()


			/*
			 * TODO:
			 *
			 * Add Untrusted switch
			 * Add Device Group setting
			 * Add Auto Accept rule switch
			 * Add folders section for sharing folders
			 * 		"Select additional folders to share with this device. Select All Deselect All"
			 *
			 * Add hints with the (or similar to) description found in the web gui.
			 * Add option to copy or share the device's name or device id.
			 */

			if (!viewModel.isCreateMode) {
				OptionTile(
					title = stringResource(R.string.delete_device),
					description = null, // TODO: Add description
					leftIconPainter = rememberVectorPainter(Icons.Outlined.Delete),
					onClick = { viewModel.showDeleteDialog = true },
					contentColor = MaterialTheme.colorScheme.error,
				)
				HorizontalDivider()
			}
		}
	}

	if (viewModel.showCompressionDialog) {

		val context = LocalContext.current
		val resources = LocalResources.current

		SingleSelectDialog(
			stringResource(R.string.compression),
			null,
			remember(resources) {
				mapOf(
					Compression.NONE to resources.getString(R.string.compress_never),
					Compression.METADATA to resources.getString(R.string.compress_metadata),
					Compression.ALWAYS to resources.getString(R.string.compress_always)
				)
			},
			remember(viewModel.device.compression, context) {
				Compression.fromValue(context, viewModel.device.compression)
			},
			{ viewModel.device.compression = it.getValue(context) },
			{ viewModel.showCompressionDialog = false }
		)
	}
	if (viewModel.showDeleteDialog) {
		DeleteDialog(
			{ viewModel.onDelete(onFinish) },
			{ viewModel.showDeleteDialog = false },
			stringResource(R.string.delete_device),
			null // TODO: Add description
		)
	}
}


@Composable
@Preview(showSystemUi = true, showBackground = true, uiMode = ThemeControls.UI_MODE)
fun DeviceScreenPreview() {
	SyncthingandroidTheme(ThemeControls.useDarkMode, dynamicColor = ThemeControls.isMonetEnabled) {
		DeviceScreen(viewModel<DeviceViewModel>())
	}
}