package dev.benedek.syncthingandroid.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.ui.slides.IntroSlide
import dev.benedek.syncthingandroid.ui.slides.StorageSlide
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.PermissionUtil
import dev.benedek.syncthingandroid.util.Util
import kotlinx.coroutines.launch
import org.apache.commons.io.FileUtils
import java.io.File
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.edit
import dev.benedek.syncthingandroid.ui.slides.ApiUpgradeSlide
import dev.benedek.syncthingandroid.ui.slides.LocationSlide
import dev.benedek.syncthingandroid.ui.slides.NotificationSlide
import dev.benedek.syncthingandroid.util.ThemeControls

class FirstStartActivity : ComponentActivity() {
    enum class Slide {
        INTRO,
        STORAGE,
        LOCATION,
        API_LEVEL_30,
        NOTIFICATION
    }


    lateinit var mPreferences: SharedPreferences // Use lateinit for injected fields

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        /**
         * Recheck storage permission. If it has been revoked after the user
         * completed the welcome slides, displays the slides again.
         */
        if (!isFirstStart() && PermissionUtil.haveStoragePermission(this) && upgradedToApiLevel30()) {
            startApp()
            return
        } else {
            mPreferences.edit { putBoolean(Constants.PREF_UPGRADED_TO_API_LEVEL_30, true) }
        }

        setContent {
            SyncthingandroidTheme(
                dynamicColor = ThemeControls.getUseDynamicColor()
            ) {
                FirstStartScreen(
                    onFinish = {
                        mPreferences.edit { putBoolean(Constants.PREF_FIRST_START, false) }
                        startApp()
                    },
                    prefs = mPreferences,
                    activity = this
                )
            }

        }



/*
        // Old views based pager
        // Show first start welcome wizard UI.
        binding = ActivityFirstStartBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        // Targeting android 15 enables and 16 forces edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(
            binding!!.root,
            OnApplyWindowInsetsListener { v: View, windowInsets: WindowInsetsCompat ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                val mlp = v.layoutParams as MarginLayoutParams
                mlp.leftMargin = insets.left
                mlp.bottomMargin = insets.bottom
                mlp.rightMargin = insets.right
                v.layoutParams = mlp
                WindowInsetsCompat.CONSUMED
            })

        ViewCompat.setOnApplyWindowInsetsListener(
            binding!!.root,
            OnApplyWindowInsetsListener { v: View, insets: WindowInsetsCompat ->
                val bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            or WindowInsetsCompat.Type.displayCutout()
                )
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                WindowInsetsCompat.CONSUMED
            })

        binding?.viewPager?.setOnTouchListener { v, _ ->
            // Consume the event to prevent swiping through the slides.
            v.performClick()
            true
        }

        // Add bottom dots
        addBottomDots()
        setActiveBottomDot(0)

        mViewPagerAdapter = ViewPagerAdapter()
        binding?.viewPager?.adapter = mViewPagerAdapter
        binding?.viewPager?.addOnPageChangeListener(mViewPagerPageChangeListener)

        binding?.btnBack?.setOnClickListener { onBtnBackClick() }

        binding?.btnNext?.setOnClickListener { onBtnNextClick() }

        if (!this.isFirstStart) {
            // Skip intro slide
            onBtnNextClick()
        }
*/



    }

    fun isFirstStart(): Boolean {
        return mPreferences.getBoolean(Constants.PREF_FIRST_START, true)
    }

    fun upgradedToApiLevel30(): Boolean {
        if (mPreferences.getBoolean(Constants.PREF_UPGRADED_TO_API_LEVEL_30, false)) {
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
        if (mPreferences.getBoolean(Constants.PREF_START_INTO_WEB_GUI, false)) {
            startActivities(arrayOf<Intent>(mainIntent, Intent(this, WebGuiActivity::class.java)))
        } else {
            startActivity(mainIntent)
        }
        finish()
    }


//    private fun upgradeToApiLevel30() {
//        val dbDir = File(this.filesDir, "index-v0.14.0.db")
//        if (dbDir.exists()) {
//            try {
//                FileUtils.deleteQuietly(dbDir)
//            } catch (e: Throwable) {
//                Log.w(TAG, "Deleting database with FileUtils failed", e)
//                Util.runShellCommand("rm -r " + dbDir.absolutePath, false)
//                if (dbDir.exists()) {
//                    throw RuntimeException("Failed to delete existing database")
//                }
//            }
//        }
//        mPreferences.edit().putBoolean(Constants.PREF_UPGRADED_TO_API_LEVEL_30, true).apply()
//    }


    fun performApi30Upgrade() {
        val dbDir = File(filesDir, "index-v0.14.0.db")
        if (dbDir.exists()) {
            try {
                FileUtils.deleteQuietly(dbDir)
            } catch (e: Throwable) {
                Log.w("FirstStart", "Deleting database with FileUtils failed", e)
                Util.runShellCommand("rm -r " + dbDir.absolutePath, false)
                if (dbDir.exists()) {
                    throw RuntimeException("Failed to delete existing database")
                }
            }
        }
        mPreferences.edit { putBoolean(Constants.PREF_UPGRADED_TO_API_LEVEL_30, true) }
    }


}




