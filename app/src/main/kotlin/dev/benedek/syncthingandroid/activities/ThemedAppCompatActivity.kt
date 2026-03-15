package dev.benedek.syncthingandroid.activities

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import dev.benedek.syncthingandroid.SyncthingApp
import javax.inject.Inject

/**
 * Provides a themed instance of AppCompatActivity.
 */
open class ThemedAppCompatActivity : AppCompatActivity() {
    @JvmField
    @Inject
    var sharedPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        (getApplication() as SyncthingApp).component().inject(this)
        // Load theme.
        //For api level below 28, Follow system fall backs to light mode
        val prefAppTheme = sharedPreferences!!.getString(
            dev.benedek.syncthingandroid.service.Constants.PREF_APP_THEME,
            ThemedAppCompatActivity.Companion.FOLLOW_SYSTEM
        )!!.toInt()
        AppCompatDelegate.setDefaultNightMode(prefAppTheme)
        super.onCreate(savedInstanceState)
    }

    companion object {
        private const val FOLLOW_SYSTEM = "-1"
    }
}
