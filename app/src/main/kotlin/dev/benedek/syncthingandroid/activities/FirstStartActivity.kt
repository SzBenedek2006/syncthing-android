package dev.benedek.syncthingandroid.activities

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.google.android.material.color.MaterialColors
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.SyncthingApp
import dev.benedek.syncthingandroid.databinding.ActivityFirstStartBinding
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.Constants.PermissionRequestType
import dev.benedek.syncthingandroid.util.PermissionUtil
import dev.benedek.syncthingandroid.util.Util
import org.apache.commons.io.FileUtils
import java.io.File
import javax.inject.Inject

class FirstStartActivity : Activity() {
    private enum class Slide(val layout: Int) {
        INTRO(R.layout.activity_firststart_slide_intro),
        STORAGE(R.layout.activity_firststart_slide_storage),
        LOCATION(R.layout.activity_firststart_slide_location),
        API_LEVEL_30(R.layout.activity_firststart_slide_api_level_30),
        NOTIFICATION(R.layout.activity_firststart_slide_notification)
    }

    private var mViewPagerAdapter: ViewPagerAdapter? = null
    private lateinit var mDots: Array<TextView?> // Updated to nullable array to match instantiation

    private var binding: ActivityFirstStartBinding? = null

    @Inject
    lateinit var mPreferences: SharedPreferences // Use lateinit for injected fields

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as SyncthingApp).component().inject(this)

        /**
         * Recheck storage permission. If it has been revoked after the user
         * completed the welcome slides, displays the slides again.
         */
        if (!this.isFirstStart && PermissionUtil.haveStoragePermission(this) && upgradedToApiLevel30()) {
            startApp()
            return
        }

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
    }

    private fun onBtnBackClick() {
        val current: Int = binding!!.viewPager.currentItem - 1
        if (current >= 0) {
            // Move to previous slider.
            binding?.viewPager?.currentItem = current
            if (current == 0) {
                binding?.btnBack?.visibility = View.GONE
            }
        }
    }

    private fun onBtnNextClick() {
        val slide = currentSlide()
        // Check if we are allowed to advance to the next slide.
        when (slide) {
            Slide.STORAGE -> {
                // As the storage permission is a prerequisite to run syncthing, refuse to continue without it.
                val storagePermissionsGranted = PermissionUtil.haveStoragePermission(this)
                if (!storagePermissionsGranted) {
                    Toast.makeText(
                        this, R.string.toast_write_storage_permission_required,
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
            }

            Slide.API_LEVEL_30 -> if (!upgradedToApiLevel30()) {
                Toast.makeText(
                    this, R.string.toast_api_level_30_must_reset,
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            else -> {}
        }

        var next: Int = binding!!.viewPager.currentItem + 1
        while (next < slides.size) {
            if (!shouldSkipSlide(slides[next])) {
                binding?.viewPager?.currentItem = next
                binding?.btnBack?.visibility = View.VISIBLE
                break
            }
            next++
        }
        if (next == slides.size) {
            // Start the app after "mNextButton" was hit on the last slide.
            Log.v(TAG, "User completed first start UI.")
            mPreferences.edit().putBoolean(Constants.PREF_FIRST_START, false).apply()
            startApp()
        }
    }

    private val isFirstStart: Boolean
        get() = mPreferences.getBoolean(Constants.PREF_FIRST_START, true)

    private val isNotificationPermissionGranted: Boolean
        @TargetApi(33)
        get() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return true
            }

            return ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }


    private fun upgradedToApiLevel30(): Boolean {
        if (mPreferences.getBoolean(Constants.PREF_UPGRADED_TO_API_LEVEL_30, false)) {
            return true
        }
        if (this.isFirstStart) {
            mPreferences.edit().putBoolean(Constants.PREF_UPGRADED_TO_API_LEVEL_30, true).apply()
            return true
        }
        return false
    }

    private fun upgradeToApiLevel30() {
        val dbDir = File(this.filesDir, "index-v0.14.0.db")
        if (dbDir.exists()) {
            try {
                FileUtils.deleteQuietly(dbDir)
            } catch (e: Throwable) {
                Log.w(TAG, "Deleting database with FileUtils failed", e)
                Util.runShellCommand("rm -r " + dbDir.absolutePath, false)
                if (dbDir.exists()) {
                    throw RuntimeException("Failed to delete existing database")
                }
            }
        }
        mPreferences.edit().putBoolean(Constants.PREF_UPGRADED_TO_API_LEVEL_30, true).apply()
    }

    private fun currentSlide(): Slide {
        return slides[binding!!.viewPager.currentItem]
    }

    private fun shouldSkipSlide(slide: Slide): Boolean {
        when (slide) {
            Slide.INTRO -> return !this.isFirstStart
            Slide.STORAGE -> return PermissionUtil.haveStoragePermission(this)
            Slide.LOCATION -> return hasLocationPermission()
            Slide.API_LEVEL_30 ->                 // Skip if running as root, as that circumvents any Android FS restrictions.
                return upgradedToApiLevel30()
                        || mPreferences.getBoolean(Constants.PREF_USE_ROOT, false)

            Slide.NOTIFICATION -> return this.isNotificationPermissionGranted

        }
        return false // Removed unreachable code warning here by structure
    }

    private fun addBottomDots() {
        mDots = arrayOfNulls(slides.size)
        for (i in mDots.indices) {
            mDots[i] = TextView(this)
            mDots[i]?.text = Html.fromHtml("&#8226;")
            mDots[i]?.textSize = 35f
            binding?.layoutDots?.addView(mDots[i])
        }
    }

    private fun setActiveBottomDot(currentPage: Int) {
        val colorInactive = MaterialColors.getColor(this, R.attr.colorPrimary, Color.BLUE)
        val colorActive = MaterialColors.getColor(this, R.attr.colorSecondary, Color.BLUE)
        for (mDot in mDots) {
            mDot?.setTextColor(colorInactive)
        }
        mDots[currentPage]?.setTextColor(colorActive)
    }

    //  ViewPager change listener
    var mViewPagerPageChangeListener: OnPageChangeListener = object : OnPageChangeListener {
        override fun onPageSelected(position: Int) {
            setActiveBottomDot(position)

            // Change the next button text from next to finish on last slide.
            binding?.btnNext?.setText(getString(if (position == slides.size - 1) R.string.finish else R.string.cont))
        }

        override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {
        }

        override fun onPageScrollStateChanged(arg0: Int) {
        }
    }

    /**
     * View pager adapter
     */
    inner class ViewPagerAdapter : PagerAdapter() {
        private var layoutInflater: LayoutInflater? = null

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            layoutInflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater

            val view = layoutInflater!!.inflate(slides[position].layout, container, false)

            when (slides[position]) {
                Slide.INTRO -> {}
                Slide.STORAGE -> {
                    // Explicitly cast findViewById results to solve generic type inference errors
                    val btnGrantStoragePerm =
                        view.findViewById<Button>(R.id.btnGrantStoragePerm)
                    btnGrantStoragePerm.setOnClickListener { requestStoragePermission() }
                }

                Slide.LOCATION -> {
                    val btnGrantLocationPerm =
                        view.findViewById<Button>(R.id.btnGrantLocationPerm)
                    btnGrantLocationPerm.setOnClickListener { requestLocationPermission() }
                }

                Slide.API_LEVEL_30 -> {
                    val btnResetDatabase = view.findViewById<Button>(R.id.btnResetDatabase)
                    btnResetDatabase.setOnClickListener {
                        upgradeToApiLevel30()
                        onBtnNextClick()
                    }
                }

                Slide.NOTIFICATION -> {
                    val notificationBtn = view.findViewById<Button>(R.id.btn_notification)
                    notificationBtn.setOnClickListener { requestNotificationPermission() }
                }
            }

            container.addView(view)
            return view
        }

        override fun getCount(): Int {
            return slides.size
        }

        // FIX: Removed nullable '?' from view and obj to match PagerAdapter signature
        override fun isViewFromObject(view: View, obj: Any): Boolean {
            return view === obj
        }

        // FIX: Removed nullable '?' from obj to match PagerAdapter signature
        override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
            val view = obj as View
            container.removeView(view)
        }
    }

    /**
     * Preconditions:
     * Storage permission has been granted.
     */
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

    private fun hasLocationPermission(): Boolean {
        for (perm in PermissionUtil.getLocationPermissions()) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    perm
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    /**
     * Permission check and request functions
     */
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            PermissionUtil.getLocationPermissions(),
            PermissionRequestType.LOCATION.ordinal
        )
    }

    @TargetApi(33)
    private fun requestNotificationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf<String>(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestAllFilesAccessPermission()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PermissionRequestType.STORAGE.ordinal
            )
        }
    }

    @TargetApi(30)
    private fun requestAllFilesAccessPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        try {
            val componentName = intent.resolveActivity(packageManager)
            if (componentName != null) {
                // Launch "Allow all files access?" dialog.
                startActivity(intent)
                return
            }
            Log.w(TAG, "Request all files access not supported")
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Request all files access not supported", e)
        }
        Toast.makeText(this, R.string.dialog_all_files_access_not_supported, Toast.LENGTH_LONG)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        // FIX: Directly access values() via Constants.PermissionRequestType
        val requestType = Constants.PermissionRequestType.values().getOrNull(requestCode)

        if (requestType != null) {
            when (requestType) {
                PermissionRequestType.LOCATION -> {
                    if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        Log.i(TAG, "User denied foreground location permission")
                        // FIX: Removed 'break'
                    } else {
                        Log.i(TAG, "User granted foreground location permission")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ActivityCompat.requestPermissions(
                                this,
                                PermissionUtil.getLocationPermissions(),
                                PermissionRequestType.LOCATION_BACKGROUND.ordinal
                            )
                        }
                    }
                }

                PermissionRequestType.LOCATION_BACKGROUND -> {
                    if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        Log.i(TAG, "User denied background location permission")
                        // FIX: Removed 'break'
                    } else {
                        Log.i(TAG, "User granted background location permission")
                        Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show()
                    }
                }

                PermissionRequestType.STORAGE -> {
                    if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        Log.i(TAG, "User denied WRITE_EXTERNAL_STORAGE permission.")
                    } else {
                        Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show()
                        Log.i(TAG, "User granted WRITE_EXTERNAL_STORAGE permission.")
                    }
                }

                // If conversion resulted in redundant branches, explicit else is usually safe
                else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    companion object {
        private val slides = Slide.entries.toTypedArray()
        private const val TAG = "FirstStartActivity"
    }
}