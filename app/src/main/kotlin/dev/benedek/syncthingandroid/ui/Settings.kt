package dev.benedek.syncthingandroid.ui

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.activities.LogActivity
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.ui.reusable.SettingsAlertDialog
import dev.benedek.syncthingandroid.ui.reusable.rememberSttraceState
import dev.benedek.syncthingandroid.ui.reusable.rememberWifiSsidState
import dev.benedek.syncthingandroid.ui.reusable.sttracePreference
import dev.benedek.syncthingandroid.ui.reusable.wifiSsidPreference
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.Languages
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory
import me.zhanghai.compose.preference.rememberPreferenceState
import me.zhanghai.compose.preference.switchPreference
import me.zhanghai.compose.preference.textFieldPreference


// TODO: Use Constants class for keys everywhere


@Composable
fun Settings(
    viewModel: SettingsViewModel,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    navController: NavHostController
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadInitialValues(context)
    }

    LaunchedEffect(viewModel) {

        // --- TEXT FIELDS ---
        launch {
            snapshotFlow { viewModel.deviceName.value }
                .drop(1)
                .collect { viewModel.updateDeviceName(it) }
        }

        launch {
            snapshotFlow { viewModel.listenAddresses.value }
                .drop(1)
                .collect { viewModel.updateSettings(newListenAddresses = it) }
        }

        launch {
            snapshotFlow { viewModel.maxRecvKbps.value }
                .drop(1)
                .collect { viewModel.updateSettings(newMaxRecv = it) }
        }

        launch {
            snapshotFlow { viewModel.maxSendKbps.value }
                .drop(1)
                .collect { viewModel.updateSettings(newMaxSend = it) }
        }

        launch {
            snapshotFlow { viewModel.globalAnnounceServers.value }
                .drop(1)
                .collect { viewModel.updateSettings(newAnnounceServers = it) }
        }

        launch {
            snapshotFlow { viewModel.guiAddress.value }
                .drop(1)
                .collect { viewModel.updateSettings(newGuiAddress = it) }
        }

        launch {
            snapshotFlow { viewModel.environmentVariables.value }
                .drop(1)
                .collect { viewModel.updateEnvironmentVariables(context) }
        }

        launch {
            snapshotFlow { viewModel.httpProxyAddress.value }
                .drop(1)
                .collect { viewModel.updateHttpProxy(context) }
        }

        launch {
            snapshotFlow { viewModel.socksProxyAddress.value }
                .drop(1)
                .collect { viewModel.updateSocksProxy(context) }
        }

        // --- BOOLEANS (SWITCHES) ---

        launch {
            snapshotFlow { viewModel.natEnabled.value }
                .drop(1)
                .collect { viewModel.updateSettings(newNatEnabled = it) }
        }

        launch {
            snapshotFlow { viewModel.localAnnounceEnabled.value }
                .drop(1)
                .collect { viewModel.updateSettings(newLocalAnnounceEnabled = it) }
        }

        launch {
            snapshotFlow { viewModel.globalAnnounceEnabled.value }
                .drop(1)
                .collect { viewModel.updateSettings(newGlobalAnnounceEnabled = it) }
        }

        launch {
            snapshotFlow { viewModel.relaysEnabled.value }
                .drop(1)
                .collect { viewModel.updateSettings(newRelaysEnabled = it) }
        }

        launch {
            snapshotFlow { viewModel.urAccepted.value }
                .drop(1)
                .collect { viewModel.updateSettings(newUrAccepted = it) }
        }
    }

    ProvidePreferenceLocals {


        LaunchedEffect(Unit) {
            snapshotFlow { viewModel.useRoot.value } // Need the .value here
                .drop(1)
                .collect {
                    viewModel.onRootChanged(context, viewModel.useRoot.value)
                }

        }
        // TODO: use snapshotFlow for values where change should be ignored on launch
        LaunchedEffect(Unit) {
            snapshotFlow { viewModel.useTor.value }
                .drop(1)
                .collect {
                    viewModel.restartSyncthing()
                }
        }



        NavHost(
            navController = navController,
            startDestination = "settings_root",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("settings_root") {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding
                ) {
                    preference(
                        key = "screen_theme",
                        title = { Text(stringResource(R.string.preference_theme_title)) },
                        onClick = { navController.navigate("theme") }
                    )
                    preference(
                        key = "screen_run_conditions",
                        title = { Text(stringResource(R.string.category_run_conditions)) },
                        onClick = { navController.navigate("run_conditions") }
                    )
                    preference(
                        key = "screen_behaviour",
                        title = { Text(stringResource(R.string.category_behaviour)) },
                        onClick = { navController.navigate("behaviour") }
                    )
                    preference(
                        key = "screen_syncthing_options",
                        title = { Text(stringResource(R.string.category_syncthing_options)) },
                        onClick = { navController.navigate("syncthing_options") }
                    )
                    preference(
                        key = "screen_backup",
                        title = { Text(stringResource(R.string.category_backup)) },
                        onClick = { navController.navigate("backup") }
                    )
                    preference(
                        key = "screen_debug",
                        title = { Text(stringResource(R.string.category_debug)) },
                        onClick = { navController.navigate("debug") }
                    )
                    preference(
                        key = "screen_experimental",
                        title = { Text(stringResource(R.string.category_experimental)) },
                        onClick = { navController.navigate("experimental") }
                    )
                    preference(
                        key = "screen_about",
                        title = { Text(stringResource(R.string.category_about)) },
                        onClick = { navController.navigate("about") }
                    )
                }
            }

            composable("theme") { Theme(contentPadding) }
            composable("run_conditions") { RunConditions(contentPadding) }
            composable("behaviour") { Behaviour(contentPadding) }
            composable("syncthing_options") { SyncthingOptions(contentPadding, viewModel) }
            composable("backup") { Backup(contentPadding, viewModel) }
            composable("debug") { Debug(contentPadding, viewModel) }
            composable("experimental") { Experimental(contentPadding, viewModel) }
            composable("about") { About(contentPadding, viewModel) }
        }
    }
}


