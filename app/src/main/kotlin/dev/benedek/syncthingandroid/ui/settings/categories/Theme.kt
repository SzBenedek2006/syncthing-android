package dev.benedek.syncthingandroid.ui.settings.categories

import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.ui.reusable.preventClicksWhenExiting
import dev.benedek.syncthingandroid.util.ThemeControls
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.switchPreference

@Composable
fun Theme(contentPadding: PaddingValues) {
    val themeNames = stringArrayResource(R.array.theme_entries)
    val themeValues = stringArrayResource(R.array.theme_values)
    val themeMap = remember { themeValues.zip(themeNames).toMap() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .preventClicksWhenExiting(),
        contentPadding = contentPadding
    ) {

        listPreference(
            key = Constants.PREF_APP_THEME,
            title = { Text(stringResource(R.string.preference_theme_title)) },
            valueToText = { value -> AnnotatedString(themeMap[value] ?: value) },
            values = themeValues.toList(),
            defaultValue = "-1",
            summary = { value -> Text(themeMap[value] ?: value) },
        )
        val minVersion = 12
        switchPreference(
            key = Constants.PREF_ENABLE_BLUR,
            title = { Text(stringResource(R.string.blur_title)) },
            summary = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    Text(stringResource(R.string.blur_description))
                else
                    Text(stringResource(R.string.only_available_on_android_or_higher, minVersion))
                      },
            defaultValue = false,
            enabled = { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
        )
        switchPreference(
            key = Constants.PREF_ENABLE_MONET,
            title = {
                Text(stringResource(R.string.dynamic_colors_title))
            },
            summary = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    Text(stringResource(R.string.dynamic_colors_description))
                else
                    Text(stringResource(R.string.only_available_on_android_or_higher, minVersion))
            },
            defaultValue = false,
            enabled = { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
        )
    }
}

