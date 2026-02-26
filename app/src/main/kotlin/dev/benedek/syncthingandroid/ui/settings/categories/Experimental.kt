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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.preference.PreferenceManager
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.ui.reusable.preventClicksWhenExiting
import dev.benedek.syncthingandroid.ui.settings.SettingsViewModel
import me.zhanghai.compose.preference.switchPreference
import me.zhanghai.compose.preference.textFieldPreference

@Composable
fun Experimental(contentPadding: PaddingValues, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)



    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .preventClicksWhenExiting(),
        contentPadding = contentPadding
    ) {
        switchPreference(
            key = Constants.PREF_USE_ROOT,
            title = { Text(stringResource(R.string.use_root_title)) },
            summary = { Text(stringResource(R.string.use_root_summary)) },
            defaultValue = false,
            rememberState = { viewModel.useRoot },
        )
        switchPreference(
            key = Constants.PREF_USE_WAKE_LOCK,
            title = { Text(stringResource(R.string.keep_wakelock_while_binary_running)) },
            summary = { Text(stringResource(R.string.keep_wakelock_while_binary_running_summary)) },
            defaultValue = false
        )
        switchPreference(
            key = Constants.PREF_USE_TOR,
            title = { Text(stringResource(R.string.use_tor_title)) },
            summary = { Text(stringResource(R.string.use_tor_summary)) },
            defaultValue = false,
            rememberState = { viewModel.useTor }
        )
        textFieldPreference(
            key = Constants.PREF_SOCKS_PROXY_ADDRESS,
            defaultValue = "",
            title = { Text(stringResource(R.string.socks_proxy_address_title)) },
            textToValue = { it },
            summary = { value ->
                if (value.isEmpty()) {
                    Text(
                        stringResource(R.string.do_not_use_proxy) + " " +
                                stringResource(R.string.generic_example) + ": " +
                                stringResource(R.string.socks_proxy_address_example)
                    )
                } else {
                    Text(value)
                }
            },
            valueToText = { it },
            rememberState = { viewModel.socksProxyAddress },
            textField = { value, onValueChange, onOk ->
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Uri,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        onOk()
                    }),
                    singleLine = true
                )
            },
            enabled = { !viewModel.useTor.value }
        )
        textFieldPreference(
            key = Constants.PREF_HTTP_PROXY_ADDRESS,
            defaultValue = "",
            title = { Text(stringResource(R.string.http_proxy_address_title)) },
            textToValue = { it },
            summary = { value ->
                if (value.isEmpty()) {
                    Text(
                        stringResource(R.string.do_not_use_proxy) + " " +
                                stringResource(R.string.generic_example) + ": " +
                                stringResource(R.string.http_proxy_address_example)
                    )
                } else {
                    Text(value)
                }
            },
            valueToText = { it },
            rememberState = { viewModel.httpProxyAddress },
            textField = { value, onValueChange, onOk ->
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Uri,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        onOk()
                    }),
                    singleLine = true
                )
            },
            enabled = { !viewModel.useTor.value }
        )
        switchPreference(
            key = "use_legacy_hashing",
            title = { Text(stringResource(R.string.use_legacy_hashing_title)) },
            summary = { Text(stringResource(R.string.use_legacy_hashing_summary)) },
            defaultValue = false
        )
    }
}