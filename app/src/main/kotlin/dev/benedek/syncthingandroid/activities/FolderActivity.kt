package dev.benedek.syncthingandroid.activities

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.graphics.toColorInt
import dev.benedek.syncthingandroid.ui.FolderActions
import dev.benedek.syncthingandroid.ui.FolderScreen
import dev.benedek.syncthingandroid.ui.FolderUiState
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls
import dev.benedek.syncthingandroid.util.Util.logD
import dev.benedek.syncthingandroid.viewmodel.FolderViewModel
import dev.benedek.syncthingandroid.viewmodel.FolderViewModel.Companion.EXTRA_DEVICE_ID
import dev.benedek.syncthingandroid.viewmodel.FolderViewModel.Companion.EXTRA_FOLDER_ID
import dev.benedek.syncthingandroid.viewmodel.FolderViewModel.Companion.EXTRA_FOLDER_LABEL
import dev.benedek.syncthingandroid.viewmodel.FolderViewModel.Companion.EXTRA_IS_CREATE


class FolderActivity : SyncthingActivity(), SyncthingActivity.OnServiceConnectedListener {

	private val viewModel: FolderViewModel by viewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

		enableEdgeToEdge(
			navigationBarStyle = if (
				ThemeControls.useDarkMode == true ||
				(ThemeControls.useDarkMode == null && currentNightMode == Configuration.UI_MODE_NIGHT_YES)
			) {
				SystemBarStyle.dark("#00000000".toColorInt())
			} else {
				SystemBarStyle.light(
					"#00000000".toColorInt(),
					"#801b1b1b".toColorInt()
				)
			}
		)

		// Register to get the Service connection callback
		registerOnServiceConnectedListener(this)

		setContent {
			SyncthingandroidTheme(dynamicColor = ThemeControls.isMonetEnabled) {
				FolderScreen(
					state = FolderUiState(
						folder = viewModel.folder,
						path = viewModel.pathTextFieldState,
						isCreateMode = viewModel.isCreateMode,
						isValidFolder = logD(viewModel.isValidFolder),
						isPathWritable = viewModel.isPathWritable,
						deviceList = viewModel.deviceList,
						folderType = viewModel.folderType,
						folderPullOrders = viewModel.folderPullOrders,
						editedVersioning = viewModel.editedVersioning,
						showDeleteDialog = viewModel.showDeleteDialog,
						showDiscardDialog = viewModel.showDiscardDialog,
						showFolderTypeDialog = viewModel.showFolderTypeDialog,
						showFolderPullOrderDialog = viewModel.showFolderPullOrderDialog,
						showVersioningDialog = viewModel.showVersioningDialog,
						folderNeedsToUpdate = viewModel.folderNeedsToUpdate
					),
					actions = FolderActions(
						onFinish = { finish() },
						onDone = viewModel::onDone,
						onCancel = viewModel::onCancel,
						onDelete = viewModel::onDelete,
						onFolderSelectedViaSaf = viewModel::onFolderSelectedViaSaf,
						onPathChange = viewModel::onPathChange,
						setShowDeleteDialog = { viewModel.showDeleteDialog = it },
						setShowDiscardDialog = { viewModel.showDiscardDialog = it },
						setShowFolderTypeDialog = { viewModel.showFolderTypeDialog = it },
						onPausedChange = viewModel::onPausedChange,
						onLabelChange = viewModel::onLabelChange,
						onIdChange = viewModel::onIdChange,
						onDeviceSelectionChange = viewModel::onDeviceSelectionChange,
						setShowVersioningDialog = { viewModel.showVersioningDialog = it },
						onFsWatcherChange = viewModel::onFsWatcherChange,
						editIgnores = viewModel::editIgnores,
						onFolderTypeChange = viewModel::onFolderTypeChange,
						setShowFolderPullOrderDialog = { viewModel.showFolderPullOrderDialog = it },
						setEditedVersioning = { viewModel.editedVersioning = it },
						onVersioningSave = viewModel::onVersioningSave,
						onVersioningChange = viewModel::onVersioningChange,
						onPullOrderChange = viewModel::onPullOrderChange,
						onPickerReturned = viewModel::onPickerReturned
					)
				)
			}
		}

	}

	override fun onServiceConnected() {
		val service = service ?: return

		viewModel.setService(
			service = service,
		)
		viewModel.setInitialState(
			context = this,
			onFinish = this::finish,
			isCreate = intent.getBooleanExtra(EXTRA_IS_CREATE, false),
			folderId = intent.getStringExtra(EXTRA_FOLDER_ID),
			newDeviceId = intent.getStringExtra(EXTRA_DEVICE_ID),
			folderLabel = intent.getStringExtra(EXTRA_FOLDER_LABEL),
		)
	}

}