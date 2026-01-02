package dev.benedek.syncthingandroid.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.activities.FirstStartActivity
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.ui.slides.ApiUpgradeSlide
import dev.benedek.syncthingandroid.ui.slides.IntroSlide
import dev.benedek.syncthingandroid.ui.slides.LocationSlide
import dev.benedek.syncthingandroid.ui.slides.NotificationSlide
import dev.benedek.syncthingandroid.ui.slides.StorageSlide
import dev.benedek.syncthingandroid.util.PermissionUtil
import kotlinx.coroutines.launch

@Composable
fun FirstStartScreen(
    onFinish: () -> Unit,
    prefs: SharedPreferences,
    activity: FirstStartActivity
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val slides = FirstStartActivity.Slide.entries.toTypedArray()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Setup Pager
    val pagerState = rememberPagerState(pageCount = { slides.size })


    var isStorageGranted by remember { mutableStateOf(PermissionUtil.haveStoragePermission(context)) }
    var isLocationGranted by remember { mutableStateOf(PermissionUtil.hasLocationPermissions(context)) }
    var isNotificationGranted by remember { mutableStateOf(PermissionUtil.hasNotificationPermission(context)) }

    var isApiUpgraded by remember {
        mutableStateOf(
            prefs.getBoolean(Constants.PREF_UPGRADED_TO_API_LEVEL_30, false) ||
                    prefs.getBoolean(Constants.PREF_FIRST_START, true)
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isStorageGranted = PermissionUtil.haveStoragePermission(context)
                isLocationGranted = PermissionUtil.hasLocationPermissions(context)
                isNotificationGranted = PermissionUtil.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }


    // Launchers
    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
        ) {
        isStorageGranted = PermissionUtil.haveStoragePermission(context)
    }

    val locationLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            isLocationGranted = result.values.all { it }
        }

    val notificationLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            isNotificationGranted = isGranted
        }

    val currentSlide = slides[pagerState.currentPage]

    // Navigation Logic
    fun canAdvance(noToast: Boolean = false): Boolean {
        return when (currentSlide) {
            FirstStartActivity.Slide.STORAGE -> {
                if (!isStorageGranted && !noToast) {
                    Toast.makeText(context, R.string.toast_write_storage_permission_required, Toast.LENGTH_LONG).show()
                }
                isStorageGranted
            }
            FirstStartActivity.Slide.API_LEVEL_30 -> {
                if (!isApiUpgraded && !noToast) {
                    Toast.makeText(context, R.string.toast_api_level_30_must_reset, Toast.LENGTH_LONG).show()
                }
                isApiUpgraded
            }
            else -> true
        }
    }

    // Skip Logic
    fun shouldSkip(slide: FirstStartActivity.Slide): Boolean {
        return when (slide) {
            FirstStartActivity.Slide.INTRO -> !prefs.getBoolean(Constants.PREF_FIRST_START, true)
            FirstStartActivity.Slide.STORAGE -> isStorageGranted
            FirstStartActivity.Slide.LOCATION -> isLocationGranted
            FirstStartActivity.Slide.API_LEVEL_30 -> {
                val isRoot = prefs.getBoolean(Constants.PREF_USE_ROOT, false)
                isApiUpgraded || isRoot
            }
            FirstStartActivity.Slide.NOTIFICATION -> {
                if (Build.VERSION.SDK_INT < 33) true
                else ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    fun onNext() {
        if (!canAdvance()) return

        scope.launch {
            var nextIndex = pagerState.currentPage + 1
            // Loop until we find a slide we shouldn't skip or we run out of slides
            while (nextIndex < slides.size && shouldSkip(slides[nextIndex])) {
                nextIndex++
            }

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
    LaunchedEffect(Unit) {
        if (shouldSkip(FirstStartActivity.Slide.INTRO)) {
            onNext()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(), // Handle insets
        bottomBar = {
            BottomControls(
                pagerState = pagerState,
                slideCount = slides.size,
                onBack = ::onBack,
                onNext = ::onNext,
                canAdvance = ::canAdvance
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
                storageLauncher = storageLauncher,
                locationLauncher = locationLauncher,
                notificationLauncher = notificationLauncher,
                onUpgradeDatabase = {
                    activity.performApi30Upgrade()
                    isApiUpgraded = true
                                    },
                isStorageGranted = isStorageGranted,
                isLocationGranted = isLocationGranted,
                isNotificationGranted = isNotificationGranted,
                isApiUpgraded = isApiUpgraded
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
    canAdvance: (noToast: Boolean) -> Boolean
) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()

    // TODO: Low priority: move this into function, then use that in every composable that needs it.
    // Detect small screen
    val config = LocalWindowInfo.current.containerDpSize
    val isSmallScreen = config.width < 360.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(if (isSmallScreen) 4.dp else 16.dp),
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
            Button(
                onClick = onNext,
                // TODO: Look into if this could be done without lifecycleState checking
                //This is to update the composable state
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


@Composable
fun SlideContent(
    slide: FirstStartActivity.Slide,
    activity: FirstStartActivity,
    storageLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    locationLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    notificationLauncher: ManagedActivityResultLauncher<String, Boolean>,
    onUpgradeDatabase: () -> Unit,
    isStorageGranted: Boolean,
    isLocationGranted: Boolean,
    isNotificationGranted: Boolean,
    isApiUpgraded: Boolean

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
                Toast.makeText(context, R.string.dialog_all_files_access_not_supported, Toast.LENGTH_LONG).show()

                // TODO: Low priority: Test this
                Toast.makeText(context, "EXPERIMENTAL: Launching old write external storage permission instead.", Toast.LENGTH_LONG).show()
                storageLauncher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            }
        } else {
            // Android 10 and below
            storageLauncher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    fun askForLocationPermission() {
        // TODO: Implement one click background location permission logic, similar to askForStoragePermission() if needed
        locationLauncher.launch(PermissionUtil.getLocationPermissions())
    }

    fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // SDK 33
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
                    isLocationGranted
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
                    isNotificationGranted
                )
            }
        }
    }
}