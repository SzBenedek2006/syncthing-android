// FIXME: Cleanup before commit
package dev.benedek.syncthingandroid.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.RestApi
import dev.benedek.syncthingandroid.service.SyncthingService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.content.edit
import dev.benedek.syncthingandroid.util.Util
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

class SettingsViewModel : ViewModel() {
    //private var service: SyncthingService? = null
    private var serviceReference: WeakReference<SyncthingService>? = null

    private var api: RestApi? = null


    var deviceName = mutableStateOf("..")
        private set
    var isServiceConnected = mutableStateOf(false)
        private set

    var listenAddresses = mutableStateOf("")
    var maxRecvKbps = mutableStateOf("0")
    var maxSendKbps = mutableStateOf("0")
    var globalAnnounceServers = mutableStateOf("")
    var guiAddress = mutableStateOf("")
    var environmentVariables = mutableStateOf("")
    var httpProxyAddress = mutableStateOf("")
    var socksProxyAddress = mutableStateOf("")
    var syncthingVersion = mutableStateOf("Couldn't get version. Is syncthing running?")
    var syncthingAppVersion = mutableStateOf("")
    var useTor = mutableStateOf(false)
    var shouldAskLocationPermission = mutableStateOf(false)

    fun loadInitialValues(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        environmentVariables.value = prefs.getString(Constants.PREF_ENVIRONMENT_VARIABLES, "")!!
        httpProxyAddress.value = prefs.getString(Constants.PREF_HTTP_PROXY_ADDRESS, "")!!
        socksProxyAddress.value = prefs.getString(Constants.PREF_SOCKS_PROXY_ADDRESS, "")!!
        val currentApi = api
        if (currentApi != null) {
            syncthingVersion.value = currentApi.version
        }
        syncthingAppVersion.value = context.packageManager.getPackageInfo(context.packageName, 0).versionName!!
        useTor.value = prefs.getBoolean(Constants.PREF_USE_TOR, false)
    }

    fun setService(boundService: SyncthingService) {
        this.serviceReference = WeakReference(boundService)
        this.api = boundService.api
        this.isServiceConnected.value = true

        // Load initial values from the API
        refreshValues()
    }

    fun restartSyncthing() {
        val currentApi = api
        currentApi?.saveConfigAndRestart()
    }


    private fun refreshValues() {
        api?.let {
            // Safe call in case api is null
            val localDevice = it.localDevice
            deviceName.value = localDevice?.name ?: ""

            val options = it.options
            if (options != null) {

                listenAddresses.value = options.listenAddresses.joinToString(", ")
                maxRecvKbps.value = options.maxRecvKbps.toString()
                maxSendKbps.value = options.maxSendKbps.toString()
                globalAnnounceServers.value = options.globalAnnounceServers.joinToString(", ")

            }
            val gui = it.gui
            if (gui != null) {
                guiAddress.value = gui.address ?: ""
            }
        }
    }
    fun updateDeviceName(newName: String) {
        val currentApi = api ?: return

        // Run on background thread
        viewModelScope.launch {
            try {
                // Get the current config object
                val device = currentApi.localDevice
                if (device != null && device.name != newName) {
                    device.name = newName
                    currentApi.editDevice(device)
                    delay(1000)
                    currentApi.saveConfigAndRestart()
                }
            } catch (e: Exception) {
                Log.d("Exception", e.toString())
            }
        }
    }

    // Generic updater for Options/GUI
    fun updateSettings(
        newListenAddresses: String? = null,
        newMaxRecv: String? = null,
        newMaxSend: String? = null,
        newAnnounceServers: String? = null,
        newGuiAddress: String? = null
    ) {
        val currentApi = api ?: return

        viewModelScope.launch {
            try {
                // Get current config
                val options = currentApi.options
                val gui = currentApi.gui

                var changed = false

                if (options != null) {
                    if (newListenAddresses != null) {
                        // Split string back into list
                        options.listenAddresses = newListenAddresses.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toTypedArray()
                        changed = true
                    }
                    if (newMaxRecv != null) {
                        options.maxRecvKbps = newMaxRecv.toIntOrNull() ?: 0
                        changed = true
                    }
                    if (newMaxSend != null) {
                        options.maxSendKbps = newMaxSend.toIntOrNull() ?: 0
                        changed = true
                    }
                    if (newAnnounceServers != null) {
                        options.globalAnnounceServers = newAnnounceServers.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toTypedArray()
                        changed = true
                    }
                }

                if (gui != null && newGuiAddress != null) {
                    gui.address = newGuiAddress
                    changed = true
                }

                if (changed) {
                    // Save both objects
                    currentApi.editSettings(gui, options)
                    delay(1000)
                    currentApi.saveConfigAndRestart()
                }
            } catch (e: Exception) { Log.d("SettingsViewModel", e.toString()) }
        }
    }