@Composable
fun Theme(contentPadding: PaddingValues) {
    val themeNames = stringArrayResource(R.array.theme_entries)
    val themeValues = stringArrayResource(R.array.theme_values)
    val themeMap = remember { themeValues.zip(themeNames).toMap() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
    }
}

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
        modifier = Modifier.fillMaxSize(),
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
            valueToText = { value -> AnnotatedString(powerSourceMap[value] ?: value) },
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
        modifier = Modifier.fillMaxSize(),
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
                valueToText = { value -> AnnotatedString(map[value] ?: value) },
                summary = { value -> Text(map[value] ?: value) }

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

@Composable
fun SyncthingOptions(contentPadding: PaddingValues, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val showUndoDialog = remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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

@Composable
fun Backup(contentPadding: PaddingValues, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val showImportDialog = remember { mutableStateOf(false) }
    val showExportDialog = remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        preference(
            key = "export_config",
            title = {
                Text(stringResource(R.string.export_config))
            },
            onClick = { showExportDialog.value = true },
            enabled = viewModel.isApiAvailable
        )
        preference(
            key = "import_config",
            title = {
                Text(stringResource(R.string.import_config))
            },
            onClick = { showImportDialog.value = true },
            enabled = viewModel.isApiAvailable
        )
    }

    SettingsAlertDialog(
        text = stringResource(R.string.dialog_confirm_import),
        confirmAction = { viewModel.importConfig(context) },
        showDialog = showImportDialog,
    )

    SettingsAlertDialog(
        text = stringResource(R.string.dialog_confirm_export),
        confirmAction = { viewModel.exportConfig(context) },
        showDialog = showExportDialog,
    )
}

@Composable
fun Debug(contentPadding: PaddingValues, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val showDatabaseDialog = remember { mutableStateOf(false) }
    val showDeltaDialog = remember { mutableStateOf(false) }
    val sttraceState = rememberSttraceState()


    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
            onClick = { showDatabaseDialog.value = true}
        )
        preference(
            key = "st_reset_deltas",
            title = { Text(stringResource(R.string.st_reset_deltas_title)) },
            onClick = { showDeltaDialog.value = true}
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

@Composable
fun Experimental(contentPadding: PaddingValues, viewModel: SettingsViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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

@Composable
fun About(contentPadding: PaddingValues, viewModel: SettingsViewModel) {
    val uriHandler = LocalUriHandler.current
    val forumUrl = stringResource(R.string.syncthing_forum_url)
    val issuesUrl = stringResource(R.string.issue_tracker_url)
    val donateUrl = stringResource(R.string.donate_url)
    val privacyUrl = stringResource(R.string.privacy_policy_url)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        preference(
            key = "syncthing_forum",  // #noKey
            title = { Text(stringResource(R.string.syncthing_forum_title)) },
            summary = { Text(stringResource(R.string.syncthing_forum_summary)) },
            onClick = { uriHandler.openUri(forumUrl) }
        )
        preference(
            key = "report_issue",  // #noKey
            title = { Text(stringResource(R.string.report_issue_title)) },
            summary = { Text(stringResource(R.string.report_issue_summary)) },
            onClick = { uriHandler.openUri(issuesUrl) }
        )
        preference(
            key = "donate",  // #noKey
            title = { Text(stringResource(R.string.donate_title)) },
            summary = { Text(stringResource(R.string.donate_summary)) },
            onClick = { uriHandler.openUri(donateUrl) }
        )
        preference(
            key = "privacy",  // #noKey
            title = { Text(stringResource(R.string.privacy_title)) },
            summary = { Text(stringResource(R.string.privacy_summary)) },
            onClick = { uriHandler.openUri(privacyUrl) }
        )
        preference(
            key = "syncthing_version",
            title = { Text(stringResource(R.string.syncthing_version_title)) },
            summary = { Text(viewModel.syncthingVersion.value) }
        )
        preference(
            key = "app_version",
            title = { Text(stringResource(R.string.app_version_title)) },
            summary = { Text(viewModel.syncthingAppVersion.value) }
        )
    }
}




@Composable
@Preview(showBackground = true, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
fun SettingsPreview() {
    SyncthingandroidTheme {
        Settings(viewModel<SettingsViewModel>(), navController = rememberNavController())
    }
}