package dev.benedek.syncthingandroid.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.preference.PreferenceManager
import dev.benedek.syncthingandroid.service.Constants

/**
 * [ThemeControls] stores the global state of theming controls.
 * It stores 2 fields: [.useDynamicColor] and [.useDarkMode],
 * both of them are Boolean type.
 */
object ThemeControls : SharedPreferences.OnSharedPreferenceChangeListener {

    val showDividers: Boolean = false

    val dividerThickness: Int = 1

    // Plain old vars backed by Compose State.
    // Reading these automatically registers the Composable for updates.
    var isBlurEnabled: Boolean by mutableStateOf(false)
        private set

    var isMonetEnabled: Boolean by mutableStateOf(false)
        private set
    var useDarkMode: Boolean? by mutableStateOf(null) // null = auto

    var pureBlack: Boolean by mutableStateOf(false)

    private var isInitialized = false

    /**
     * Call this ONCE at app startup (e.g., Application.onCreate or MainActivity.onCreate).
     * It loads the initial values and binds the singleton as a listener so the GC doesn't kill it.
     */
    fun init(context: Context) {
        if (isInitialized) return

        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

        isBlurEnabled = prefs.getBoolean(Constants.PREF_ENABLE_BLUR, false)
        isMonetEnabled = prefs.getBoolean(Constants.PREF_ENABLE_MONET, false)
        useDarkMode = useDarkMode(prefs)
        pureBlack = prefs.getBoolean(Constants.PREF_PURE_BLACK, false)

        prefs.registerOnSharedPreferenceChangeListener(this)
        isInitialized = true
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            Constants.PREF_ENABLE_BLUR -> isBlurEnabled = sharedPreferences.getBoolean(key, false)
            Constants.PREF_ENABLE_MONET -> isMonetEnabled = sharedPreferences.getBoolean(key, false)
            Constants.PREF_APP_THEME -> useDarkMode = useDarkMode(sharedPreferences)
            Constants.PREF_PURE_BLACK -> pureBlack = sharedPreferences.getBoolean(key, false)
        }
    }
    private fun useDarkMode(prefs: SharedPreferences): Boolean? {
        val themeValue = prefs.getString(Constants.PREF_APP_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString())
        AppCompatDelegate.setDefaultNightMode(themeValue?.toInt() ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        return if (themeValue?.toInt() == AppCompatDelegate.MODE_NIGHT_YES) {
            true
        } else if (themeValue?.toInt() == AppCompatDelegate.MODE_NIGHT_NO) {
            false
        } else {
            null
        }

    }

    val blurRadius: Int = 12

    const val PREVIEW_DARK_THEME = true

    /**
     * A compile-time knowable if-statement would be perfectly sane here, but Kotlin *somehow*
     * isn't smart enough to evaluate an if statement at compile time
     * (it can evaluate it, but doesn't accept it as const for some reason)
     * so instead of: if (darkTheme) UI_MODE_NIGHT_YES else UI_MODE_NIGHT_NO
     * I have to use a math operations, which are compile-time resolvable.
     *
     * Luckily UI_MODE_NIGHT_YES has an int value 32 and UI_MODE_NIGHT_NO 16,
     * so the operation should be mathematically easy. Now how do I get the int value from a Boolean?
     * Just cast it to Int, right? right? surely it works? **NO?** why?
     * Maybe it has a reason so complicated I wouldn't understand.
     *
     * Anyway, Bool.compareTo saved the day.
     */

    const val UI_MODE = UI_MODE_NIGHT_NO * (1 + PREVIEW_DARK_THEME.compareTo(false))
}