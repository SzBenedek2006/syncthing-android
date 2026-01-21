package dev.benedek.syncthingandroid.ui.reusable

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.preference.PreferenceManager
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.logic.util.WifiSsidUtil
import dev.benedek.syncthingandroid.service.Constants
import me.zhanghai.compose.preference.multiSelectListPreference

@Composable
fun rememberWifiSsidState(): State<WifiSsidUtil.WifiListResult> {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    val resultState = produceState(
        initialValue = WifiSsidUtil.WifiListResult(emptyList(), emptyList())
    ) {
        value = WifiSsidUtil.calculateWifiList(context, prefs, Constants.PREF_WIFI_SSID_WHITELIST)
    }

    return resultState
}

fun LazyListScope.wifiSsidPreference(
    state: WifiSsidUtil.WifiListResult,
    enabled: (Set<String>) -> Boolean = { true }
) {
    // We can use the messageId from the state to change the UI behavior
    val isWifiReady = state.messageId == null

    multiSelectListPreference(
        key = Constants.PREF_WIFI_SSID_WHITELIST,
        defaultValue = emptySet(),
        enabled = { isWifiReady && enabled(it) },

        values = state.entryValues,
        title = { Text(stringResource(R.string.run_on_whitelisted_wifi_title)) },
        valueToText = { value -> AnnotatedString(WifiSsidUtil.stripQuotes(value)) },

        summary = { selectedValues: Set<String> ->
            if (!isWifiReady) {
                Text(
                    text = stringResource(state.messageId),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error
                )
            } else if (selectedValues.isEmpty()) {
                Text(stringResource(R.string.run_on_all_wifi_networks))
            } else {
                val cleanNames = selectedValues.map { WifiSsidUtil.stripQuotes(it) }
                Text(stringResource(R.string.run_on_whitelisted_wifi_networks, cleanNames.joinToString(", ")))
            }
        }
    )
}