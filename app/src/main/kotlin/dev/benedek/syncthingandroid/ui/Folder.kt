package dev.benedek.syncthingandroid.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.model.Device
import dev.benedek.syncthingandroid.model.isValid
import dev.benedek.syncthingandroid.model.isValidDefault
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.ui.reusable.AppDropDownMenu
import dev.benedek.syncthingandroid.ui.reusable.AppScaffold
import dev.benedek.syncthingandroid.ui.reusable.AppTextField
import dev.benedek.syncthingandroid.ui.reusable.CustomDialog
import dev.benedek.syncthingandroid.ui.reusable.HorizontalDivider
import dev.benedek.syncthingandroid.ui.reusable.OptionTile
import dev.benedek.syncthingandroid.ui.reusable.SingleSelectDialog
import dev.benedek.syncthingandroid.util.FileUtils
import java.io.File
import kotlin.collections.forEachIndexed


@Composable
fun Folder(
    viewModel: FolderViewModel,
    onFinish: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(
        viewModel.folder.label,
        viewModel.folder.id,
        viewModel.folder.path
    ) {
        val folder = viewModel.folder
        //TODO: Improvement: maybe check if syncthing has access to path
        if (folder.label.isNullOrEmpty() || !folder.id!!.isValid || folder.path.isNullOrEmpty()) {
            viewModel.isValidFolder = false
        } else {
            viewModel.isValidFolder = true
        }
    }

    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onFolderSelectedViaSaf(uri, context)
        }
    }

    AppScaffold(
        topAppBarTitle =
            if (viewModel.isCreateMode) stringResource(R.string.create_folder)
            else stringResource(R.string.edit_folder),
        topActionOnClick = { viewModel.onDone(context, onFinish) },
        topActionActive = viewModel.isValidFolder,
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
                label = R.string.folder_label,
                leadingIconPainter = R.drawable.ic_label_outline_24dp,
                value = viewModel.folder.label ?: "",
                onValueChange = { viewModel.onLabelChange(it) }
            )
            HorizontalDivider()
            AppTextField(
                label = stringResource(R.string.folder_id),
                leadingIconPainter = rememberVectorPainter(Icons.Outlined.VpnKey),
                value = viewModel.folder.id ?: "",
                onValueChange = { viewModel.onIdChange(it) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Text
                ),
                readOnly = !viewModel.isCreateMode
            )
            HorizontalDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppTextField(
                    label = stringResource(R.string.directory),
                    leadingIconPainter = rememberVectorPainter(Icons.Outlined.Folder),
                    value = viewModel.folder.path ?: "",
                    onValueChange = { viewModel.onPathChange(it) },
                    modifier = Modifier.weight(1f),
                    readOnly = !viewModel.isCreateMode
                )
                if (viewModel.isCreateMode) {
                    Button(
                        onClick = {
                            directoryPicker.launch(
                                FileUtils.getPickerInitialUri(
                                    context,
                                    viewModel.folder.path
                                )
                            )
                        },
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            disabledContainerColor = ButtonDefaults.buttonColors().disabledContainerColor,
                            disabledContentColor = ButtonDefaults.buttonColors().disabledContentColor
                            ),
                        modifier = Modifier
                            .height(intrinsicSize = IntrinsicSize.Max)
                    ) {
                        Text(stringResource(R.string.select))
                    }
                }
            }
            HorizontalDivider()
            Column(
                modifier = Modifier
            ) {
                var showItems by remember { mutableStateOf(false) }
                val rotationAmount: Float? = if (showItems) 180f else 0f
                OptionTile(
                    title = stringResource(R.string.devices),
                    leftIconPainter = rememberVectorPainter(Icons.Outlined.Devices),
                    rightIconPainter = rememberVectorPainter(Icons.Outlined.ExpandMore),
                    onClick = {showItems = !showItems},
                    rightIconRotationAmount = rotationAmount,
                    contentColor = if (showItems) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                DeviceListSection(viewModel.deviceList, showItems, viewModel::onDeviceSelectionChange)
            }
            HorizontalDivider()
            OptionTile(
                title = stringResource(R.string.folder_type),
                description = stringResource(
                    viewModel.folderType.find { it.value == viewModel.folder.type }!!.titleRes
                ),
                leftIconPainter = painterResource(R.drawable.folder_managed_24px),
                onClick = { viewModel.showFolderTypeDialog = true }
            )
            HorizontalDivider()

            OptionTile(
                title = stringResource(R.string.folder_fileWatcher),
                description = stringResource(R.string.folder_fileWatcherDescription),
                leftIconPainter = painterResource(R.drawable.sync_eye_24dp),
                checked = viewModel.folder.fsWatcherEnabled,
                onCheckedChange = { viewModel.onFsWatcherChange(!viewModel.folder.fsWatcherEnabled) }
            )
            HorizontalDivider()
            OptionTile(
                title = stringResource(R.string.folder_pause),
                leftIconPainter = rememberVectorPainter(Icons.Outlined.Pause),
                checked = viewModel.folder.paused,
                onCheckedChange = { viewModel.onPausedChange(!viewModel.folder.paused) }
            )
            HorizontalDivider()
            OptionTile(
                title = stringResource(R.string.pull_order),
                description = if (viewModel.folder.order != null) stringResource(
                    viewModel.folderPullOrders.find { it.value == viewModel.folder.order }?.titleRes
                        ?: R.string.pull_order
                ) else stringResource(viewModel.folderPullOrders[0].titleRes),
                leftIconPainter = rememberVectorPainter(Icons.AutoMirrored.Outlined.Sort),
                onClick = { viewModel.showFolderPullOrderDialog = true }
            )
            HorizontalDivider()
            OptionTile(
                title = stringResource(R.string.file_versioning),
                description = if (viewModel.folder.versioning?.type.isNullOrEmpty() || viewModel.folder.versioning == null)
                    Constants.FVER_TYPE_NONE else {
                        viewModel.folder.versioning!!.type +
                                when (viewModel.folder.versioning!!.type) {
                                    Constants.FVER_TYPE_SIMPLE ->
                                        "\n" + Constants.FVER_PARAM_SIMPLE_KEEP + " = " + viewModel.folder.versioning?.params[Constants.FVER_PARAM_SIMPLE_KEEP]

                                    Constants.FVER_TYPE_TRASHCAN ->
                                        "\n" + Constants.FVER_PARAM_TRASHCAN_CLEANDAYS + " = " + viewModel.folder.versioning?.params[Constants.FVER_PARAM_TRASHCAN_CLEANDAYS]

                                    Constants.FVER_TYPE_STAGGERED ->
                                        "\n" + Constants.FVER_PARAM_STAGGERED_PATH + " = " + viewModel.folder.versioning?.params[Constants.FVER_PARAM_STAGGERED_PATH] + "\n" +
                                                Constants.FVER_PARAM_STAGGERED_MAXAGE + " = " + viewModel.folder.versioning?.params[Constants.FVER_PARAM_STAGGERED_MAXAGE]

                                    Constants.FVER_TYPE_EXTERNAL ->
                                        "\n" + Constants.FVER_PARAM_EXTERNAL_COMMAND + " = " + viewModel.folder.versioning?.params[Constants.FVER_PARAM_EXTERNAL_COMMAND]

                                    else -> ""
                                }
                },
                leftIconPainter = rememberVectorPainter(Icons.Outlined.Archive),
                onClick = { viewModel.showVersioningDialog = true }
            )
            HorizontalDivider()
            OptionTile(
                title = stringResource(R.string.ignore_patterns),
                description = stringResource(R.string.open_stignore_description),
                leftIconPainter = rememberVectorPainter(Icons.Outlined.FilterAlt),
                onClick = { viewModel.editIgnores(context) },
                enabled = if (viewModel.folder.path != null) File(viewModel.folder.path!!).exists() else false
            )
            HorizontalDivider()
            if (!viewModel.isCreateMode) {
                OptionTile(
                    title = stringResource(R.string.delete_folder),
                    description = stringResource(R.string.delete_folder_description),
                    leftIconPainter = rememberVectorPainter(Icons.Outlined.Delete),
                    onClick = { viewModel.showDeleteDialog = true },
                    contentColor = MaterialTheme.colorScheme.error,
                )
                HorizontalDivider()
            }



        }
    }

    @Composable
    fun DiscardDialog(
        onOk: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        AlertDialog(
            onDismissRequest = onCancel ?: { viewModel.showDiscardDialog = false },
            confirmButton = {
                TextButton(
                    onClick = onOk ?: onFinish
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onCancel ?: { viewModel.showDiscardDialog = false }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = { Text(stringResource(R.string.dialog_discard_changes)) }
        )
    }
    @Composable
    fun DeleteDialog(
        onOk: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        AlertDialog(
            onDismissRequest = onCancel ?: { viewModel.showDeleteDialog = false },
            confirmButton = {
                TextButton(
                    onClick = onOk
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onCancel ?: { viewModel.showDeleteDialog = false }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.delete_folder)) },
            text = { Text(stringResource(R.string.delete_folder_description)) }
        )
    }

    if (viewModel.showDiscardDialog) {
        DiscardDialog()
    }

    if (viewModel.showFolderTypeDialog) {
        SingleSelectDialog(
            title = stringResource(R.string.folder_type),
            items = viewModel.folderType.map { stringResource(it.titleRes) },
            initialSelectedIndex = viewModel.folderType.indexOfFirst { it.value == viewModel.folder.type },
            onSelect = { index ->
                viewModel.onFolderTypeChange(viewModel.folderType[index].value)
            },
            onDismiss = { viewModel.showFolderTypeDialog = false }
        )
    }
    if (viewModel.showFolderPullOrderDialog) {
        SingleSelectDialog(
            title = stringResource(R.string.pull_order),
            items = viewModel.folderPullOrders.map { stringResource(it.titleRes) },
            initialSelectedIndex = viewModel.folderPullOrders.indexOfFirst { it.value == viewModel.folder.order },
            onSelect = { index ->
                viewModel.onPullOrderChange(viewModel.folderPullOrders[index].value)
            },
            onDismiss = { viewModel.showFolderPullOrderDialog = false }
        )
    }
    if (viewModel.showVersioningDialog) {

        var typeIndex by remember { mutableIntStateOf(0) }
        typeIndex = if (Constants.FVER_TYPES.indexOf(viewModel.editedVersioning!!.type) == -1) {
            0
        } else {
            Constants.FVER_TYPES.indexOf(viewModel.editedVersioning!!.type)
        }
        VersioningDialog(
            title = stringResource(R.string.file_versioning),
            onDismissRequest = {
                viewModel.editedVersioning = viewModel.folder.versioning!!.deepCopy()
                viewModel.showVersioningDialog = false
            },
            typeIndex = typeIndex,
            viewModel = viewModel,
            onSelectedIndexChange = { index ->
                viewModel.onVersioningChange(Constants.FVER_TYPES[index])
                typeIndex = index
            }
        )
    }
    if (viewModel.showDeleteDialog) {
        DeleteDialog({viewModel.onDelete(onFinish) })
    }
}




