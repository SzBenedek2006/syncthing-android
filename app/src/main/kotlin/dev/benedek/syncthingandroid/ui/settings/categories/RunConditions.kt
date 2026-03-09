package dev.benedek.syncthingandroid.ui.settings.categories

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.ui.reusable.preventClicksWhenExiting
import dev.benedek.syncthingandroid.ui.reusable.rememberWifiSsidState
import dev.benedek.syncthingandroid.ui.reusable.wifiSsidPreference
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.rememberPreferenceState
import me.zhanghai.compose.preference.switchPreference

@Composable
fun RunConditions(contentPadding: PaddingValues) {
    val wifiState = rememberWifiSsidState()
    val powerSourceNames = stringArrayResource(R.array.power_source_entries)
    val powerSourceValues = stringArrayResource(R.array.power_source_values)
    val powerSourceMap = remember { powerSourceValues.zip(powerSourceNames).toMap() }

    val runConditionsEnabled by rememberPreferenceState(
        key = Constants.PREF_RUN_CONDITIONS,
        defaultValue = true
    )
    val runOnWifiEnabled by rememberPreferenceState(
        key = Constants.PREF_RUN_ON_WIFI,
        defaultValue = true
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .preventClicksWhenExiting(),
        contentPadding = contentPadding
    ) {
        switchPreference(
            key = Constants.PREF_RUN_CONDITIONS,
            title = { Text(stringResource(R.string.run_conditions_title)) },
            summary = { Text(stringResource(R.string.run_conditions_summary)) },
            defaultValue = true
        )
        switchPreference(
            key = Constants.PREF_RUN_ON_WIFI,
            title = { Text(stringResource(R.string.run_on_wifi_title)) },
            summary = { Text(stringResource(R.string.run_on_wifi_summary)) },
            defaultValue = true,
            enabled = { runConditionsEnabled }
        )
        switchPreference(
            key = Constants.PREF_RUN_ON_METERED_WIFI,
            title = { Text(stringResource(R.string.run_on_metered_wifi_title)) },
            summary = { Text(stringResource(R.string.run_on_metered_wifi_summary)) },
            defaultValue = false,
            enabled = { runConditionsEnabled && runOnWifiEnabled }
        )
        wifiSsidPreference(
            wifiState.value,
            enabled = { runConditionsEnabled && runOnWifiEnabled }
        )
        switchPreference(
            key = Constants.PREF_RUN_ON_MOBILE_DATA,
            title = { Text(stringResource(R.string.run_on_mobile_data_title)) },
            summary = { Text(stringResource(R.string.run_on_mobile_data_summary)) },
            defaultValue = false,
            enabled = { runConditionsEnabled }
        )
        listPreference(
            key = Constants.PREF_POWER_SOURCE,
            title = { Text(stringResource(R.string.power_source_title)) },
            valueToText = { value ->
                AnnotatedString(
                    powerSourceMap[value] ?: value
                )
            },
            values = powerSourceValues.toList(),
            defaultValue = "ac_and_battery_power",
            enabled = { runConditionsEnabled },
            summary = { value -> Text(powerSourceMap[value] ?: value) }
        )
        switchPreference(
            key = Constants.PREF_RESPECT_BATTERY_SAVING,
            title = { Text(stringResource(R.string.respect_battery_saving_title)) },
            summary = { Text(stringResource(R.string.respect_battery_saving_summary)) },
            defaultValue = true,
            enabled = { runConditionsEnabled }
        )
        switchPreference(
            key = Constants.PREF_RESPECT_MASTER_SYNC,
            title = { Text(stringResource(R.string.respect_master_sync_title)) },
            summary = { Text(stringResource(R.string.respect_master_sync_summary)) },
            defaultValue = false,
            enabled = { runConditionsEnabled }
        )
        switchPreference(
            key = Constants.PREF_RUN_IN_FLIGHT_MODE,
            title = { Text(stringResource(R.string.run_in_flight_mode_title)) },
            summary = { Text(stringResource(R.string.run_in_flight_mode_summary)) },
            defaultValue = false,
            enabled = { runConditionsEnabled }
        )
    }
}