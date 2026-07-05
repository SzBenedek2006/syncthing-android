package dev.benedek.syncthingandroid.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.activities.FirstStartActivity
import dev.benedek.syncthingandroid.ui.reusable.ButtonSmallTokens
import dev.benedek.syncthingandroid.ui.reusable.OutlinedButtonTokens
import dev.benedek.syncthingandroid.ui.reusable.value
import dev.benedek.syncthingandroid.ui.slides.ApiUpgradeSlide
import dev.benedek.syncthingandroid.ui.slides.BatterySlide
import dev.benedek.syncthingandroid.ui.slides.IntroSlide
import dev.benedek.syncthingandroid.ui.slides.LocationSlide
import dev.benedek.syncthingandroid.ui.slides.NotificationSlide
import dev.benedek.syncthingandroid.ui.slides.StorageSlide
import dev.benedek.syncthingandroid.util.PermissionUtil
import dev.benedek.syncthingandroid.viewmodel.FirstStartViewModel
import kotlinx.coroutines.launch

val LocalIsLandscape = compositionLocalOf { false }
private const val TAG = "FirstStart.kt"

@Composable
fun FirstStartScreen(
	onFinish: () -> Unit,
	prefs: SharedPreferences,
	activity: FirstStartActivity,
) {
	val context = LocalContext.current
	val viewModel: FirstStartViewModel = viewModel(
		factory = viewModelFactory {
			initializer {
				// Use applicationContext to avoid passing Activity context leaks down to the VM
				FirstStartViewModel(context.applicationContext, prefs)
			}
		}
	)

	val scope = rememberCoroutineScope()
	val slides = viewModel.slides
	val lifecycleOwner = LocalLifecycleOwner.current


	// Setup Pager
	val pagerState = rememberPagerState(pageCount = { slides.size })


	DisposableEffect(lifecycleOwner) {
		val observer = LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_RESUME) {
				viewModel.updatePermissions(context)
			}
		}
		lifecycleOwner.lifecycle.addObserver(observer)
		onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
	}


	// Launcher
	val permissionLauncher = rememberLauncherForActivityResult(
		ActivityResultContracts.RequestMultiplePermissions(),
	) {
		viewModel.updatePermissions(context)
	}


	fun onNext() {
		if (pagerState.currentPage >= slides.size) return
		if (!viewModel.canAdvance(slides[pagerState.currentPage], context)) return

		scope.launch {
			val nextIndex = pagerState.currentPage + 1

			if (nextIndex >= slides.size) {
				onFinish()
			} else {
				pagerState.animateScrollToPage(nextIndex)
			}
		}
	}

	fun onBack() {
		val prevIndex = pagerState.currentPage - 1
		if (prevIndex >= 0) {
			scope.launch { pagerState.animateScrollToPage(prevIndex) }
		}
	}

	// Auto-skip Intro if needed on first load
	// No longer needed as we filter slides at init

	Scaffold(
		modifier = Modifier
			.fillMaxSize(),
		bottomBar = {
			BottomControls(
				pagerState = pagerState,
				slideCount = slides.size,
				onBack = ::onBack,
				onNext = ::onNext,
				canAdvance = { noToast ->
					if (pagerState.currentPage < slides.size) {
						viewModel.canAdvance(slides[pagerState.currentPage], context, noToast)
					} else {
						false
					}
				},
				slide = viewModel.slides[pagerState.currentPage],
				modifier = Modifier.safeDrawingPadding()
			)
		}
	) { innerPadding ->
		HorizontalPager(
			state = pagerState,
			modifier = Modifier
				.padding(innerPadding)
				.fillMaxSize(),
			userScrollEnabled = false // Disable swipe
		) { page ->
			SlideContent(
				slide = slides[page],
				activity = activity,
				permissionLauncher = permissionLauncher,
				onUpgradeDatabase = {
					viewModel.onUpgradeDatabase(activity)
				},
				isStorageGranted = viewModel.isStorageGranted,
				isLocationGranted = viewModel.isLocationGranted,
				isNotificationGranted = viewModel.isNotificationGranted,
				isApiUpgraded = viewModel.isApiUpgraded,
				isBatteryOptimizationIgnoreGranted = viewModel.isBatteryOptimizationIgnoreGranted,
				askForIgnoreBatteryOptimization = activity::askForIgnoreBatteryOptimization,
				denyIgnoreBatteryOptimization = { activity.denyIgnoreBatteryOptimization(); onNext() },
				denyNotificationAccess = { activity.denyNotificationAccess(); onNext() },
				denyLocationAccess = { activity.denyLocationAccess(); onNext() }
			)
		}
	}
}

