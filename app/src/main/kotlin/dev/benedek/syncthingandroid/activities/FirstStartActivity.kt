package dev.benedek.syncthingandroid.activities

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.preference.PreferenceManager
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.ui.FirstStartScreen
import dev.benedek.syncthingandroid.ui.LocalIsLandscape
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.PermissionUtil
import dev.benedek.syncthingandroid.util.ThemeControls
import dev.benedek.syncthingandroid.util.Util
import java.io.File

class FirstStartActivity : ComponentActivity() {
	enum class Slide {
		INTRO,
		STORAGE,
		LOCATION,
		API_LEVEL_30,
		NOTIFICATION
	}


	private val preferences: SharedPreferences by lazy {
		PreferenceManager.getDefaultSharedPreferences(this)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		val splashScreen = installSplashScreen()
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
		/**
		 * Recheck storage permission. If it has been revoked after the user
		 * completed the welcome slides, displays the slides again.
		 */
		if (!isFirstStart() && PermissionUtil.haveStoragePermission(this) && upgradedToApiLevel30()) {
			startApp()
			return
		} else {
			preferences.edit { putBoolean(Constants.PREF_UPGRADED_TO_API_LEVEL_30, true) }
		}

		setContent {
			val windowInfo = LocalWindowInfo.current
			val isLandscape = windowInfo.containerSize.width > windowInfo.containerSize.height

			SyncthingandroidTheme(
				dynamicColor = ThemeControls.isMonetEnabled
			) {
				CompositionLocalProvider(
					LocalIsLandscape provides isLandscape
				) {
					FirstStartScreen(
						onFinish = {
							preferences.edit { putBoolean(Constants.PREF_FIRST_START, false) }
							startApp()
						},
						prefs = preferences,
						activity = this
					)
				}
			}
		}
	}

	fun isFirstStart(): Boolean {
		return preferences.getBoolean(Constants.PREF_FIRST_START, true)
	}

	fun upgradedToApiLevel30(): Boolean {
		if (preferences.getBoolean(Constants.PREF_UPGRADED_TO_API_LEVEL_30, false)) {
			return true
		}
		if (isFirstStart()) {
			return true
		}
		return false
	}


	private fun startApp() {
		val doInitialKeyGeneration = !Constants.getConfigFile(this).exists()
		val mainIntent = Intent(this, MainActivity::class.java)
		mainIntent.putExtra(MainActivity.EXTRA_KEY_GENERATION_IN_PROGRESS, doInitialKeyGeneration)
		/**
		 * In case start_into_web_gui option is enabled, start both activities
		 * so that back navigation works as expected.
		 */
		if (preferences.getBoolean(Constants.PREF_START_INTO_WEB_GUI, false)) {
			startActivities(arrayOf(mainIntent, Intent(this, WebGuiActivity::class.java)))
		} else {
			startActivity(mainIntent)
		}
		finish()
	}


	fun performApi30Upgrade() {
		val dbDir = File(filesDir, "index-v0.14.0.db")
		if (dbDir.exists()) {
			try {
				dbDir.deleteRecursively()
			} catch (e: Throwable) {
				Log.w("FirstStart", "Deleting database with Kotlin failed", e)
				Util.runShellCommand("rm -r " + dbDir.absolutePath, false)
				if (dbDir.exists()) {
					throw RuntimeException("Failed to delete existing database")
				}
			}
		}
		preferences.edit { putBoolean(Constants.PREF_UPGRADED_TO_API_LEVEL_30, true) }
	}
}
