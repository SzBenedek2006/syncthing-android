package dev.benedek.syncthingandroid.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Process.myUid
import android.util.Log
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.FloatRange
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.activities.FolderPickerActivity
import dev.benedek.syncthingandroid.model.Device
import dev.benedek.syncthingandroid.model.Folder
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.ui.reusable.AppDropDownMenu
import dev.benedek.syncthingandroid.ui.reusable.AppScaffold
import dev.benedek.syncthingandroid.ui.reusable.AppTextField
import dev.benedek.syncthingandroid.ui.reusable.CustomDialog
import dev.benedek.syncthingandroid.ui.reusable.DeleteDialog
import dev.benedek.syncthingandroid.ui.reusable.ComposeDialog
import dev.benedek.syncthingandroid.ui.reusable.HorizontalDivider
import dev.benedek.syncthingandroid.ui.reusable.OptionTile
import dev.benedek.syncthingandroid.ui.reusable.SingleSelectDialog
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.FileUtils
import dev.benedek.syncthingandroid.util.ThemeControls
import dev.benedek.syncthingandroid.util.Util.logD
import dev.benedek.syncthingandroid.viewmodel.FolderViewModel
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.ln
import kotlin.math.min

data class FolderUiState(
	val folder: Folder = Folder(),
	val path: TextFieldState = TextFieldState(),
	val isCreateMode: Boolean = false,
	val isValidFolder: Boolean = false,
	val isPathWritable: Boolean = false,
	val deviceList: List<FolderViewModel.DeviceUiState> = emptyList(),
	val folderType: List<FolderViewModel.FolderType> = emptyList(),
	val folderPullOrders: List<FolderViewModel.FolderPullOrder> = emptyList(),
	val editedVersioning: Folder.Versioning? = null,
	val showDeleteDialog: Boolean = false,
	val showDiscardDialog: Boolean = false,
	val showFolderTypeDialog: Boolean = false,
	val showFolderPullOrderDialog: Boolean = false,
	val showVersioningDialog: Boolean = false,
	val folderNeedsToUpdate: Boolean = false
)