@Composable
fun BottomControls(
	pagerState: PagerState,
	slideCount: Int,
	onBack: () -> Unit,
	onNext: () -> Unit,
	canAdvance: (noToast: Boolean) -> Boolean,
	slide: FirstStartActivity.Slide,
	modifier: Modifier = Modifier
) {
	val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()

	// TODO: Low priority: move this into function, then use that in every composable that needs it.
	// Detect small screen
	val config = LocalWindowInfo.current.containerDpSize
	val isSmallScreen = config.width < 360.dp

	Row(
		modifier = modifier
			.fillMaxWidth()
			.padding(
				horizontal = if (isSmallScreen) 4.dp else 16.dp,
				vertical = if (isSmallScreen) 4.dp else 8.dp
			),
		horizontalArrangement = Arrangement.SpaceBetween,
		verticalAlignment = Alignment.CenterVertically
	) {
		// Back Button
		Box(
			modifier = Modifier.weight(1f),
			contentAlignment = Alignment.CenterStart
		) {

			TextButton(
				onClick = onBack,
				enabled = pagerState.currentPage > 0,
			) {
				if (isSmallScreen) {
					Icon(
						Icons.AutoMirrored.Filled.ArrowBack,
						contentDescription = stringResource(R.string.back)
					)
				} else {
					Text(
						text = stringResource(R.string.back),
						color = if (pagerState.currentPage == 0) Color.Transparent else MaterialTheme.colorScheme.primary,
					)
				}

			}
		}


		// Dots
		Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
			repeat(slideCount) { iteration ->
				val color = if (pagerState.currentPage == iteration)
					MaterialTheme.colorScheme.primary
				else
					MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

				Box(
					modifier = Modifier
						.size(10.dp)
						.background(color, CircleShape)
				)
			}
		}

		// Next/Finish Button
		Box(
			modifier = Modifier.weight(1f),
			contentAlignment = Alignment.CenterEnd
		) {
			if (isOptional(slide) && !isAccepted(slide, LocalContext.current)) {
				OutlinedButton(
					onClick = onNext,
					// TODO: Look into if this could be done without lifecycleState checking
					// This is to update the composable state
					enabled = lifecycleState.isAtLeast(Lifecycle.State.RESUMED) && canAdvance(true),
					colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
					border = BorderStroke(
						width = ButtonSmallTokens.OutlinedOutlineWidth,
						color = MaterialTheme.colorScheme.primary.copy(
							alpha = OutlinedButtonTokens.DisabledContainerOpacity
						)
					),
				) {
					if (isSmallScreen) {
						Icon(
							imageVector = if (pagerState.currentPage == slideCount - 1) Icons.Filled.Check else Icons.AutoMirrored.Filled.ArrowForward,
							contentDescription = stringResource(
								R.string.dialog_disable_battery_optimization_later
							)
						)
					} else {
						Text(
							text = stringResource(
								R.string.dialog_disable_battery_optimization_later
							)
						)
					}
				}
			} else {
				Button(
					onClick = onNext,
					// TODO: Look into if this could be done without lifecycleState checking
					// This is to update the composable state
					enabled = lifecycleState.isAtLeast(Lifecycle.State.RESUMED) && canAdvance(true),
				) {
					if (isSmallScreen) {
						Icon(
							imageVector = if (pagerState.currentPage == slideCount - 1) Icons.Filled.Check else Icons.AutoMirrored.Filled.ArrowForward,
							contentDescription = stringResource(
								if (pagerState.currentPage == slideCount - 1) R.string.finish else R.string.cont
							)
						)
					} else {
						Text(
							text = stringResource(
								if (pagerState.currentPage == slideCount - 1) R.string.finish else R.string.cont
							)
						)
					}
				}
			}
		}

	}
}


