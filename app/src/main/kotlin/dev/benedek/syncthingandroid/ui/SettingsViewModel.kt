package dev.benedek.syncthingandroid.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.model.Options
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.RestApi
import dev.benedek.syncthingandroid.service.SyncthingService
import dev.benedek.syncthingandroid.util.Util
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

class SettingsViewModel : ViewModel() {

    private var serviceReference: WeakReference<SyncthingService>? = null
    private var api: RestApi? = null

    private var cachedUrVersionMax = 0

    var isServiceConnected = mutableStateOf(false)
        private set
    var isApiAvailable = mutableStateOf(false)
        private set


    // Syncthing Options (Strings)
    var deviceName = mutableStateOf("")
    var listenAddresses = mutableStateOf("")
    var globalAnnounceServers = mutableStateOf("")
    var guiAddress = mutableStateOf("")
    var maxRecvKbps = mutableStateOf("0")
    var maxSendKbps = mutableStateOf("0")

    // Syncthing Options (Booleans)
    var natEnabled = mutableStateOf(false)
    var localAnnounceEnabled = mutableStateOf(false)
    var globalAnnounceEnabled = mutableStateOf(false)
    var relaysEnabled = mutableStateOf(false)
    var urAccepted = mutableStateOf(false)

    // Android/App Settings
    var environmentVariables = mutableStateOf("")
    var httpProxyAddress = mutableStateOf("")
    var socksProxyAddress = mutableStateOf("")
    var useTor = mutableStateOf(false)
    var useRoot = mutableStateOf(false)

    // Info
    var syncthingVersion = mutableStateOf("Couldn't get version. Is syncthing running?")
    var syncthingAppVersion = mutableStateOf("")


    fun loadInitialValues(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        environmentVariables.value = prefs.getString(Constants.PREF_ENVIRONMENT_VARIABLES, "")!!
        httpProxyAddress.value = prefs.getString(Constants.PREF_HTTP_PROXY_ADDRESS, "")!!
        socksProxyAddress.value = prefs.getString(Constants.PREF_SOCKS_PROXY_ADDRESS, "")!!
        useTor.value = prefs.getBoolean(Constants.PREF_USE_TOR, false)
        useRoot.value = prefs.getBoolean(Constants.PREF_USE_ROOT, false)

        val currentApi = api
        if (currentApi != null) {
            syncthingVersion.value = currentApi.version
        }
        syncthingAppVersion.value = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    }

    fun setService(boundService: SyncthingService) {
        this.serviceReference = WeakReference(boundService)
        this.api = boundService.api
        this.isServiceConnected.value = true
        refreshValues()
    }

    private fun refreshValues() {
        val currentApi = api

        isApiAvailable.value = currentApi != null
        if (currentApi == null) return

        currentApi.getSystemInfo { info ->
            cachedUrVersionMax = info.urVersionMax

            val options = currentApi.options
            if (options != null) {
                urAccepted.value = options.isUsageReportingAccepted(cachedUrVersionMax)
            }
        }

        val localDevice = currentApi.localDevice
        deviceName.value = localDevice?.name ?: ""

        val options = currentApi.options
        if (options != null) {
            listenAddresses.value = options.listenAddresses.joinToString(", ")
            globalAnnounceServers.value = options.globalAnnounceServers.joinToString(", ")
            maxRecvKbps.value = options.maxRecvKbps.toString()
            maxSendKbps.value = options.maxSendKbps.toString()

            natEnabled.value = options.natEnabled
            localAnnounceEnabled.value = options.localAnnounceEnabled
            globalAnnounceEnabled.value = options.globalAnnounceEnabled
            relaysEnabled.value = options.relaysEnabled
        }

        val gui = currentApi.gui
        if (gui != null) {
            guiAddress.value = gui.address ?: ""
        }
    }

