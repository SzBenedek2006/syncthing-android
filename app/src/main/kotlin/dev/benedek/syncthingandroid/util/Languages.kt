package dev.benedek.syncthingandroid.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.text.TextUtils
import androidx.preference.PreferenceManager
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.SyncthingApp
import java.util.Arrays
import java.util.Collections
import java.util.Locale
import java.util.TreeMap
import javax.inject.Inject
import androidx.core.content.edit

/**
 * Based on https://gitlab.com/fdroid/fdroidclient/blob/master/app/src/main/java/org/fdroid/fdroid/Languages.java
 */
class Languages(context: Context) {
    
    private val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    /**
     * Handles setting the language if it is different than the current language,
     * or different than the current system-wide locale.  The preference is cleared
     * if the language matches the system-wide locale or "System Default" is chosen.
     */
    fun setLanguage(context: Context) {
        val language = preferences.getString(PREFERENCE_LANGUAGE, null)
        val locale: Locale?
        if (TextUtils.equals(language, DEFAULT_LOCALE.language)) {
            preferences.edit { remove(PREFERENCE_LANGUAGE) }
            locale = DEFAULT_LOCALE
        } else if (language == null || language == USE_SYSTEM_DEFAULT) {
            preferences.edit { remove(PREFERENCE_LANGUAGE) }
            locale = DEFAULT_LOCALE
        } else {
            /* handle locales with the country in it, i.e. zh_CN, zh_TW, etc */
            val localeSplit: Array<String?> =
                language.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (localeSplit.size > 1) {
                locale = Locale(localeSplit[0], localeSplit[1])
            } else {
                locale = Locale(language)
            }
        }
        Locale.setDefault(locale)

        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    /**
     * Force reload the [to make language changes take effect.][Activity]
     * 
     * @param activity the `Activity` to force reload
     */
    @SuppressLint("ApplySharedPref")
    fun forceChangeLanguage(activity: Activity, newLanguage: String?) {
        preferences.edit(commit = true) { putString(PREFERENCE_LANGUAGE, newLanguage) }
        setLanguage(activity)
        val intent = activity.intent ?: return // when launched as LAUNCHER

        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        activity.finish()
        activity.overridePendingTransition(0, 0)
        activity.startActivity(intent)
        activity.overridePendingTransition(0, 0)
    }

    val allNames: Array<String?>
        /**
         * @return an array of the names of all the supported languages, sorted to
         * match what is returned by [Languages.getSupportedLocales].
         */
        get() = mAvailableLanguages.values.toTypedArray<String?>()

    val supportedLocales: Array<String?>
        /**
         * @return sorted list of supported locales.
         */
        get() {
            val keys: MutableSet<String?> =
                mAvailableLanguages.keys
            return keys.toTypedArray<String?>()
        }

    init {
        (context.applicationContext as SyncthingApp).component().inject(this)
        val tmpMap: MutableMap<String?, String?> = TreeMap<String?, String?>()
        val locales = listOf<Locale?>(*LOCALES_TO_TEST)
        // Capitalize language names
        Collections.sort(
            locales,
            Comparator { l1: Locale?, l2: Locale? ->
                l1!!.displayLanguage.compareTo(l2!!.displayLanguage)
            })
        for (locale in locales) {
            var displayLanguage = locale?.getDisplayLanguage(locale)
            displayLanguage =
                locale?.let { displayLanguage?.substring(0, 1)?.uppercase(it) } + displayLanguage?.substring(1)
            tmpMap[locale?.language] = displayLanguage
        }

        // remove the current system language from the menu
        tmpMap.remove(Locale.getDefault().language)

        /* SYSTEM_DEFAULT is a fake one for displaying in a chooser menu. */
        tmpMap.put(USE_SYSTEM_DEFAULT, context.getString(R.string.pref_language_default))
        mAvailableLanguages = Collections.unmodifiableMap<String?, String?>(tmpMap)
    }

    companion object {
        const val USE_SYSTEM_DEFAULT: String = ""

        private val DEFAULT_LOCALE: Locale
        const val PREFERENCE_LANGUAGE: String = "pref_current_language"

        private lateinit var mAvailableLanguages: MutableMap<String?, String?>

        init {
            DEFAULT_LOCALE = Locale.getDefault()
        }

        private val LOCALES_TO_TEST = arrayOf<Locale?>(
            Locale.ENGLISH,
            Locale.FRENCH,
            Locale.GERMAN,
            Locale.ITALIAN,
            Locale.JAPANESE,
            Locale.KOREAN,
            Locale.SIMPLIFIED_CHINESE,
            Locale.TRADITIONAL_CHINESE,
            Locale("af"),
            Locale("ar"),
            Locale("be"),
            Locale("bg"),
            Locale("ca"),
            Locale("cs"),
            Locale("da"),
            Locale("el"),
            Locale("es"),
            Locale("eo"),
            Locale("et"),
            Locale("eu"),
            Locale("fa"),
            Locale("fi"),
            Locale("he"),
            Locale("hi"),
            Locale("hu"),
            Locale("hy"),
            Locale("id"),
            Locale("is"),
            Locale("it"),
            Locale("ml"),
            Locale("my"),
            Locale("nb"),
            Locale("nl"),
            Locale("pl"),
            Locale("pt"),
            Locale("ro"),
            Locale("ru"),
            Locale("sc"),
            Locale("sk"),
            Locale("sn"),
            Locale("sr"),
            Locale("sv"),
            Locale("th"),
            Locale("tr"),
            Locale("uk"),
            Locale("vi"),
        )
    }
}
