package dev.benedek.syncthingandroid.ui

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.FloatRange
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.activities.QRScannerActivity
import dev.benedek.syncthingandroid.ui.reusable.AppScaffold
import dev.benedek.syncthingandroid.ui.reusable.AppTextField
import dev.benedek.syncthingandroid.ui.reusable.ComposeDialog
import dev.benedek.syncthingandroid.ui.reusable.DeleteDialog
import dev.benedek.syncthingandroid.ui.reusable.OptionTile
import dev.benedek.syncthingandroid.ui.reusable.SingleSelectDialog
import dev.benedek.syncthingandroid.ui.reusable.ThemedHorizontalDivider
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.Compression
import dev.benedek.syncthingandroid.util.ThemeControls
import dev.benedek.syncthingandroid.util.Util.logD
import dev.benedek.syncthingandroid.viewmodel.DeviceViewModel
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.ln

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceScreen(
	viewModel: DeviceViewModel,
	onFinish: () -> Unit = {}
) {
	val context = LocalContext.current
	val focusManager = LocalFocusManager.current

	/**
	 * Receives value of scanned QR code and sets it as device ID.
	 */
	val qrScannerLauncher: ActivityResultLauncher<Intent> = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
		if (result.resultCode == RESULT_OK) {
			val scanResult = result.data?.getStringExtra(QRScannerActivity.QR_RESULT_ARG)
			if (scanResult != null) {
				viewModel.updateDeviceId(scanResult)
			}
		}
	}


	AppScaffold(
		topAppBarTitle =
			if (viewModel.isCreateMode) stringResource(R.string.add_device)
			else stringResource(R.string.edit_device),
		topActionOnClick = if (viewModel.deviceNeedsToUpdate) {{ viewModel.onDone(context, onFinish) }} else null,
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
			ThemedHorizontalDivider()
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
					readOnly = !viewModel.isCreateMode
				)

				if (viewModel.isCreateMode) {
					val context = LocalContext.current
					IconButton(
						onClick = { qrScannerLauncher.launch(QRScannerActivity.intent(context)) },
						modifier = Modifier.padding(horizontal = 14.dp)
					) {
						Icon(Icons.Outlined.QrCodeScanner, stringResource(R.string.scan_qr_code_description))
					}
				}
			}
			ThemedHorizontalDivider()
			AppTextField(
				label = stringResource(R.string.device_name),
				leadingIconPainter = rememberVectorPainter(Icons.AutoMirrored.Outlined.Label),
				value = viewModel.device.name,
				placeholder = viewModel.device.displayName,
				onValueChange = { viewModel.onNameChange(it) }
			)
			ThemedHorizontalDivider()
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
			ThemedHorizontalDivider()
			OptionTile(
				title = stringResource(R.string.compression),
				description = Compression.fromValue(context, viewModel.device.compression).getTitle(context),
				leftIconPainter = rememberVectorPainter(Icons.Outlined.FolderZip),
				onClick = { viewModel.showCompressionDialog = true }
			)
			ThemedHorizontalDivider()
			OptionTile(
				title = stringResource(R.string.introducer),
				leftIconPainter = rememberVectorPainter(Icons.Outlined.Devices),
				checked = viewModel.device.introducer,
				onCheckedChange = { viewModel.onIntroducerChange(it) }
			)
			ThemedHorizontalDivider()
			OptionTile(
				title = stringResource(R.string.pause_device),
				leftIconPainter = rememberVectorPainter(Icons.Outlined.Pause),
				checked = viewModel.device.paused,
				onCheckedChange = { viewModel.onPauseChange(it) }
			)
			ThemedHorizontalDivider()


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
				ThemedHorizontalDivider()
			}

			if (!viewModel.currentAddress.isNullOrEmpty()) {
				OptionTile(
					enabled = false,
					title = stringResource(R.string.current_address),
					description = viewModel.currentAddress,
					leftIconPainter = rememberVectorPainter(Icons.Outlined.DeviceHub),
				)
				ThemedHorizontalDivider()
			}

			if (!viewModel.deviceVersion.isNullOrEmpty()) {
				OptionTile(
					enabled = false,
					title = stringResource(R.string.syncthing_version_title),
					description = viewModel.deviceVersion,
					leftIconPainter = rememberVectorPainter(Icons.Outlined.Info),
				)
				ThemedHorizontalDivider()
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
			{ viewModel.onCompressionChange(it.getValue(context)) },
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

	/**
	 * This is needed due another horrible bug in Android
	 */
	val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
	val isSoftwareKeyboardVisible = WindowInsets.isImeVisible // && imeBottom > 0 TODO: (maybe needed?)

	var backProgress by remember { mutableStateOf<Float?>(null) } // FIXME

	fun backProgressMultiplier(value: Float, @FloatRange(0.0) multiplier: Float): Float {
		return ln(1 + multiplier * value) / ln(1 + multiplier)
	}

	/*
	 * Without checking IME visibility check, it activates for the first swipe!
	 */
	PredictiveBackHandler(viewModel.deviceNeedsToUpdate && !isSoftwareKeyboardVisible) { backEventFlow ->
		logD("PredictiveBackHandler ran!")
		try {
			backEventFlow.collect {
				backProgress = backProgressMultiplier(it.progress, 10f)
				if (backProgress != null && backProgress!! > 0f)
					viewModel.showDiscardDialog = true
				Log.d("BackProgress", backProgress.toString())
			}
			viewModel.showDiscardDialog = true
		} catch (_: CancellationException) {
			viewModel.showDiscardDialog = false
		} finally {
			backProgress = null
		}
	}

	val animatedProgress = backProgress?.let { animateFloatAsState(
		targetValue = it,
		animationSpec = spring(),
		label = "animatedProgress"
	) }

	AnimatedVisibility(viewModel.showDiscardDialog, enter = fadeIn(), exit = fadeOut()) {
		ComposeDialog(
			onOk = onFinish,
			onCancel = { viewModel.showDiscardDialog = false },

			modifier = Modifier,
			onDismiss = { viewModel.showDiscardDialog = false },
			title = stringResource(R.string.dialog_discard_changes),
			animationProgress = animatedProgress?.value,
			shouldCancel = !viewModel.showDiscardDialog
		)
	}

	AnimatedVisibility(viewModel.showAlreadyAddedDialog, enter = fadeIn(), exit = fadeOut()) {
		ComposeDialog(
			onOk = {
				val deviceID = viewModel.device.deviceID
				if (deviceID == null) {
					Toast.makeText(context, "Failed to load device", Toast.LENGTH_LONG).show()
					return@ComposeDialog
				}
				viewModel.loadExistingDevice(deviceID, onFinish, context) /*Edit already existing device*/
				viewModel.showAlreadyAddedDialog = false
			},
			onCancel = {
				viewModel.showAlreadyAddedDialog = false
				viewModel.onIdChange(null)
			},

			onDismiss = {
				viewModel.showAlreadyAddedDialog = false
				viewModel.onIdChange(null)
			},
			title = stringResource(R.string.device_already_added),
			description = stringResource(R.string.device_already_added_edit_question),
			shouldCancel = !viewModel.showAlreadyAddedDialog,
			okText = stringResource(R.string.edit_device),
			cancelText = stringResource(android.R.string.cancel),
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

@Preview(showSystemUi = true, showBackground = true, uiMode = ThemeControls.UI_MODE)
@Composable
fun AAPreview() {
	SyncthingandroidTheme(ThemeControls.useDarkMode, ThemeControls.isMonetEnabled) {
		ComposeDialog(
			onOk = { /*Edit already existing device*/ },
			onCancel = { },

			onDismiss = { },
			title = stringResource(R.string.device_already_added),
			description = stringResource(R.string.device_already_added_edit_question),
			shouldCancel = false,
			okText = stringResource(R.string.edit_device),
			cancelText = stringResource(android.R.string.cancel),
		)
	}
}

