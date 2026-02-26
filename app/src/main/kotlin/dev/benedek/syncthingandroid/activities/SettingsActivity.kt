package dev.benedek.syncthingandroid.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                AppScaffold(
                    topAppBarTitle = when(currentRoute) {
                        "theme" ->              stringResource(R.string.preference_theme_title)
                        "run_conditions" ->     stringResource(R.string.category_run_conditions)
                        "behaviour" ->          stringResource(R.string.category_behaviour)
                        "syncthing_options" ->  stringResource(R.string.category_syncthing_options)
                        "backup" ->             stringResource(R.string.category_backup)
                        "debug" ->              stringResource(R.string.category_debug)
                        "experimental" ->       stringResource(R.string.category_experimental)
                        "about" ->              stringResource(R.string.category_about)
                        else ->                 stringResource(R.string.settings_title)
                    },
                    topNavigationActive = true,
                    topNavigationOnClick = { onBackPressedDispatcher.onBackPressed() }
                ) { innerPadding ->
                    Settings(viewModel, innerPadding, navController)
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
