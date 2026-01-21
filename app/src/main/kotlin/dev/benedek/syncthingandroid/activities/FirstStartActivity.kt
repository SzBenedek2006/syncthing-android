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
import dev.benedek.syncthingandroid.ui.Settings
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
                dynamicColor = ThemeControls.useDynamicColor
            ) {
                //Settings()
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