@Composable
fun DeviceListSection(
    deviceList: SnapshotStateList<FolderViewModel.DeviceUiState>,
    showItems: Boolean,
    onDeviceChecked: (Device, Boolean) -> Unit
) {
    AnimatedVisibility(
        visible = showItems,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column {
            deviceList.forEachIndexed { _, item ->
                key(item.device.deviceID) {
                    OptionTile(
                        title = item.device.name,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersioningDialog(
    title: String? = null,
    description: String? = null,
    onDismissRequest: () -> Unit,
    typeIndex: Int,
    viewModel: FolderViewModel,
    onSelectedIndexChange: (Int) -> Unit
) {

    val onOk = {
        viewModel.onVersioningSave()
        viewModel.showVersioningDialog = false
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

            AppDropDownMenu(null, Constants.FVER_TYPES, typeIndex, onSelectedIndexChange, focusManager)

            val types = remember { Constants.FVER_TYPES }

            when (types[typeIndex]) {
                Constants.FVER_TYPE_SIMPLE -> {
                    Text(stringResource(R.string.simple_file_versioning_description))
                    OutlinedTextField(
                        value = viewModel.editedVersioning?.params[Constants.FVER_PARAM_SIMPLE_KEEP]
                            ?: "",
                        onValueChange = {
                            viewModel.onVersioningChange(
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
                        value = viewModel.editedVersioning?.params[Constants.FVER_PARAM_TRASHCAN_CLEANDAYS]
                            ?: "",
                        onValueChange = {
                            viewModel.onVersioningChange(
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
                        value = viewModel.editedVersioning?.params[Constants.FVER_PARAM_STAGGERED_PATH]
                            ?: "",
                        onValueChange = {
                            viewModel.onVersioningChange(
                                types[typeIndex],
                                Constants.FVER_PARAM_STAGGERED_PATH,
                                it
                            )
                        },
                        label = { Text(Constants.FVER_PARAM_STAGGERED_PATH) }

                    )
                    Text(stringResource(R.string.versions_path_description))
                    OutlinedTextField(
                        value = viewModel.editedVersioning?.params[Constants.FVER_PARAM_STAGGERED_MAXAGE]
                            ?: "",
                        onValueChange = {
                            viewModel.onVersioningChange(
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
                        value = viewModel.editedVersioning?.params[Constants.FVER_PARAM_EXTERNAL_COMMAND]
                            ?: "",
                        onValueChange = {
                            viewModel.onVersioningChange(
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



@Composable
@Preview(showSystemUi = true, showBackground = true, uiMode = UI_MODE_NIGHT_YES)
fun FolderPreview() {
    MaterialTheme() {
        Folder(FolderViewModel())
    }
}