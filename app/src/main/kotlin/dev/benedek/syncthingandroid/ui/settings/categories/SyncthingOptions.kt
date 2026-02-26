package dev.benedek.syncthingandroid.ui.settings.categories

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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.ui.reusable.SettingsAlertDialog
import dev.benedek.syncthingandroid.ui.reusable.preventClicksWhenExiting
import dev.benedek.syncthingandroid.ui.settings.SettingsViewModel
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.switchPreference
import me.zhanghai.compose.preference.textFieldPreference

@Composable
fun SyncthingOptions(contentPadding: PaddingValues, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val showUndoDialog = remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .preventClicksWhenExiting(),
        contentPadding = contentPadding
    ) {
        textFieldPreference(
            key = "deviceName",
            defaultValue = "",
            title = { Text(stringResource(R.string.device_name)) },
            textToValue = { it },
            summary = { value -> Text(value) },
            valueToText = { it },
            rememberState = { viewModel.deviceName },
            textField = { value, onValueChange, onOk ->
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        onOk()
                    }),
                    singleLine = true
                )
            },
            enabled = { viewModel.isApiAvailable }
        )
        textFieldPreference(
            key = "listenAddresses",
            defaultValue = "",
            title = { Text(stringResource(R.string.listen_address)) },
            textToValue = { it },
            summary = { value -> Text(value) },
            valueToText = { it },
            rememberState = { viewModel.listenAddresses },
            textField = { value, onValueChange, onOk ->
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        onOk()
                    }),
                    singleLine = true
                )
            },
            enabled = { viewModel.isApiAvailable }
        )
        textFieldPreference(
            key = "maxRecvKbps",
            defaultValue = "",
            title = { Text(stringResource(R.string.max_recv_kbps)) },
            textToValue = { it },
            summary = { value -> Text(value) },
            valueToText = { it },
            rememberState = { viewModel.maxRecvKbps },
            textField = { value, onValueChange, onOk ->
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        onOk()
                    }),
                    singleLine = true
                )
            },
            enabled = { viewModel.isApiAvailable }
        )
        textFieldPreference(
            key = "maxSendKbps",
            defaultValue = "",
            title = { Text(stringResource(R.string.max_send_kbps)) },
            textToValue = { it },
            summary = { value -> Text(value) },
            valueToText = { it },
            rememberState = { viewModel.maxSendKbps },
            textField = { value, onValueChange, onOk ->
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        onOk()
                    }),
                    singleLine = true
                )
            },
            enabled = { viewModel.isApiAvailable }
        )
        switchPreference(
            key = "natEnabled",
            title = { Text(stringResource(R.string.enable_nat_traversal)) },
            defaultValue = false,
            rememberState = { viewModel.natEnabled },
            enabled = { viewModel.isApiAvailable }
        )
        switchPreference(
            key = "localAnnounceEnabled",
            title = { Text(stringResource(R.string.local_announce_enabled)) },
            defaultValue = false,
            rememberState = { viewModel.localAnnounceEnabled },
            enabled = { viewModel.isApiAvailable }
        )
        switchPreference(
            key = "globalAnnounceEnabled",
            title = { Text(stringResource(R.string.global_announce_enabled)) },
            defaultValue = false,
            rememberState = { viewModel.globalAnnounceEnabled },
            enabled = { viewModel.isApiAvailable }
        )
        switchPreference(
            key = "relaysEnabled",
            title = { Text(stringResource(R.string.enable_relaying)) },
            defaultValue = false,
            rememberState = { viewModel.relaysEnabled },
            enabled = { viewModel.isApiAvailable }
        )
        textFieldPreference(
            key = "globalAnnounceServers",
            defaultValue = "",
            title = { Text(stringResource(R.string.global_announce_server)) },
            textToValue = { it },
            summary = { value -> Text(value) },
            valueToText = { it },
            rememberState = { viewModel.globalAnnounceServers },
            textField = { value, onValueChange, onOk ->
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        onOk()
                    }),
                    singleLine = true
                )
            },
            enabled = { viewModel.isApiAvailable }
        )
        textFieldPreference(
            key = "address",
            defaultValue = "",
            title = { Text(stringResource(R.string.gui_address)) },
            textToValue = { it },
            summary = { value -> Text(value) },
            valueToText = { it },
            rememberState = { viewModel.guiAddress },
            textField = { value, onValueChange, onOk ->
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        onOk()
                    }),
                    singleLine = true
                )
            },
            enabled = { viewModel.isApiAvailable }
        )
        switchPreference(
            key = "urAccepted",
            title = { Text(stringResource(R.string.usage_reporting)) },
            defaultValue = false,
            rememberState = { viewModel.urAccepted },
            enabled = { viewModel.isApiAvailable }
        )
        preference(
            key = "undo_ignored_devices_folders",
            title = {
                Text(stringResource(R.string.undo_ignored_devices_folders_title))
            },
            onClick = { showUndoDialog.value = true },
            enabled = viewModel.isApiAvailable
        )
    }

    SettingsAlertDialog(
        text = stringResource(R.string.undo_ignored_devices_folders_question),
        confirmAction = { viewModel.resetIgnored(context) },
        showDialog = showUndoDialog,
    )
}