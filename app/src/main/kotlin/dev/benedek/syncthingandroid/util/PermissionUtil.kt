package dev.benedek.syncthingandroid.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager

object PermissionUtil {
	val locationPermissions: Array<String>
		/**
		 * Returns the location permissions required to access wifi SSIDs depending
		 * on the respective Android version.
		 */
		get() {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) { // before android 9
				return arrayOf(
					Manifest.permission.ACCESS_COARSE_LOCATION,
				)
			}
			return arrayOf(
				Manifest.permission.ACCESS_FINE_LOCATION,
			)
		}

	@JvmStatic
	fun haveStoragePermission(context: Context): Boolean {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			return Environment.isExternalStorageManager()
		}
		val permissionState = ContextCompat.checkSelfPermission(
			context,
			Manifest.permission.WRITE_EXTERNAL_STORAGE
		)
		return permissionState == PackageManager.PERMISSION_GRANTED
	}

	/**
	 * Checks if the required location permissions to obtain WiFi SSID are granted.
	 */
	fun hasLocationPermissions(context: Context): Boolean {
		val perms: Array<String> = locationPermissions
		for (i in perms.indices) {
			if (ContextCompat.checkSelfPermission(
					context,
					perms[i]
				) != PackageManager.PERMISSION_GRANTED
			) {
				return false
			}
		}
		return true
	}


	fun hasNotificationPermission(context: Context): Boolean {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			return ContextCompat.checkSelfPermission(
				context,
				Manifest.permission.POST_NOTIFICATIONS
			) == PackageManager.PERMISSION_GRANTED
		}
		return true
	}

	fun hasBatteryOptimizationIgnoreGranted(context: Context): Boolean {
		val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
		return powerManager.isIgnoringBatteryOptimizations(context.packageName)
	}

	fun shouldAskForBatteryOptimization(context: Context): Boolean {
		val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
		val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
		val dontShowAgain = sharedPreferences.getBoolean(
			"battery_optimization_dont_show_again",
			false
		)
		val isGranted = powerManager.isIgnoringBatteryOptimizations(context.packageName)
		return !dontShowAgain && !isGranted
	}

	fun shouldAskForNotificationPermission(context: Context): Boolean {
		val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
		val dontShowAgain = sharedPreferences.getBoolean("send_notification_permission_dont_show_again", false)
		val isGranted = hasNotificationPermission(context)
		return !isGranted && !dontShowAgain
	}

	fun shouldAskForLocationPermission(context: Context): Boolean {
		val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
		val dontShowAgain = sharedPreferences.getBoolean("location_permission_dont_show_again", false)
		val isGranted = hasLocationPermissions(context)
		return !isGranted && !dontShowAgain
	}
}


