package dev.benedek.syncthingandroid.ui.reusable

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.benedek.syncthingandroid.ui.SettingsViewModel
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

@Composable
fun SettingsAlertDialog(
    modifier: Modifier = Modifier,
    title: String? = null,
    text: String? = null,
    confirmAction: () -> Unit,
    showDialog: MutableState<Boolean>,
    @StringRes confirmText: Int = android.R.string.ok,
    @StringRes dismissText: Int = android.R.string.cancel,
    ) {

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showDialog.value = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmAction()
                        showDialog.value = false
                    }
                ) {

                    Text(stringResource(confirmText))
                }

            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog.value = false
                    }
                ) {
                    Text(stringResource(dismissText))
                }
            },
            title = {
                if (title != null) {
                    Text(title)
                }
            },
            text = {
                if (text != null) {
                    Text(text)
                }
            }
        )
    }
}




@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsComponentsPreview() {
    SyncthingandroidTheme(dynamicColor = ThemeControls.useDynamicColor) {
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