    fun updateDeviceName(newName: String) {
        val currentApi = api ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val device = currentApi.localDevice
                if (device != null && device.name != newName) {
                    device.name = newName
                    currentApi.editDevice(device)
                    delay(1000)
                    currentApi.saveConfigAndRestart()
                }
            } catch (e: Exception) {
                Log.d("SettingsViewModel", e.toString())
            }
        }
    }

    fun updateSettings(
        newListenAddresses: String? = null,
        newMaxRecv: String? = null,
        newMaxSend: String? = null,
        newAnnounceServers: String? = null,
        newGuiAddress: String? = null,
        newNatEnabled: Boolean? = null,
        newLocalAnnounceEnabled: Boolean? = null,
        newGlobalAnnounceEnabled: Boolean? = null,
        newRelaysEnabled: Boolean? = null,
        newUrAccepted: Boolean? = null
    ) {
        val currentApi = api ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val options = currentApi.options
                val gui = currentApi.gui
                var changed = false

                if (options != null) {
                    if (newListenAddresses != null) {
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
                    if (newNatEnabled != null) {
                        options.natEnabled = newNatEnabled
                        changed = true
                    }
                    if (newLocalAnnounceEnabled != null) {
                        options.localAnnounceEnabled = newLocalAnnounceEnabled
                        changed = true
                    }
                    if (newGlobalAnnounceEnabled != null) {
                        options.globalAnnounceEnabled = newGlobalAnnounceEnabled
                        changed = true
                    }
                    if (newRelaysEnabled != null) {
                        options.relaysEnabled = newRelaysEnabled
                        changed = true
                    }
                    if (newUrAccepted != null) {
                        options.urAccepted = if (newUrAccepted) cachedUrVersionMax else Options.USAGE_REPORTING_DENIED
                        changed = true
                    }
                }

                if (gui != null && newGuiAddress != null) {
                    gui.address = newGuiAddress
                    changed = true
                }

                if (changed) {
                    currentApi.editSettings(gui, options)
                    delay(1000)
                    currentApi.saveConfigAndRestart()
                }
            } catch (e: Exception) {
                Log.d("SettingsViewModel", e.toString())
            }
        }
    }

    fun refreshRunConditionsAndNotifications() {
        val service = serviceReference?.get() ?: return
        service.evaluateRunConditions()
        service.notificationHandler.updatePersistentNotification(service)
    }

    fun restartSyncthing() {
        val currentApi = api ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                currentApi.saveConfigAndRestart()
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Restart failed", e)
            }
        }
    }

    fun onRootChanged(context: Context, enabled: Boolean) {
        useRoot.value = enabled

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        if (enabled) {
            viewModelScope.launch(Dispatchers.IO) {
                val hasRoot = Shell.SU::available
                if (!hasRoot()) {
                    withContext(Dispatchers.Main) {
                        delay(150)
                        useRoot.value = false
                        Toast.makeText(context, R.string.toast_root_denied, Toast.LENGTH_SHORT).show()
                        prefs.edit { putBoolean(Constants.PREF_USE_ROOT, false) }
                    }
                } else {
                    prefs.edit { putBoolean(Constants.PREF_USE_ROOT, true) }
                    api?.saveConfigAndRestart()
                }
            }
        } else {
            prefs.edit { putBoolean(Constants.PREF_USE_ROOT, false) }
            viewModelScope.launch(Dispatchers.IO) {
                Util.fixAppDataPermissions(context)
                api?.saveConfigAndRestart()
            }
        }
    }

    fun updateEnvironmentVariables(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val input = environmentVariables.value

        if (input.matches("^(\\w+=[\\w:/.]+)?( \\w+=[\\w:/.]+)*$".toRegex())) {
            prefs.edit { putString(Constants.PREF_ENVIRONMENT_VARIABLES, input) }
            restartSyncthing()
        } else {
            Toast.makeText(context, R.string.toast_invalid_environment_variables, Toast.LENGTH_SHORT).show()
            environmentVariables.value = prefs.getString(Constants.PREF_ENVIRONMENT_VARIABLES, "") ?: ""
        }
    }

    fun updateSocksProxy(context: Context) {
        val input = socksProxyAddress.value.trim()
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        if (input.isEmpty() || input.matches("^socks5://.*:\\d{1,5}$".toRegex())) {
            prefs.edit { putString(Constants.PREF_SOCKS_PROXY_ADDRESS, input) }
            restartSyncthing()
        } else {
            socksProxyAddress.value = prefs.getString(Constants.PREF_SOCKS_PROXY_ADDRESS, "") ?: ""
            Toast.makeText(context, R.string.toast_invalid_socks_proxy_address, Toast.LENGTH_SHORT).show()
        }
    }

    fun updateHttpProxy(context: Context) {
        val input = httpProxyAddress.value.trim()
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        if (input.isEmpty() || input.matches("^http://.*:\\d{1,5}$".toRegex())) {
            prefs.edit { putString(Constants.PREF_HTTP_PROXY_ADDRESS, input) }
            restartSyncthing()
        } else {
            httpProxyAddress.value = prefs.getString(Constants.PREF_HTTP_PROXY_ADDRESS, "") ?: ""
            Toast.makeText(context, R.string.toast_invalid_http_proxy_address, Toast.LENGTH_SHORT).show()
        }
    }

    fun resetIgnored(context: Context) {
        val currentApi = api
        if (currentApi == null) {
            Toast.makeText(context, context.getString(R.string.generic_error) + context.getString(R.string.syncthing_disabled_title), Toast.LENGTH_SHORT).show()
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    currentApi.undoIgnoredDevicesAndFolders()
                    currentApi.saveConfigAndRestart()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.undo_ignored_devices_folders_done), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "Reset ignored failed", e)
                }
            }
        }
    }

    fun resetDatabase(context: Context) {
        val intent = Intent(context, SyncthingService::class.java).setAction(SyncthingService.ACTION_RESET_DATABASE)
        context.startService(intent)
        Toast.makeText(context, R.string.st_reset_database_done, Toast.LENGTH_LONG).show()
    }

    fun resetDeltas(context: Context) {
        val intent = Intent(context, SyncthingService::class.java).setAction(SyncthingService.ACTION_RESET_DELTAS)
        context.startService(intent)
        Toast.makeText(context, R.string.st_reset_deltas_done, Toast.LENGTH_LONG).show()
    }

    fun importConfig(context: Context) {
        val currentService = serviceReference?.get()
        viewModelScope.launch(Dispatchers.IO) {
            val result = currentService?.importConfig() == true
            withContext(Dispatchers.Main) {
                if (currentService == null) {
                    Toast.makeText(context, context.getString(R.string.generic_error) + context.getString(R.string.syncthing_disabled_title), Toast.LENGTH_SHORT).show()
                } else if (result) {
                    Toast.makeText(context, context.getString(R.string.config_imported_successful), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.config_import_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun exportConfig(context: Context) {
        val currentService = serviceReference?.get()
        viewModelScope.launch(Dispatchers.IO) {
            if (currentService == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.generic_error) + context.getString(R.string.syncthing_disabled_title), Toast.LENGTH_SHORT).show()
                }
            } else {
                currentService.exportConfig()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.config_export_successful, Constants.EXPORT_PATH), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}