@Immutable
data class FolderActions(
	val onFinish: () -> Unit = {},
	val onDone: (Context, onFinish: () -> Unit) -> Unit = { _, _ -> },
	val onCancel: (onFinish: () -> Unit) -> Unit = {},
	val onDelete: (onFinish: () -> Unit) -> Unit = {},
	val onFolderSelectedViaSaf: (Uri, Context) -> Unit = { _, _ -> },
	val onPathChange: (String) -> Unit = {},
	val setShowDeleteDialog: (Boolean) -> Unit = {},
	val setShowDiscardDialog: (Boolean) -> Unit = {},
	val setShowFolderTypeDialog: (Boolean) -> Unit = {},
	val onPausedChange: (checked: Boolean) -> Unit = {},
	val onLabelChange: (value: String) -> Unit = {},
	val onIdChange: (value: String) -> Unit = {},
	val onDeviceSelectionChange: (device: Device, isSelected: Boolean) -> Unit = { _, _ -> },
	val setShowVersioningDialog: (Boolean) -> Unit = {},
	val onFsWatcherChange: (Boolean) -> Unit = {},
	val editIgnores: (Context) -> Unit = {},
	val onFolderTypeChange: (type: String) -> Unit = {},
	val setShowFolderPullOrderDialog: (Boolean) -> Unit = {},
	val setEditedVersioning: (Folder.Versioning?) -> Unit = {},
	val onVersioningSave: () -> Unit = {},
	val onVersioningChange: (type: String?, param: String?, paramValue: String?) -> Unit = { _, _, _ -> },
	val onPullOrderChange: (order: String) -> Unit = {},
	val onPickerReturned: (String) -> Unit = {},
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun FolderScreen(
	state: FolderUiState,
	actions: FolderActions
) {
	logD("ui: ${state.isValidFolder}")

	val context = LocalContext.current
	val focusManager = LocalFocusManager.current


	with(actions) {
		val directoryPicker = rememberLauncherForActivityResult(
			contract = ActivityResultContracts.OpenDocumentTree()
		) { uri: Uri? ->
			if (uri != null) {
				onFolderSelectedViaSaf(uri, context)
			}
		}

		val advancedDirectoryPicker = rememberLauncherForActivityResult(
			contract = ActivityResultContracts.StartActivityForResult()
		) { result ->
			if (result.resultCode == Activity.RESULT_OK) {
				val path =
					result.data?.getStringExtra(FolderPickerActivity.EXTRA_RESULT_DIRECTORY)
				if (!path.isNullOrEmpty()) {
					onPickerReturned(path)
				}
			}
		}

		AppScaffold(
			topAppBarTitle =
				if (state.isCreateMode) stringResource(R.string.create_folder)
				else stringResource(R.string.edit_folder),
			topActionOnClick = { onDone(context, onFinish) },
			topActionActive = state.isValidFolder,
			topNavigationOnClick = { onCancel(onFinish) },
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
					label = R.string.folder_label,
					leadingIconPainter = R.drawable.ic_label_outline_24dp,
					value = state.folder.label ?: "",
					onValueChange = onLabelChange
				)
				HorizontalDivider()
				AppTextField(
					label = stringResource(R.string.folder_id),
					leadingIconPainter = rememberVectorPainter(Icons.Outlined.VpnKey),
					value = state.folder.id ?: "",
					onValueChange = onIdChange,
					keyboardOptions = KeyboardOptions(
						capitalization = KeyboardCapitalization.None,
						keyboardType = KeyboardType.Text
					),
					readOnly = !state.isCreateMode
				)
				HorizontalDivider()
				Row(
					verticalAlignment = Alignment.CenterVertically,
					modifier = Modifier.height(IntrinsicSize.Min)
				) {
					val userId = remember { myUid() / 100000 }
					val homePrefix = remember { "/storage/emulated/$userId" }
					val color = MaterialTheme.colorScheme.primary

					AppTextField(
						label = stringResource(R.string.directory),
						leadingIconPainter = rememberVectorPainter(Icons.Outlined.Folder),
						state = state.path,
						modifier = Modifier.weight(1f),
						readOnly = !state.isCreateMode,
						inputTransformation = InputTransformation {
							if (
								// If the home prefix is present and
								originalText.startsWith(homePrefix) &&
								// If the homePrefix was deleted as a result of the transformation
								asCharSequence().toString() == originalText.removePrefix(
									homePrefix
								).toString()
							) {
								replace(
									0,
									min(1, originalText.length),
									homePrefix.removeRange(homePrefix.length - 1, homePrefix.length)
								)
							}
							if (asCharSequence().startsWith('~')) {
								replace(0, 1, homePrefix)
							}
							actions.onPathChange(asCharSequence().toString())
							logD("path: ${asCharSequence()}")
						},
						outputTransformation = OutputTransformation {
							if (asCharSequence().toString().startsWith(homePrefix)) {
								replace(
									start = 0,
									end = homePrefix.length,
									text = "~"
								)
								addStyle(
									spanStyle = SpanStyle(
										color = color,
										fontWeight = FontWeight.Bold
									),
									start = 0,
									end = 1
								)
							}
						}
					)
					if (state.isCreateMode) {

						if (state.isPathWritable) {
							Icon(Icons.Outlined.CheckCircle, "")
						}

						Button(
							onClick = {
								val prefs =
									PreferenceManager.getDefaultSharedPreferences(context)

								if (prefs.getBoolean(
										Constants.PREF_ADVANCED_FOLDER_PICKER,
										false
									)
								) {
									val intent = FolderPickerActivity.createIntent(
										context = context,
										initialDirectory = state.path.toString(),
										rootDirectory = null // or whatever your logic requires
									)
									advancedDirectoryPicker.launch(intent)
								} else {
									directoryPicker.launch(
										FileUtils.getPickerInitialUri(
											context,
											state.path.toString()
										)
									)
								}

							},
							shape = MaterialTheme.shapes.medium,
							modifier = Modifier.fillMaxHeight().padding(horizontal = 14.dp, vertical = 18.dp)
						) {
							Text(stringResource(R.string.select))

						}
					}
				}
				HorizontalDivider()

				var showItems by rememberSaveable { mutableStateOf(false) }
				Surface(tonalElevation = if (showItems) 4.dp else 0.dp) {
					Column() {
						val rotationAmount: Float? = if (showItems) 180f else 0f
						OptionTile(
							title = stringResource(R.string.devices),
							leftIconPainter = rememberVectorPainter(Icons.Outlined.Devices),
							rightIconPainter = rememberVectorPainter(Icons.Outlined.ExpandMore),
							onClick = { showItems = !showItems },
							rightIconRotationAmount = rotationAmount,
							contentColor = if (showItems) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
						)
						DeviceListSection(
							state.deviceList,
							showItems,
							onDeviceSelectionChange
						)
					}
				}
				HorizontalDivider()
				OptionTile(
					title = stringResource(R.string.folder_type),
					description = state.folderType.find { it.value == state.folder.type }?.titleRes?.let {
						stringResource(
							it
						)
					},
					leftIconPainter = painterResource(R.drawable.folder_managed_24px),
					onClick = { setShowFolderTypeDialog(true) }
				)
				HorizontalDivider()

				OptionTile(
					title = stringResource(R.string.folder_fileWatcher),
					description = stringResource(R.string.folder_fileWatcherDescription),
					leftIconPainter = painterResource(R.drawable.sync_eye_24dp),
					checked = state.folder.fsWatcherEnabled,
					onCheckedChange = { onFsWatcherChange(!state.folder.fsWatcherEnabled) }
				)
				HorizontalDivider()
				OptionTile(
					title = stringResource(R.string.folder_pause),
					leftIconPainter = rememberVectorPainter(Icons.Outlined.Pause),
					checked = state.folder.paused,
					onCheckedChange = { onPausedChange(!state.folder.paused) }
				)
				HorizontalDivider()
				OptionTile(
					title = stringResource(R.string.pull_order),
					description = state.folderPullOrders.find { it.value == state.folder.order }?.titleRes?.let {
						stringResource(
							it
						)
					}
						?: state.folderPullOrders.firstOrNull()?.titleRes?.let {
							stringResource(
								it
							)
						}
						?: stringResource(R.string.pull_order),

					leftIconPainter = rememberVectorPainter(Icons.AutoMirrored.Outlined.Sort),
					onClick = { setShowFolderPullOrderDialog(true) }
				)
				HorizontalDivider()
				OptionTile(
					title = stringResource(R.string.file_versioning),
					description = if (state.folder.versioning?.type.isNullOrEmpty() || state.folder.versioning == null)
						Constants.FVER_TYPE_NONE else {
						state.folder.versioning!!.type +
								when (state.folder.versioning!!.type) {
									Constants.FVER_TYPE_SIMPLE ->
										"\n" + Constants.FVER_PARAM_SIMPLE_KEEP + " = " + state.folder.versioning?.params[Constants.FVER_PARAM_SIMPLE_KEEP]

									Constants.FVER_TYPE_TRASHCAN ->
										"\n" + Constants.FVER_PARAM_TRASHCAN_CLEANDAYS + " = " + state.folder.versioning?.params[Constants.FVER_PARAM_TRASHCAN_CLEANDAYS]

									Constants.FVER_TYPE_STAGGERED ->
										"\n" + Constants.FVER_PARAM_STAGGERED_PATH + " = " + state.folder.versioning?.params[Constants.FVER_PARAM_STAGGERED_PATH] + "\n" +
												Constants.FVER_PARAM_STAGGERED_MAXAGE + " = " + state.folder.versioning?.params[Constants.FVER_PARAM_STAGGERED_MAXAGE]

									Constants.FVER_TYPE_EXTERNAL ->
										"\n" + Constants.FVER_PARAM_EXTERNAL_COMMAND + " = " + state.folder.versioning?.params[Constants.FVER_PARAM_EXTERNAL_COMMAND]

									else -> ""
								}
					},
					leftIconPainter = rememberVectorPainter(Icons.Outlined.Archive),
					onClick = { setShowVersioningDialog(true) }
				)
				HorizontalDivider()
				OptionTile(
					title = stringResource(R.string.ignore_patterns),
					description = stringResource(R.string.open_stignore_description),
					leftIconPainter = rememberVectorPainter(Icons.Outlined.FilterAlt),
					onClick = { editIgnores(context) },
					enabled = state.isValidFolder
				)
				HorizontalDivider()
				if (!state.isCreateMode) {
					OptionTile(
						title = stringResource(R.string.delete_folder),
						description = stringResource(R.string.delete_folder_description),
						leftIconPainter = rememberVectorPainter(Icons.Outlined.Delete),
						onClick = { setShowDeleteDialog(true) },
						contentColor = MaterialTheme.colorScheme.error,
					)
					HorizontalDivider()
				}
			}
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
		PredictiveBackHandler(state.folderNeedsToUpdate && !isSoftwareKeyboardVisible) { backEventFlow ->
			logD("PredictiveBackHandler ran!")
			try {
				backEventFlow.collect {
					backProgress = backProgressMultiplier(it.progress, 10f)
					if (backProgress != null && backProgress!! > 0f)
						setShowDiscardDialog(true)
					Log.d("BackProgress", backProgress.toString())
				}
				setShowDiscardDialog(true)
			} catch (_: CancellationException) {
				setShowDiscardDialog(false)
			} finally {
				backProgress = null
			}
		}

		val animatedProgress = backProgress?.let { animateFloatAsState(
			targetValue = it,
			animationSpec = spring(),
			label = "animatedProgress"
		) }

		AnimatedVisibility(state.showDiscardDialog, enter = fadeIn(), exit = fadeOut()) {
			ComposeDialog(
				onOk = onFinish,
				onCancel = { setShowDiscardDialog(false) },
				modifier = Modifier,
				onDismiss = { setShowDiscardDialog(false) },
				title = stringResource(R.string.dialog_discard_changes),
				animationProgress = animatedProgress?.value,
				shouldCancel = !state.showDiscardDialog
			)
		}

		if (state.showFolderTypeDialog) {
			SingleSelectDialog(
				title = stringResource(R.string.folder_type),
				text = null,
				items = state.folderType.map { stringResource(it.titleRes) },
				initialSelectedIndex = state.folderType.indexOfFirst { it.value == state.folder.type },
				onSelect = { index ->
					onFolderTypeChange(state.folderType[index].value)
				},
				onDismiss = { setShowFolderTypeDialog(false) }
			)
		}
		if (state.showFolderPullOrderDialog) {
			SingleSelectDialog(
				title = stringResource(R.string.pull_order),
				text = null,
				items = state.folderPullOrders.map { stringResource(it.titleRes) },
				initialSelectedIndex = state.folderPullOrders.indexOfFirst { it.value == state.folder.order },
				onSelect = { index ->
					onPullOrderChange(state.folderPullOrders[index].value)
				},
				onDismiss = { setShowFolderPullOrderDialog(false) }
			)
		}
		if (state.showVersioningDialog) {

			var typeIndex by remember(state.editedVersioning?.type) {
				mutableIntStateOf(
					Constants.FVER_TYPES
						.indexOf(state.editedVersioning?.type)
						.coerceAtLeast(0)
				)
			}

			VersioningDialog(
				title = stringResource(R.string.file_versioning),
				onDismissRequest = {
					setEditedVersioning(state.folder.versioning!!.deepCopy())
					setShowVersioningDialog(false)
				},
				typeIndex = typeIndex,
				onSelectedIndexChange = { index ->
					onVersioningChange(Constants.FVER_TYPES[index], null, null)
					typeIndex = index
				},
				editedVersioning = state.editedVersioning,
				onVersioningSave = onVersioningSave,
				setShowVersioningDialog = setShowVersioningDialog,
				onVersioningChange = onVersioningChange
			)
		}
		if (state.showDeleteDialog) {
			DeleteDialog(
				{ onDelete(onFinish) },
				{ setShowDeleteDialog(false) },
				stringResource(R.string.delete_folder),
				stringResource(R.string.delete_folder_description)
			)
		}
	}

}