    fun resetIgnored(context: Context) {

        val currentApi: RestApi? = api
        if (currentApi == null) { // Todo: Make this null check instead of return in other functions too.
            Toast.makeText(
                context,
                context.getString(R.string.generic_error) + context.getString(R.string.syncthing_disabled_title),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            viewModelScope.launch {
                try {
                    currentApi.undoIgnoredDevicesAndFolders()
                    currentApi.saveConfigAndRestart()

                    // TODO: Learn about vms.launch then delete this comment
                    //  Toast is safe here because viewModelScope.launch defaults to Main thread for updates
                    Toast.makeText(
                        context,
                        context.getString(R.string.undo_ignored_devices_folders_done),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "Reset ignored failed", e)
                }
            }
        }
    }

    fun importConfig(context: Context) {
        val currentService = serviceReference?.get()
        if (currentService == null) { // Fail
            Toast.makeText(
                context,
                context.getString(R.string.generic_error) + context.getString(R.string.syncthing_disabled_title),
                Toast.LENGTH_SHORT
            ).show()
        } else if (currentService.importConfig()) { // Success
            Toast.makeText(
                context,
                context.getString(R.string.config_imported_successful),
                Toast.LENGTH_SHORT
            ).show()
        } else  { // Fail
            Toast.makeText(
                context,
                context.getString(R.string.config_import_failed),
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    fun exportConfig(context: Context) {
        val currentService = serviceReference?.get()
        if (currentService == null) { // Todo: Make this null check instead of return in other functions too.
            Toast.makeText(
                context,
                context.getString(R.string.generic_error) + context.getString(R.string.syncthing_disabled_title),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            currentService.exportConfig()

            Toast.makeText(
                context,
                context.getString(R.string.config_export_successful, Constants.EXPORT_PATH),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun updateEnvironmentVariables(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        if (environmentVariables.value.matches("^(\\w+=[\\w:/.]+)?( \\w+=[\\w:/.]+)*$".toRegex())) {
            prefs.edit { putString(Constants.PREF_ENVIRONMENT_VARIABLES, environmentVariables.value) }

            viewModelScope.launch {
                try {
                    api?.saveConfigAndRestart()
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "EnvVars restart failed", e)
                }
            }
        } else {
            Toast.makeText(
                context,
                R.string.toast_invalid_environment_variables,
                Toast.LENGTH_SHORT
            ).show()
            environmentVariables.value = prefs.getString(Constants.PREF_ENVIRONMENT_VARIABLES, "")!!
        }
    }

    fun resetDatabase(context: Context) {
        val intent = Intent(context, SyncthingService::class.java).setAction(SyncthingService.ACTION_RESET_DATABASE)
        context.startService(intent)
        Toast.makeText(
            context,
            R.string.st_reset_database_done,
            Toast.LENGTH_LONG
        ).show()
    }

    fun resetDeltas(context: Context) {
        val intent = Intent(context, SyncthingService::class.java).setAction(SyncthingService.ACTION_RESET_DELTAS)
        context.startService(intent)
        Toast.makeText(
            context,
            R.string.st_reset_deltas_done,
            Toast.LENGTH_LONG
        ).show()
    }


    fun onRootChanged(context: Context, enabled: Boolean) {
        if (enabled) {
            // Check if Root is actually available
            viewModelScope.launch(Dispatchers.IO) {
                val hasRoot = Shell.SU.available()
                if (!hasRoot) {
                    // Failed: Turn switch back OFF and show Toast
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, R.string.toast_root_denied, Toast.LENGTH_SHORT).show()
                        PreferenceManager.getDefaultSharedPreferences(context)
                            .edit { putBoolean(Constants.PREF_USE_ROOT, false) }
                    }
                } else {
                    // Success: Restart to apply
                    api?.saveConfigAndRestart()
                }
            }
        } else {
            // Root disabled: Fix permissions and restart
            viewModelScope.launch(Dispatchers.IO) {
                Util.fixAppDataPermissions(context)
                api?.saveConfigAndRestart()
            }
        }
    }




    fun updateSocksProxy(context: Context) {
        val input = socksProxyAddress.value.trim()
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val currentApi = api
        if (currentApi != null) {
            if (input.isEmpty() || input.matches("^socks5://.*:\\d{1,5}$".toRegex())) {

                prefs.edit { putString(Constants.PREF_SOCKS_PROXY_ADDRESS, input) }

                viewModelScope.launch {
                    try {
                        currentApi.saveConfigAndRestart()
                    } catch (e: Exception) {
                        Log.e("SettingsViewModel", "Failed to restart: $e")
                    }
                }
            }
            else {
                // If value is incorrect, reset the viewmodel's variable
                socksProxyAddress.value = prefs.getString(Constants.PREF_SOCKS_PROXY_ADDRESS, "")!!
                Toast.makeText(
                    context,
                    R.string.toast_invalid_socks_proxy_address,
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else  {
            Toast.makeText(
                context,
                context.getString(R.string.generic_error) + context.getString(R.string.syncthing_disabled_title),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun updateHttpProxy(context: Context) {
        val input = httpProxyAddress.value.trim()
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val currentApi = api
        if (currentApi != null) {
            if (input.isEmpty() || input.matches("^http://.*:\\d{1,5}$".toRegex())) {

                prefs.edit { putString(Constants.PREF_HTTP_PROXY_ADDRESS, input) }

                viewModelScope.launch {
                    try {
                        currentApi.saveConfigAndRestart()
                    } catch (e: Exception) {
                        Log.e("SettingsViewModel", "Failed to restart: $e")
                    }
                }
            }
            else {
                httpProxyAddress.value = prefs.getString(Constants.PREF_HTTP_PROXY_ADDRESS, "")!!
                Toast.makeText(
                    context,
                    R.string.toast_invalid_http_proxy_address,
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else  {
            Toast.makeText(
                context,
                context.getString(R.string.generic_error) + context.getString(R.string.syncthing_disabled_title),
                Toast.LENGTH_SHORT
            ).show()
        }
    }


}