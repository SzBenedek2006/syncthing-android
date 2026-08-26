package dev.benedek.syncthingandroid.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.activities.FirstStartActivity
import dev.benedek.syncthingandroid.model.Slide
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.util.PermissionUtil
import dev.benedek.syncthingandroid.util.PermissionUtil.shouldAskForBatteryOptimization
import dev.benedek.syncthingandroid.util.PermissionUtil.shouldAskForLocationPermission
import dev.benedek.syncthingandroid.util.PermissionUtil.shouldAskForNotificationPermission
import dev.benedek.syncthingandroid.util.atMostSdk

class FirstStartViewModel(
	context: Context,
	sharedPreferences: SharedPreferences,
	savedStateHandle: SavedStateHandle
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

	var slides by mutableStateOf<List<Slide>>(emptyList())
		private set
	var savedSlides: List<Slide>? by savedStateHandle.saved { null }

	init {
		updatePermissions(context)
		initApiUpgradeState(sharedPreferences)
		if (savedSlides != null) {
			slides = savedSlides!!
		} else {
			savedSlides = Slide.entries.filter { !shouldSkip(it, context, sharedPreferences) }
			slides = savedSlides!!
		}

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
	fun canAdvance(currentSlide: Slide, context: Context, noToast: Boolean = false): Boolean {
		return when (currentSlide) {
			Slide.STORAGE -> {
				if (!isStorageGranted && !noToast) {
					Toast.makeText(
						context,
						R.string.toast_write_storage_permission_required,
						Toast.LENGTH_LONG
					).show()
				}
				isStorageGranted
			}

			Slide.API_LEVEL_30 -> {
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
	fun shouldSkip(slide: Slide, context: Context, prefs: SharedPreferences): Boolean {
		return when (slide) {
			Slide.INTRO -> !prefs.getBoolean(Constants.PREF_FIRST_START, true)
			Slide.STORAGE -> isStorageGranted
			Slide.LOCATION -> !shouldAskForLocationPermission(context)
			Slide.API_LEVEL_30 -> {
				val isRoot = prefs.getBoolean(Constants.PREF_USE_ROOT, false)
				isApiUpgraded || isRoot
			}

			Slide.NOTIFICATION -> {
				atMostSdk(32, { true }) {
					!shouldAskForNotificationPermission(context)
				}
			}

			Slide.BATTERY ->  !shouldAskForBatteryOptimization(context)
		}
	}
}
