package dev.benedek.syncthingandroid.ui.settings.categories

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.activities.LogActivity
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.ui.reusable.SettingsAlertDialog
import dev.benedek.syncthingandroid.ui.reusable.preventClicksWhenExiting
import dev.benedek.syncthingandroid.ui.reusable.rememberSttraceState
import dev.benedek.syncthingandroid.ui.reusable.sttracePreference
import dev.benedek.syncthingandroid.ui.settings.SettingsViewModel
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.switchPreference
import me.zhanghai.compose.preference.textFieldPreference

@Composable
fun Debug(contentPadding: PaddingValues, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val showDatabaseDialog = remember { mutableStateOf(false) }
    val showDeltaDialog = remember { mutableStateOf(false) }
    val sttraceState = rememberSttraceState()


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .preventClicksWhenExiting(),
        contentPadding = contentPadding
    ) {
        preference(
            key = "open_log", // #noKey
            title = {
                Text(stringResource(R.string.open_log))
            },
            summary = {
                Text(stringResource(R.string.open_log_summary))
            },
            onClick = { context.startActivity(Intent(context, LogActivity::class.java)) }
        )
        switchPreference(
            key = "notify_crashes", // TODO: Take a look at NotificationHandler.java
            title = { Text(stringResource(R.string.notify_crashes_title)) },
            summary = { Text(stringResource(R.string.notify_crashes_summary)) },
            defaultValue = false // There was no default value in the original
        )
        sttracePreference( // TODO: Test
            state = sttraceState.value,
            key = Constants.PREF_DEBUG_FACILITIES_ENABLED
        )
        textFieldPreference(
            key = Constants.PREF_ENVIRONMENT_VARIABLES,
            defaultValue = "",
            title = { Text(stringResource(R.string.environment_variables)) },
            textToValue = { it },
            summary = { value -> Text(value) },
            valueToText = { it },
            rememberState = { viewModel.environmentVariables },
            textField = { value, onValueChange, onOk ->
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Text,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        onOk()
                    }),
                    singleLine = true
                )
            }
        )
        preference(
            key = "st_reset_database",
            title = { Text(stringResource(R.string.st_reset_database_title)) },
            onClick = { showDatabaseDialog.value = true }
        )
        preference(
            key = "st_reset_deltas",
            title = { Text(stringResource(R.string.st_reset_deltas_title)) },
            onClick = { showDeltaDialog.value = true }
        )
    }

    SettingsAlertDialog(
        text = stringResource(R.string.st_reset_database_question),
        title = stringResource(R.string.st_reset_database_title),
        confirmAction = { viewModel.resetDatabase(context) },
        showDialog = showDatabaseDialog,
    )
    SettingsAlertDialog(
        text = stringResource(R.string.st_reset_deltas_question),
        title = stringResource(R.string.st_reset_deltas_title),
        confirmAction = { viewModel.resetDeltas(context) },
        showDialog = showDeltaDialog,
    )
}