@Composable
fun SlideContent(
	slide: FirstStartActivity.Slide,
	activity: FirstStartActivity,
	permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
	onUpgradeDatabase: () -> Unit,
	isStorageGranted: Boolean,
	isLocationGranted: Boolean,
	isNotificationGranted: Boolean,
	isApiUpgraded: Boolean,
	isBatteryOptimizationIgnoreGranted: Boolean,
	askForIgnoreBatteryOptimization: () -> Unit,
	denyIgnoreBatteryOptimization: () -> Unit,
	denyNotificationAccess: () -> Unit,
	denyLocationAccess: () -> Unit
) {
	val context = LocalContext.current

	fun askForStoragePermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			// Android 11+ All Files Access
			try {
				val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
				intent.data = "package:${context.packageName}".toUri()
				activity.startActivity(intent)
			} catch (_: Exception) {
				Toast.makeText(
					context,
					R.string.dialog_all_files_access_not_supported,
					Toast.LENGTH_LONG
				).show()

				// TODO: Low priority: Test this
				Toast.makeText(
					context,
					"EXPERIMENTAL: Launching old write external storage permission instead.",
					Toast.LENGTH_LONG
				).show()
				permissionLauncher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
			}
		} else {
			// Android 10 and below
			permissionLauncher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
		}
	}

	fun askForLocationPermission() {
		// TODO: Implement one click background location permission logic, similar to askForStoragePermission() if needed
		permissionLauncher.launch(PermissionUtil.locationPermissions)
	}

	fun askForNotificationPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // SDK 33
			permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
		}
	}





	Column(
		modifier = Modifier
			.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		when (slide) {
			FirstStartActivity.Slide.INTRO -> {
				IntroSlide()
			}

			FirstStartActivity.Slide.STORAGE -> {
				StorageSlide(
					::askForStoragePermission,
					isStorageGranted
				)
			}

			FirstStartActivity.Slide.LOCATION -> {
				LocationSlide(
					::askForLocationPermission,
					isLocationGranted,
					denyLocationAccess
				)
			}

			FirstStartActivity.Slide.API_LEVEL_30 -> {
				ApiUpgradeSlide(
					onUpgradeDatabase,
					isApiUpgraded
				)
			}

			FirstStartActivity.Slide.NOTIFICATION -> {
				NotificationSlide(
					::askForNotificationPermission,
					isNotificationGranted,
					denyNotificationAccess
				)
			}

			FirstStartActivity.Slide.BATTERY -> {
				BatterySlide(
					askForIgnoreBatteryOptimization,
					isBatteryOptimizationIgnoreGranted,
					denyIgnoreBatteryOptimization
				)
			}
		}
	}
}

fun isOptional(slide: FirstStartActivity.Slide): Boolean {
	return when (slide) {
		FirstStartActivity.Slide.LOCATION -> true
		FirstStartActivity.Slide.NOTIFICATION -> true
		FirstStartActivity.Slide.BATTERY -> true
		else -> false
	}
}

/**
 * Return `true` if the permission for the slide is granted. `false` otherwise.
 */
fun isAccepted(slide: FirstStartActivity.Slide, context: Context): Boolean {
	return when (slide) {
		FirstStartActivity.Slide.LOCATION -> PermissionUtil.hasLocationPermissions(context)
		FirstStartActivity.Slide.NOTIFICATION -> PermissionUtil.hasNotificationPermission(context)
		FirstStartActivity.Slide.BATTERY -> PermissionUtil.hasBatteryOptimizationIgnoreGranted(context)
		FirstStartActivity.Slide.STORAGE -> PermissionUtil.haveStoragePermission(context)
		else -> false
	}
}