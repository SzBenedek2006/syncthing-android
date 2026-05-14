package dev.benedek.syncthingandroid.activities

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dev.benedek.syncthingandroid.ui.Folder
import dev.benedek.syncthingandroid.ui.FolderViewModel
import dev.benedek.syncthingandroid.ui.FolderViewModel.Companion.EXTRA_DEVICE_ID
import dev.benedek.syncthingandroid.ui.FolderViewModel.Companion.EXTRA_FOLDER_ID
import dev.benedek.syncthingandroid.ui.FolderViewModel.Companion.EXTRA_FOLDER_LABEL
import dev.benedek.syncthingandroid.ui.FolderViewModel.Companion.EXTRA_IS_CREATE
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls
import androidx.core.graphics.toColorInt


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