package dev.benedek.syncthingandroid.activities

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager

/**
 * Provides a themed instance of AppCompatActivity.
 */
open class ThemedAppCompatActivity : AppCompatActivity() {

    val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        // Load theme.
        //For api level below 28, Follow system fall backs to light mode
        val prefAppTheme = sharedPreferences.getString(
            dev.benedek.syncthingandroid.service.Constants.PREF_APP_THEME,
            FOLLOW_SYSTEM
        )!!.toInt()
        AppCompatDelegate.setDefaultNightMode(prefAppTheme)
        super.onCreate(savedInstanceState)
    }

    companion object {
        private const val FOLLOW_SYSTEM = "-1"
    }
}
