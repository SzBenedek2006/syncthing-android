package dev.benedek.syncthingandroid.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.ui.reusable.AppScaffold
import dev.benedek.syncthingandroid.ui.reusable.AppTextField
import dev.benedek.syncthingandroid.ui.reusable.DeleteDialog
import dev.benedek.syncthingandroid.ui.reusable.HorizontalDivider
import dev.benedek.syncthingandroid.ui.reusable.OptionTile
import dev.benedek.syncthingandroid.ui.reusable.SingleSelectDialog
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.Compression
import dev.benedek.syncthingandroid.util.ThemeControls
import dev.benedek.syncthingandroid.viewmodel.DeviceViewModel

@Composable
fun Device(
	viewModel: DeviceViewModel,
	onFinish: () -> Unit = {}
) {
	val context = LocalContext.current
	val focusManager = LocalFocusManager.current

	LaunchedEffect( // TODO!!!
		viewModel.device.name,
		viewModel.device.deviceID,
		viewModel.device.addresses
	) {
		val device = viewModel.device
		//TODO: Improvement: maybe check if syncthing has access to path
		if (device.name.isEmpty()  || device.deviceID.isNullOrEmpty()) {
			viewModel.isValidDevice = false
		} else {
			viewModel.isValidDevice = true
		}
	}

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
			AppTextField(
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
fun DevicePreview() {
	SyncthingandroidTheme(ThemeControls.useDarkMode, dynamicColor = ThemeControls.isMonetEnabled) {
		Device(viewModel<DeviceViewModel>())
	}
}