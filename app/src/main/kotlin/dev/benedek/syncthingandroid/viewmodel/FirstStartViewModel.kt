package dev.benedek.syncthingandroid.viewmodel

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.activities.FirstStartActivity
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.util.PermissionUtil
import dev.benedek.syncthingandroid.util.PermissionUtil.shouldAskForBatteryOptimization
import dev.benedek.syncthingandroid.util.PermissionUtil.shouldAskForLocationPermission
import dev.benedek.syncthingandroid.util.PermissionUtil.shouldAskForNotificationPermission

class FirstStartViewModel(
	context: Context,
	prefs: SharedPreferences
) : ViewModel() {
	var isStorageGranted by mutableStateOf(false)
		private set
	var isLocationGranted by mutableStateOf(false)
		private set
	var isNotificationGranted by mutableStateOf(false)
		private set
	var isApiUpgraded by mutableStateOf(false)
		private set
	var isBatteryOptimizationIgnoreGranted by mutableStateOf(false)
		private set

	var slides by mutableStateOf<List<FirstStartActivity.Slide>>(emptyList())
		private set

	init {
		updatePermissions(context)
		initApiUpgradeState(prefs)
		slides = FirstStartActivity.Slide.entries.filter { !shouldSkip(it, context, prefs) }
	}

	fun updatePermissions(context: Context) {
		isStorageGranted = PermissionUtil.haveStoragePermission(context)
		isLocationGranted = PermissionUtil.hasLocationPermissions(context)
		isNotificationGranted = PermissionUtil.hasNotificationPermission(context)
		isBatteryOptimizationIgnoreGranted = PermissionUtil.hasBatteryOptimizationIgnoreGranted(context)
	}

	fun initApiUpgradeState(prefs: SharedPreferences) {
		isApiUpgraded = prefs.getBoolean(Constants.PREF_UPGRADED_TO_API_LEVEL_30, false) ||
				prefs.getBoolean(Constants.PREF_FIRST_START, true)
	}

	fun onUpgradeDatabase(activity: FirstStartActivity) {
		activity.performApi30Upgrade()
		isApiUpgraded = true
	}

	/**
	 * Navigation Logic
	 */
	fun canAdvance(currentSlide: FirstStartActivity.Slide, context: Context, noToast: Boolean = false): Boolean {
		return when (currentSlide) {
			FirstStartActivity.Slide.STORAGE -> {
				if (!isStorageGranted && !noToast) {
					Toast.makeText(
						context,
						R.string.toast_write_storage_permission_required,
						Toast.LENGTH_LONG
					).show()
				}
				isStorageGranted
			}

			FirstStartActivity.Slide.API_LEVEL_30 -> {
				if (!isApiUpgraded && !noToast) {
					Toast.makeText(
						context,
						R.string.toast_api_level_30_must_reset,
						Toast.LENGTH_LONG
					).show()
				}
				isApiUpgraded
			}

			else -> true
		}
	}

	/**
	 * Skip Logic
	 */
	fun shouldSkip(slide: FirstStartActivity.Slide, context: Context, prefs: SharedPreferences): Boolean {
		return when (slide) {
			FirstStartActivity.Slide.INTRO -> !prefs.getBoolean(Constants.PREF_FIRST_START, true)
			FirstStartActivity.Slide.STORAGE -> isStorageGranted
			FirstStartActivity.Slide.LOCATION -> !shouldAskForLocationPermission(context)
			FirstStartActivity.Slide.API_LEVEL_30 -> {
				val isRoot = prefs.getBoolean(Constants.PREF_USE_ROOT, false)
				isApiUpgraded || isRoot
			}

			FirstStartActivity.Slide.NOTIFICATION -> {
				if (Build.VERSION.SDK_INT < 33) true
				else !shouldAskForNotificationPermission(context)
			}

			FirstStartActivity.Slide.BATTERY ->  !shouldAskForBatteryOptimization(context)
		}
	}
}