@Composable
fun DeviceListSection(
	deviceList: List<FolderViewModel.DeviceUiState>,
	showItems: Boolean,
	onDeviceChecked: (Device, Boolean) -> Unit
) {
	AnimatedVisibility(
		visible = showItems,
		enter = expandVertically() + fadeIn(),
		exit = shrinkVertically() + fadeOut()
	) {
		if (deviceList.isEmpty()) {
			Box(modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp), Alignment.Center) {
				Text(stringResource(R.string.devices_list_empty), style = MaterialTheme.typography.titleMedium)
			}
		} else {
			Column {
				deviceList.forEachIndexed { _, item ->
					key(item.device.deviceID) {
						OptionTile(
							title = item.device.displayName,
							checked = item.isSelected,
							onCheckedChange = { isChecked ->
								onDeviceChecked(item.device, isChecked)
							}
						)
					}
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersioningDialog(
	title: String? = null,
	description: String? = null,
	onDismissRequest: () -> Unit,
	typeIndex: Int,
	onSelectedIndexChange: (Int) -> Unit,
	editedVersioning: Folder.Versioning?,
	onVersioningSave: () -> Unit,
	setShowVersioningDialog: (Boolean) -> Unit,
	onVersioningChange: (type: String?, param: String?, paramValue: String?) -> Unit
) {

	val onOk = {
		onVersioningSave()
		setShowVersioningDialog(false)
	}
	CustomDialog(
		title,
		description,
		onDismissRequest,
		onOk,
	) {
		Column(
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			val focusManager = LocalFocusManager.current

			AppDropDownMenu(
				null,
				Constants.FVER_TYPES,
				typeIndex,
				onSelectedIndexChange,
				focusManager
			)

			val types = remember { Constants.FVER_TYPES }

			when (types[typeIndex]) {
				Constants.FVER_TYPE_SIMPLE -> {
					Text(stringResource(R.string.simple_file_versioning_description))
					OutlinedTextField(
						value = editedVersioning?.params[Constants.FVER_PARAM_SIMPLE_KEEP]
							?: "",
						onValueChange = {
							onVersioningChange(
								types[typeIndex],
								Constants.FVER_PARAM_SIMPLE_KEEP,
								it
							)
						},
						label = { Text(Constants.FVER_PARAM_SIMPLE_KEEP) }
					)
					Text(stringResource(R.string.keep_versions_description))
				}

				Constants.FVER_TYPE_TRASHCAN -> {
					Text(stringResource(R.string.trashcan_versioning_description))
					OutlinedTextField(
						value = editedVersioning?.params[Constants.FVER_PARAM_TRASHCAN_CLEANDAYS]
							?: "",
						onValueChange = {
							onVersioningChange(
								types[typeIndex],
								Constants.FVER_PARAM_TRASHCAN_CLEANDAYS,
								it
							)
						},
						label = { Text(Constants.FVER_PARAM_TRASHCAN_CLEANDAYS) }
					)
					Text(stringResource(R.string.cleanout_after_description))
				}

				Constants.FVER_TYPE_STAGGERED -> {
					Text(stringResource(R.string.staggered_versioning_description))
					OutlinedTextField(
						value = editedVersioning?.params[Constants.FVER_PARAM_STAGGERED_PATH]
							?: "",
						onValueChange = {
							onVersioningChange(
								types[typeIndex],
								Constants.FVER_PARAM_STAGGERED_PATH,
								it
							)
						},
						label = { Text(Constants.FVER_PARAM_STAGGERED_PATH) }

					)
					Text(stringResource(R.string.versions_path_description))
					OutlinedTextField(
						value = editedVersioning?.params[Constants.FVER_PARAM_STAGGERED_MAXAGE]
							?: "",
						onValueChange = {
							onVersioningChange(
								types[typeIndex],
								Constants.FVER_PARAM_STAGGERED_MAXAGE,
								it
							)
						},
						label = { Text(Constants.FVER_PARAM_STAGGERED_MAXAGE) }
					)
					Text(stringResource(R.string.maximum_age_description))
				}

				Constants.FVER_TYPE_EXTERNAL -> {
					OutlinedTextField(
						value = editedVersioning?.params[Constants.FVER_PARAM_EXTERNAL_COMMAND]
							?: "",
						onValueChange = {
							onVersioningChange(
								types[typeIndex],
								Constants.FVER_PARAM_EXTERNAL_COMMAND,
								it
							)
						},
						label = { Text(Constants.FVER_PARAM_EXTERNAL_COMMAND) }
					)
					Text(stringResource(R.string.external_versioning_description))
				}
			}
		}
	}
}


@SuppressLint("UnrememberedMutableState")
@Composable
@Preview(showSystemUi = true, showBackground = true, uiMode = ThemeControls.UI_MODE)
fun FolderScreenPreview() {
	SyncthingandroidTheme(ThemeControls.useDarkMode, dynamicColor = ThemeControls.isMonetEnabled) {
		FolderScreen(
			state = FolderUiState(
				folder = Folder(),
				path = TextFieldState("/storage/emulated/0"),
				isCreateMode = true,
				isValidFolder = true,
				deviceList = mutableStateListOf(),
				folderType = emptyList(),
				folderPullOrders = emptyList(),
				editedVersioning = null,
				showDeleteDialog = false,
				showDiscardDialog = false,
				showFolderTypeDialog = false,
				showFolderPullOrderDialog = false,
				showVersioningDialog = false,
			),
			actions = FolderActions()
		)
	}
}