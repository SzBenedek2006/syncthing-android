package dev.benedek.syncthingandroid.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.toColorInt
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.ui.reusable.AppScaffold
import dev.benedek.syncthingandroid.ui.settings.Settings
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls
import dev.benedek.syncthingandroid.util.Util
import dev.benedek.syncthingandroid.viewmodel.SettingsViewModel
import me.zhanghai.compose.preference.isDefaultPreferenceFlowAndroidLongSupportEnabled

class SettingsActivity : SyncthingActivity(), SyncthingActivity.OnServiceConnectedListener {

	val viewModel: SettingsViewModel by viewModels()

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
		registerOnServiceConnectedListener(this)



		isDefaultPreferenceFlowAndroidLongSupportEnabled = true

		setContent {
			SyncthingandroidTheme(
				dynamicColor = ThemeControls.isMonetEnabled,
				darkTheme = ThemeControls.useDarkMode
			) {
				val navController = rememberNavController()
				val navBackStackEntry by navController.currentBackStackEntryAsState()
				val currentRoute = navBackStackEntry?.destination?.route
				var page: String? by rememberSaveable { mutableStateOf(intent.getStringExtra(EXTRA_OPEN_SUB_PREF_SCREEN)) }


				LaunchedEffect(Unit) {
					page?.let { navController.navigate(it) }
					page = null
				}

				AppScaffold(
					topAppBarTitle = when (currentRoute) {
						PREF_CATEGORY_THEME -> stringResource(R.string.preference_theme_title)
						PREF_CATEGORY_RUN_CONDITIONS -> stringResource(R.string.category_run_conditions)
						PREF_CATEGORY_BEHAVIOUR -> stringResource(R.string.category_behaviour)
						PREF_CATEGORY_SYNCTHING_OPTIONS -> stringResource(R.string.category_syncthing_options)
						PREF_CATEGORY_BACKUP -> stringResource(R.string.category_backup)
						PREF_CATEGORY_DEBUG -> stringResource(R.string.category_debug)
						PREF_CATEGORY_EXPERIMENTAL -> stringResource(R.string.category_experimental)
						PREF_CATEGORY_ABOUT -> stringResource(R.string.category_about)
						else -> stringResource(R.string.settings_title)
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
		service?.let { viewModel.setService(it) }
	}

	companion object {
		const val EXTRA_OPEN_SUB_PREF_SCREEN: String =
			"activities.syncthingandroid.benedek.dev.SettingsActivity.OPEN_SUB_PREF_SCREEN"

		/**
		 * Navigation destinations
		 */
		const val PREF_SETTINGS: String = "settings_root"
		const val PREF_CATEGORY_THEME: String = "category_theme"
		const val PREF_CATEGORY_RUN_CONDITIONS: String = "category_run_conditions"
		const val PREF_CATEGORY_BEHAVIOUR: String = "category_behaviour"
		const val PREF_CATEGORY_SYNCTHING_OPTIONS: String = "category_syncthing_options"
		const val PREF_CATEGORY_BACKUP: String = "category_backup"
		const val PREF_CATEGORY_DEBUG: String = "category_debug"
		const val PREF_CATEGORY_EXPERIMENTAL: String = "category_experimental"
		const val PREF_CATEGORY_ABOUT: String = "category_about"
	}


}
