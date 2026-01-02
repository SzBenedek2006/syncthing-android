package dev.benedek.syncthingandroid.ui.reusable

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls


@Composable
fun SettingEntry(
    onClick: @Composable () -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        onClick()
    }
    // TODO
}

@Composable
fun SettingCategory() {
    // TODO
}

@Composable
fun SelectionSetting() {
    // TODO
}

@Composable
fun MultiSelectionSetting() {
    // TODO
}

@Composable
fun TextInputSetting() {
    // TODO
}

@Composable
fun DummyComposable() {
    Text("Test")
}




@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsComponentsPreview() {
    SyncthingandroidTheme(dynamicColor = ThemeControls.getUseDynamicColor()) {
        Column() {
            Spacer(Modifier.size(200.dp))
            SelectionSetting()
            MultiSelectionSetting()
            TextInputSetting()
            SettingEntry(
                onClick = {
                    DummyComposable()
                }
            )
            SettingCategory()
        }

    }
}