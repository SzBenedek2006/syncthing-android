package dev.benedek.syncthingandroid.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.model.Folder
import dev.benedek.syncthingandroid.ui.Folder
import dev.benedek.syncthingandroid.ui.FolderViewModel
import dev.benedek.syncthingandroid.ui.FolderViewModel.Companion.EXTRA_DEVICE_ID
import dev.benedek.syncthingandroid.ui.FolderViewModel.Companion.EXTRA_FOLDER_ID
import dev.benedek.syncthingandroid.ui.FolderViewModel.Companion.EXTRA_FOLDER_LABEL
import dev.benedek.syncthingandroid.ui.FolderViewModel.Companion.EXTRA_IS_CREATE
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls


class FolderActivity : SyncthingActivity(), SyncthingActivity.OnServiceConnectedListener {

    private val viewModel: FolderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register to get the Service connection callback
        registerOnServiceConnectedListener(this)

        setContent {
            SyncthingandroidTheme(dynamicColor = ThemeControls.useDynamicColor) {
                Folder(
                    viewModel = viewModel,
                    onFinish = { finish() } // Pass the function to close the activity
                )
            }
        }

    }

    override fun onServiceConnected() {
        val service = service ?: return

        viewModel.setService(
            boundService = service,
        )
        viewModel.setInitialState(
            context = this,
            isCreate = intent.getBooleanExtra(EXTRA_IS_CREATE, false),
            folderId = intent.getStringExtra(EXTRA_FOLDER_ID),
            deviceId = intent.getStringExtra(EXTRA_DEVICE_ID),
            folderLabel = intent.getStringExtra(EXTRA_FOLDER_LABEL),
        )
    }

}