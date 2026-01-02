package dev.benedek.syncthingandroid.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.PermissionUtil
import dev.benedek.syncthingandroid.util.Util
import org.apache.commons.io.FileUtils
import java.io.File
import androidx.core.content.edit
import dev.benedek.syncthingandroid.ui.FirstStartScreen
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
            startActivities(arrayOf(mainIntent, Intent(this, WebGuiActivity::class.java)))
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
