package dev.benedek.syncthingandroid.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.ui.Settings
import dev.benedek.syncthingandroid.ui.SettingsViewModel
import dev.benedek.syncthingandroid.ui.reusable.AppScaffold
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls
import dev.benedek.syncthingandroid.util.Util
import me.zhanghai.compose.preference.isDefaultPreferenceFlowLongSupportEnabled

class SettingsActivity : SyncthingActivity(), SyncthingActivity.OnServiceConnectedListener {

    val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        registerOnServiceConnectedListener(this)


        isDefaultPreferenceFlowLongSupportEnabled = true

        setContent {
            SyncthingandroidTheme(
                dynamicColor = ThemeControls.useDynamicColor
            ) {
                AppScaffold(topAppBarTitle = stringResource(R.string.settings_title)) { innerPadding ->
                    Settings(viewModel, innerPadding)
                }

            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.PermissionRequestType.LOCATION.ordinal) {
            var granted = grantResults.isNotEmpty()
            for (i in grantResults.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    granted = false
                    break
                }
            }
            if (granted) {
                this.startService(
                    Intent(this, SyncthingService::class.java)
                        .setAction(SyncthingService.ACTION_REFRESH_NETWORK_INFO)
                )
            } else {
                Util.getAlertDialogBuilder(this)
                    .setTitle(R.string.sync_only_wifi_ssids_location_permission_rejected_dialog_title)
                    .setMessage(R.string.sync_only_wifi_ssids_location_permission_rejected_dialog_content)
                    .setPositiveButton(android.R.string.ok, null).show()
            }
        }
    }


    override fun onServiceConnected() {
        if (service != null) {
            viewModel.setService(service)
        }
    }

    companion object {
        const val EXTRA_OPEN_SUB_PREF_SCREEN: String =
            "activities.syncthingandroid.benedek.dev.SettingsActivity.OPEN_SUB_PREF_SCREEN"
    }


}
