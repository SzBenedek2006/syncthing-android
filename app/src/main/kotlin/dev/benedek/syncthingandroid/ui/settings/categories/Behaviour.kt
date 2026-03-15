package dev.benedek.syncthingandroid.ui.settings.categories

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.preference.PreferenceManager
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.ui.reusable.preventClicksWhenExiting
import dev.benedek.syncthingandroid.util.Languages
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.switchPreference

@Composable
fun Behaviour(contentPadding: PaddingValues) {
    val context = LocalContext.current

    val languageData = remember {
        if (Build.VERSION.SDK_INT < 24) {
            val languages = Languages(context)
            val codes = languages.supportedLocales.toList()
            val names = languages.allNames.toList()
            val map = codes.zip(names).toMap()
            Triple(codes, map, languages)
        } else {
            null
        }
    }

    DisposableEffect(Unit) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == Languages.PREFERENCE_LANGUAGE) {
                // Determine the new locale and restart activity
                // Using the helper logic from Languages, but manually triggers the restart
                // to avoid infinite loops of writing to prefs again.
                val activity = context as? Activity
                if (activity != null) {
                    val helper = Languages(activity)
                    helper.setLanguage(activity)

                    // Restart Activity (Logic from Languages.forceChangeLanguage)
                    val intent = activity.intent
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        activity.finish()
                        activity.overridePendingTransition(0, 0)
                        activity.startActivity(intent)
                        activity.overridePendingTransition(0, 0)
                    }
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .preventClicksWhenExiting(),
        contentPadding = contentPadding
    ) {
        switchPreference(
            key = "advanced_folder_picker",
            title = { Text(stringResource(R.string.advanced_folder_picker)) },
            summary = { Text(stringResource(R.string.advanced_folder_picker_summary)) },
            defaultValue = false
        )
        if (Build.VERSION.SDK_INT < 24 && languageData != null) {
            val (codes, map, _) = languageData
            listPreference(
                key = Languages.PREFERENCE_LANGUAGE,
                title = { Text(stringResource(R.string.preference_language_title)) },
                values = codes,
                defaultValue = Languages.USE_SYSTEM_DEFAULT,
                valueToText = { value ->
                    AnnotatedString(
                        map[value!!] ?: value
                    )
                },
                summary = { value -> Text(map[value!!] ?: value) }

            )
        }
        switchPreference(
            key = Constants.PREF_START_INTO_WEB_GUI,
            title = { Text(stringResource(R.string.start_into_web_gui_title)) },
            summary = { Text(stringResource(R.string.start_into_web_gui_summary)) },
            defaultValue = false
        )
        preference(
            key = "static_service_settings",
            title = { Text(stringResource(R.string.service_settings_title)) },
            summary = { Text(stringResource(R.string.service_settings_summary)) },
        )
        switchPreference(
            key = Constants.PREF_START_SERVICE_ON_BOOT,
            title = { Text(stringResource(R.string.start_service_on_boot)) },
            defaultValue = false
        )
    }
}