@Composable
fun FirstStartScreen(
    onFinish: () -> Unit,
    prefs: SharedPreferences,
    activity: FirstStartActivity
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val slides = FirstStartActivity.Slide.entries.toTypedArray()

    // Setup Pager
    val pagerState = rememberPagerState(pageCount = { slides.size })


    // Launchers
    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
    }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
    }

    // Calculate current slide (derived from pager)
    val currentSlide = slides[pagerState.currentPage]

    // Navigation Logic
    fun canAdvance(noToast: Boolean = false): Boolean {
        // We read permissionRefreshTrigger to ensure this block re-runs when permissions change
        //val tick = permissionRefreshTrigger

        return when (currentSlide) {
            FirstStartActivity.Slide.STORAGE -> {
                val granted = PermissionUtil.haveStoragePermission(context)
                if (!granted && !noToast) {
                    Toast.makeText(context, R.string.toast_write_storage_permission_required, Toast.LENGTH_LONG).show()
                }
                granted
            }
            FirstStartActivity.Slide.API_LEVEL_30 -> {
                val upgraded = prefs.getBoolean(Constants.PREF_UPGRADED_TO_API_LEVEL_30, false) ||
                        prefs.getBoolean(Constants.PREF_FIRST_START, true) // Logic from activity: if first start, we mark upgraded

                if (!upgraded && !noToast) {
                    Toast.makeText(context, R.string.toast_api_level_30_must_reset, Toast.LENGTH_LONG).show()
                }
                upgraded
            }
            else -> true
        }
    }

    // Skip Logic
    fun shouldSkip(slide: FirstStartActivity.Slide): Boolean {
        return when (slide) {
            FirstStartActivity.Slide.INTRO -> !prefs.getBoolean(Constants.PREF_FIRST_START, true)
            FirstStartActivity.Slide.STORAGE -> PermissionUtil.haveStoragePermission(context)
            FirstStartActivity.Slide.LOCATION -> {
                // Check if all location perms are granted
                PermissionUtil.getLocationPermissions().all {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                }
            }
            FirstStartActivity.Slide.API_LEVEL_30 -> {
                val upgraded = prefs.getBoolean(Constants.PREF_UPGRADED_TO_API_LEVEL_30, false)
                val isFirstStart = prefs.getBoolean(Constants.PREF_FIRST_START, true)
                val isRoot = prefs.getBoolean(Constants.PREF_USE_ROOT, false)
                upgraded || isFirstStart || isRoot
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
            userScrollEnabled = false // Disable swipe, force buttons like original
        ) { page ->
            SlideContent(
                slide = slides[page],
                activity = activity,
                storageLauncher = storageLauncher,
                locationLauncher = locationLauncher,
                notificationLauncher = notificationLauncher,
                onUpgradeDatabase = {
                    activity.performApi30Upgrade()
                },
                refreshCallback = { }
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
    refreshCallback: () -> Unit
) {
    val context = LocalContext.current

    fun askForStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ All Files Access
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:${context.packageName}".toUri()
                activity.startActivity(intent)
                // NOTE: Compose doesn't refresh after resuming, so manual resuming is necessary
            } catch (e: Exception) {
                Toast.makeText(context, R.string.dialog_all_files_access_not_supported, Toast.LENGTH_LONG).show()
            }
        } else {
            // Android 10 and below
            storageLauncher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    @Composable
    fun isStoragePermissionNotGranted(context: Context): Boolean {
        //This is to update the composable state
        val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()

        return lifecycleState.isAtLeast(Lifecycle.State.RESUMED) && !PermissionUtil.haveStoragePermission(context)
    }


    fun askForLocationPermission() { // TODO: Implement one click background location permission logic, similar to askForStoragePermission()
        locationLauncher.launch(PermissionUtil.getLocationPermissions())
    }

    @Composable
    fun isLocationPermissionNotGranted(context: Context): Boolean {
        //This is to update the composable state
        val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()

        return lifecycleState.isAtLeast(Lifecycle.State.RESUMED) && !PermissionUtil.hasLocationPermissions(context)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // SDK 33
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @Composable
    fun isNotificationPermissionNotGranted(context: Context): Boolean {
        //This is to update the composable state
        val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()

        return lifecycleState.isAtLeast(Lifecycle.State.RESUMED) && !PermissionUtil.hasNotificationPermission(context)
    }



    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // This switches based on the slide type, similar to adapter `instantiateItem`
        when (slide) {
            FirstStartActivity.Slide.INTRO -> {
                IntroSlide()
            }

            FirstStartActivity.Slide.STORAGE -> {
                StorageSlide(::askForStoragePermission, ::isStoragePermissionNotGranted)
            }

            FirstStartActivity.Slide.LOCATION -> {
                LocationSlide(
                    ::askForLocationPermission,
                    ::isLocationPermissionNotGranted
                    )
            }

            FirstStartActivity.Slide.API_LEVEL_30 -> {
                ApiUpgradeSlide(
                     onUpgradeDatabase,
                    {true}
                )
            }

            FirstStartActivity.Slide.NOTIFICATION -> {
                NotificationSlide(
                    ::askForNotificationPermission,
                    ::isNotificationPermissionNotGranted
                )
                /*
                {
                Text(text = "Enable Notifications")
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) {
                    Text("Enable Notifications")
                }

                * */
            }
        }
    }
}