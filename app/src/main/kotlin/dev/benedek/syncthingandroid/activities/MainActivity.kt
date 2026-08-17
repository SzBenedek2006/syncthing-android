package dev.benedek.syncthingandroid.activities

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.service.RestApi
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.ui.main.Main
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.PermissionUtil
import dev.benedek.syncthingandroid.util.ThemeControls
import dev.benedek.syncthingandroid.util.Util
import dev.benedek.syncthingandroid.util.atLeastSdk
import dev.benedek.syncthingandroid.viewmodel.MainViewModel
import java.util.Date
import java.util.concurrent.TimeUnit


class MainActivity : StateDialogActivity() {
	private val viewModel: MainViewModel by viewModels()

	private var drawerToggle: ActionBarDrawerToggle? = null



	private val firstStartTime: Long
		/**
		 * Returns the unix timestamp at which the app was first installed.
		 */
		get() {
			val pm = packageManager
			var firstInstallTime: Long = 0
			try {
				firstInstallTime = pm.getPackageInfo(packageName, 0).firstInstallTime
			} catch (e: PackageManager.NameNotFoundException) {
				Log.wtf(TAG, "This should never happen", e)
			}
			return firstInstallTime
		}


	/**
	 * Initializes tab navigation.
	 */
	public override fun onCreate(savedInstanceState: Bundle?) {
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

		lifecycle.addObserver(viewModel.mainVisibilityObserver)

		setContent {
			SyncthingandroidTheme(dynamicColor = ThemeControls.isMonetEnabled) {
				Main(viewModel, this::doExit)
			}

		}


		// SyncthingService needs to be started from this activity as the user
		// can directly launch this activity from the recent activity switcher.
		val serviceIntent = Intent(this, SyncthingService::class.java)
		atLeastSdk(
			Build.VERSION_CODES.O,
			{ startForegroundService(serviceIntent) },
			{ startService(serviceIntent) }
		)


		onNewIntent(intent)
	}

	public override fun onResume() {
		// Check if storage permission has been revoked at runtime.
		if (!PermissionUtil.haveStoragePermission(this)) {
			startActivity(Intent(this, FirstStartActivity::class.java))
			this.finish()
		}

		// Evaluate run conditions to detect changes made to the metered Wi-Fi flags.
		val syncthingService = service
		syncthingService?.evaluateRunConditions()
		super.onResume()
	}

	public override fun onDestroy() {
		super.onDestroy()
	}

	override fun onServiceConnected(componentName: ComponentName?, iBinder: IBinder?) {
		super.onServiceConnected(componentName, iBinder)

		service?.let {
			viewModel.setService(it)
			it.registerOnServiceStateChangeListener(::onServiceConnected)
		}
		if (service == null) Log.wtf(
			this.toString(),
			"On service connected, service was null (not connected)!"
		)

	}

	/**
	 * TODO: Add back original functionality.
	 */
	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
	}

	override fun onPostCreate(savedInstanceState: Bundle?) {
		super.onPostCreate(savedInstanceState)

		drawerToggle?.syncState()

		val actionBar = supportActionBar
		actionBar?.setHomeButtonEnabled(true)
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		drawerToggle?.onConfigurationChanged(newConfig)
	}

	fun doExit() {
		if (isFinishing) {
			return
		}
		Log.i(TAG, "Exiting app on user request")
		stopService(Intent(this, SyncthingService::class.java))
		finishAndRemoveTask()
	}


	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return drawerToggle?.onOptionsItemSelected(item) == true || super.onOptionsItemSelected(item)
	}


	/**
	 * Displays dialog asking user to accept/deny usage reporting.
	 */
	private fun showUsageReportingDialog(restApi: RestApi) {
		val listener = DialogInterface.OnClickListener { _: DialogInterface?, which: Int ->
			try {
				when (which) {
					DialogInterface.BUTTON_POSITIVE -> {
						restApi.setUsageReporting(true)
						restApi.saveConfigAndRestart()
					}

					DialogInterface.BUTTON_NEGATIVE -> {
						restApi.setUsageReporting(false)
						restApi.saveConfigAndRestart()
					}

					DialogInterface.BUTTON_NEUTRAL -> {
						val uri = "https://data.syncthing.net".toUri()
						startActivity(Intent(Intent.ACTION_VIEW, uri))
					}
				}
			} catch (e: Exception) {
				Log.e(TAG, "showUsageReportingDialog:OnClickListener", e)
			}
		}

		restApi.getUsageReport { report: String? ->
			if (!this@MainActivity.isFinishing && !this@MainActivity.isDestroyed) {
				@SuppressLint("InflateParams")
				val view = LayoutInflater.from(this@MainActivity)
					.inflate(R.layout.dialog_usage_reporting, null)
				val textView = view.findViewById<TextView>(R.id.example)
				textView.text = report

				Util.getAlertDialogBuilder(this@MainActivity)
					.setTitle(R.string.usage_reporting_dialog_title)
					.setView(view)
					.setPositiveButton(R.string.yes, listener)
					.setNegativeButton(R.string.no, listener)
					.setNeutralButton(R.string.open_website, listener)
					.show()
			}
		}
	}

	fun onServiceConnected(state: SyncthingService.State?) {
		if (state == SyncthingService.State.ACTIVE) {
			// Check if the usage reporting minimum delay passed by.
			val usageReportingDelayPassed =
				(Date().time > this.firstStartTime + USAGE_REPORTING_DIALOG_DELAY)
			val restApi = api
			if (usageReportingDelayPassed && restApi != null && !restApi.isUsageReportingDecided) {
				showUsageReportingDialog(restApi)
			}
		}
	}

	companion object {
		private const val TAG = "MainActivity"
		const val EXTRA_KEY_GENERATION_IN_PROGRESS: String =
			"dev.benedek.syncthing-android.SyncthingActivity.KEY_GENERATION_IN_PROGRESS"

		/**
		 * Time after first start when usage reporting dialog should be shown.
		 *
		 * @see .showUsageReportingDialog
		 */
		private val USAGE_REPORTING_DIALOG_DELAY = TimeUnit.DAYS.toMillis(3)
	}
}
