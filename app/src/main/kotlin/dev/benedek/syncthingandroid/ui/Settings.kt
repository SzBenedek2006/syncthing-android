package dev.benedek.syncthingandroid.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.switchPreference

@Composable
fun Settings() {
    val themeNames = stringArrayResource(R.array.theme_names)
    val themeValues = stringArrayResource(R.array.theme_values)
    val themeMap = remember { themeValues.zip(themeNames).toMap() }

    ProvidePreferenceLocals {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            listPreference(
                key = "theme",
                title = { Text(stringResource(R.string.preference_theme_title)) },
                valueToText = { value ->
                    AnnotatedString(themeMap[value] ?: value)
                },
                values = themeValues.toList(),
                defaultValue = "-1",
                summary = { value ->
                    Text(themeMap[value] ?: value)
                },
                //rememberState = TODO(),
                //enabled = TODO(),
                //icon = TODO(),
                //type = TODO(),
                //item = TODO()
            )
        }
    }
}

@Composable
@Preview(showBackground = true, uiMode = AndroidUiModes.UI_MODE_NIGHT_NO)
fun SettingsPreview() {
    SyncthingandroidTheme() {
        Settings()
    }
}