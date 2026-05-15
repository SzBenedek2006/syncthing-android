package dev.benedek.syncthingandroid.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import dev.benedek.syncthingandroid.BuildConfig
import dev.benedek.syncthingandroid.activities.ShareActivity
import dev.benedek.syncthingandroid.http.GetRequest
import dev.benedek.syncthingandroid.http.PostConfigRequest
import dev.benedek.syncthingandroid.http.PostRequest
import dev.benedek.syncthingandroid.model.Completion
import dev.benedek.syncthingandroid.model.CompletionInfo
import dev.benedek.syncthingandroid.model.Config
import dev.benedek.syncthingandroid.model.Config.Gui
import dev.benedek.syncthingandroid.model.DeviceStatuses
import dev.benedek.syncthingandroid.model.Device
import dev.benedek.syncthingandroid.model.Event
import dev.benedek.syncthingandroid.model.Folder
import dev.benedek.syncthingandroid.model.FolderStatus
import dev.benedek.syncthingandroid.model.IgnoredFolder
import dev.benedek.syncthingandroid.model.Options
import dev.benedek.syncthingandroid.model.RemoteIgnoredDevice
import dev.benedek.syncthingandroid.model.SystemInfo
import dev.benedek.syncthingandroid.model.SystemVersion
import java.lang.reflect.Type
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.edit
import dev.benedek.syncthingandroid.activities.FolderActivity

/**
 * Provides functions to interact with the syncthing REST API.
 */
class RestApi(
    private val context: Context,
    val url: URL,
    private val apiKey: String,
    private val onApiAvailableListener: () -> Unit,
    private val onConfigChangedListener: () -> Unit
) {

    /**
     * Returns the version name, or a (text) error message on failure.
     */
    var version: String? = null
        private set
    private var config: Config? = null

    /**
     * Results cached from systemInfo
     */
    private var localDeviceId: String? = null
    private var urVersionMax: Int? = null

    /**
     * Stores the result of the last successful request to [GetRequest.URI_CONNECTIONS],
     * or an empty Map.
     */
    private var previousDeviceStatuses: DeviceStatuses? = null

    /**
     * Stores the timestamp of the last successful request to [GetRequest.URI_CONNECTIONS].
     */
    private var previousConnectionTime: Long = 0

    /**
     * In the last-finishing [.readConfigFromRestApi] callback, we have to call
     * [SyncthingService.onApiAvailable] to indicate that the RestApi class is fully initialized.
     * We do this to avoid getting stuck with our main thread due to synchronous REST queries.
     * The correct indication of full initialization is crucial to stability as other listeners of
     * [SyncthingService.onServiceStateChange] needs cached config and system information available.
     * e.g. SettingsFragment need "localDeviceId"
     */
    private var asyncQueryConfigComplete = false
    private var asyncQueryVersionComplete = false
    private var asyncQuerySystemInfoComplete = false

    /**
     * Object that must be locked upon accessing the following variables:
     * asyncQueryConfigComplete, asyncQueryVersionComplete, asyncQuerySystemInfoComplete
     */
    private val asyncQueryCompleteLock = Any()

    /**
     * Object that must be locked upon accessing config
     */
    private val configLock = Any()

    /**
     * Stores the latest result of [.getFolderStatus] for each folder
     */
    private val cachedFolderStatuses =
        HashMap<String?, FolderStatus?>() // For some reason it's not used

    /**
     * Stores the latest result of device and folder completion events.
     */
    private val completion = Completion()


    val notificationHandler: NotificationHandler by lazy { NotificationHandler(context) }


    /**
     * Gets local device ID, syncthing version and config, then calls all OnApiAvailableListeners.
     */
    fun readConfigFromRestApi() {
        Log.v(TAG, "Reading config from REST ...")
        synchronized(asyncQueryCompleteLock) {
            asyncQueryVersionComplete = false
            asyncQueryConfigComplete = false
            asyncQuerySystemInfoComplete = false
        }
        GetRequest(context, this.url, GetRequest.URI_VERSION, apiKey, null) { result ->
            val json = JsonParser.parseString(result).getAsJsonObject()
            this.version = json.get("version").asString
            Log.i(TAG, "Syncthing version is " + this.version)

            //updateDebugFacilitiesCache();
            synchronized(asyncQueryCompleteLock) {
                asyncQueryVersionComplete = true
                checkReadConfigFromRestApiCompleted()
            }
        }

        GetRequest(context, this.url, GetRequest.URI_CONFIG, apiKey, null) { result ->
            onReloadConfigComplete(result)
            synchronized(asyncQueryCompleteLock) {
                asyncQueryConfigComplete = true
                checkReadConfigFromRestApiCompleted()
            }
        }

        getSystemInfo { info: SystemInfo? ->
            localDeviceId = info?.myID
            urVersionMax = info?.urVersionMax
            synchronized(asyncQueryCompleteLock) {
                asyncQuerySystemInfoComplete = true
                checkReadConfigFromRestApiCompleted()
            }
        }
    }

    fun checkReadConfigFromRestApiCompleted() {
        if (asyncQueryVersionComplete && asyncQueryConfigComplete && asyncQuerySystemInfoComplete) {
            Log.v(TAG, "Reading config from REST completed.")
            onApiAvailableListener()
        }
    }

    fun reloadConfig() {
        GetRequest(
            context,
            this.url,
            GetRequest.URI_CONFIG,
            apiKey,
            null
        ) { result: String? -> this.onReloadConfigComplete(result) }
    }

    private fun onReloadConfigComplete(result: String?) {
        val configParseSuccess: Boolean
        synchronized(configLock) {
            config = Gson().fromJson(result, Config::class.java)
            configParseSuccess = config != null
        }
        if (!configParseSuccess) {
            throw RuntimeException("config is null: $result")
        }
        Log.v(TAG, "onReloadConfigComplete: Successfully parsed configuration.")
        if (BuildConfig.DEBUG) {
            Log.v(
                TAG,
                "config.remoteIgnoredDevices = " + Gson().toJson(config?.remoteIgnoredDevices)
            )
        }

        // Update cached device and folder information stored in the completion model.
        completion.updateFromConfig(getDevices(true), this.folders)
    }

    /**
     * Queries debug facilities available from the currently running syncthing binary
     * if the syncthing binary version changed. First launch of the binary is also
     * considered as a version change.
     * Precondition: [.version] read from REST
     * 
     * 
     * It's not possible as of 2.0, so always falling back to the hardcoded list.
     */

    /**
     * Permanently ignore a device when it tries to connect.
     * Ignored devices will not trigger the "DeviceRejected" event
     * in [EventProcessor.onEvent].
     */
    fun ignoreDevice(deviceId: String, deviceName: String?, deviceAddress: String?) {
        synchronized(configLock) {
            // Check if the device has already been ignored.

            val devices = config?.remoteIgnoredDevices ?: return

            if (devices.any { it?.deviceID == deviceId }) {
                // Device already ignored.
                Log.d(TAG, "Device already ignored [$deviceId]")
                return
            }



            val remoteIgnoredDevice = RemoteIgnoredDevice()
            remoteIgnoredDevice.deviceID = deviceId
            remoteIgnoredDevice.address = deviceAddress.toString()
            remoteIgnoredDevice.name = deviceName.toString()
            remoteIgnoredDevice.time = dateFormat.format(Date())
            config?.remoteIgnoredDevices?.add(remoteIgnoredDevice)
            sendConfig()
            Log.d(TAG, "Ignored device [$deviceId]")
        }
    }

    /**
     * Permanently ignore a folder share request.
     * Ignored folders will not trigger the "FolderRejected" event
     * in [EventProcessor.onEvent].
     */
    fun ignoreFolder(deviceId: String, folderId: String, folderLabel: String?) {
        synchronized(configLock) {
            val device = config?.devices?.find { it?.deviceID == deviceId } ?: return

            //Check if the folder has already been ignored.
            if (device.ignoredFolders?.any { it?.id == folderId } == true) {
                // Folder already ignored.
                Log.d(TAG, "Folder [$folderId] already ignored on device [$deviceId]")
                return
            }

            /*
              Ignore folder by moving its corresponding "pendingFolder" entry to
              a newly created "ignoredFolder" entry.
             */
            device.ignoredFolders?.add(IgnoredFolder().apply {
                this.id = folderId
                this.label = folderLabel.toString()
                this.time = dateFormat.format(Date())
            })

            if (BuildConfig.DEBUG) {
                Log.v(TAG, "device.ignoredFolders = ${Gson().toJson(device.ignoredFolders)}")
            }
            sendConfig()
            Log.d(TAG, "Ignored folder [$folderId] announced by device [$deviceId]")


        }
    }

    /**
     * Undo ignoring devices and folders.
     */
    fun undoIgnoredDevicesAndFolders() {
        Log.d(TAG, "Undo ignoring devices and folders ...")
        synchronized(configLock) {
            config?.let { config ->
                config.remoteIgnoredDevices?.clear()
                config.devices?.forEach { device -> device?.ignoredFolders?.clear() }
            }
        }
    }

    /**
     * Override folder changes. This is the same as hitting
     * the "override changes" button from the web UI.
     */
    fun overrideChanges(folderId: String) {
        Log.d(TAG, "overrideChanges '$folderId'")
        PostRequest(
            context,
            this.url,
            PostRequest.URI_DB_OVERRIDE,
            apiKey,
            mutableMapOf("folder" to folderId), null
        )
    }

    /**
     * Sends current config to Syncthing.
     * Will result in a "ConfigSaved" event.
     * EventProcessor will trigger this.reloadConfig().
     */
    private fun sendConfig() {
        val jsonConfig: String?
        synchronized(configLock) {
            jsonConfig = Gson().toJson(config)
        }
        PostConfigRequest(context, this.url, apiKey, jsonConfig, null)
        onConfigChangedListener()
    }

    /**
     * Sends current config and restarts Syncthing.
     */
    fun saveConfigAndRestart() {
        val jsonConfig: String?
        synchronized(configLock) {
            jsonConfig = Gson().toJson(config)
        }
        PostConfigRequest(
            context,
            this.url,
            apiKey,
            jsonConfig
        ) { _: String? ->
            val intent = Intent(context, SyncthingService::class.java)
                .setAction(SyncthingService.ACTION_RESTART)
            context.startService(intent)
        }
        onConfigChangedListener()
    }

    fun shutdown() {
        notificationHandler.cancelRestartNotification()
    }

    val folders: MutableList<Folder?>? get() {
        synchronized(configLock) {
            val folders = config?.folders ?: return null
            return deepCopy(folders, object : TypeToken<MutableList<Folder?>>() {}.type.apply {
                folders.sortWith(FOLDERS_COMPARATOR)
            })

        }
    }

    /**
     * This is only used for new folder creation, see [FolderActivity].
     */
    fun createFolder(folder: Folder?) {
        synchronized(configLock) {
            // Add the new folder to the model.
            config?.folders?.add(folder)
            // Send model changes to syncthing, does not require a restart.
            sendConfig()
        }
    }

    fun updateFolder(newFolder: Folder) {
        synchronized(configLock) {
            removeFolderInternal(newFolder.id)
            config?.folders?.add(newFolder)
            sendConfig()
        }
    }

    fun removeFolder(id: String?) {
        synchronized(configLock) {
            removeFolderInternal(id)
            // completion will be updated after the ConfigSaved event.
            sendConfig()
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            remove(ShareActivity.PREF_FOLDER_SAVED_SUBDIRECTORY + id)
        }
    }

    private fun removeFolderInternal(id: String?) {
        synchronized(configLock) {
            config?.folders?.removeAll { it?.id == id }

//            val it = config!!.folders.iterator()
//            while (it.hasNext()) {
//                val f = it.next()
//                if (f?.id == id) {
//                    it.remove()
//                    break
//                }
//            }
        }
    }

    /**
     * Returns a list of all existing devices.
     * 
     * @param includeLocal True if the local device should be included in the result.
     */
    fun getDevices(includeLocal: Boolean): MutableList<Device>? {
        val devices: MutableList<Device> = synchronized(configLock) {
            val currentDevices = config?.devices ?: return null

            deepCopy(currentDevices, object : TypeToken<MutableList<Device?>>() {}.type)
                ?.filterNotNull()?.toMutableList() ?: return null
        }

        if (!includeLocal) {
            devices.removeAll { it.deviceID == localDeviceId }
        }
        return devices
    }

    val localDevice: Device?
        get() {
            val devices = getDevices(true)
            if (devices.isNullOrEmpty()) {
                throw RuntimeException("RestApi.getLocalDevice: devices is empty.")
            }

            Log.v(TAG, "getLocalDevice: Looking for local device ID $localDeviceId")

            return try {
                devices.first { it.deviceID == localDeviceId }
            } catch (_: NoSuchElementException) {
                throw RuntimeException("RestApi.getLocalDevice: Failed to get the local device crucial to continuing execution.")
            }
        }

    fun addDevice(device: Device, errorListener: (String?) -> Unit) {
        if (device.deviceID != null) {
            normalizeDeviceId(
                device.deviceID!!,
                { _: String? ->
                    synchronized(configLock) {
                        config!!.devices?.add(device)
                        sendConfig()
                    }
                },
                errorListener
            )
        }
    }

    fun editDevice(newDevice: Device) {
        synchronized(configLock) {
            removeDeviceInternal(newDevice.deviceID)
            config?.devices?.add(newDevice)
            sendConfig()
        }
    }

    fun removeDevice(deviceId: String?) {
        synchronized(configLock) {
            removeDeviceInternal(deviceId)
            // completion will be updated after the ConfigSaved event.
            sendConfig()
        }
    }

    private fun removeDeviceInternal(deviceId: String?) {
        synchronized(configLock) {
            config?.devices?.removeAll { it?.deviceID == deviceId }

//            val it = config!!.devices.iterator()
//            while (it.hasNext()) {
//                val d = it.next()
//                if (d.deviceID == deviceId) {
//                    it.remove()
//                    break
//                }
//            }
        }
    }

    val options: Options?
        get() {
            synchronized(configLock) {
                return deepCopy<Options?>(
                    config?.options,
                    Options::class.java
                )
            }
        }

    val gui: Gui?
        get() {
            synchronized(configLock) {
                return deepCopy<Gui?>(config?.gui, Gui::class.java)
            }
        }

    fun editSettings(newGui: Gui?, newOptions: Options?) {
        synchronized(configLock) {
            config?.gui = newGui
            config?.options = newOptions
        }
    }

    /**
     * Returns a deep copy of object.
     * 
     * 
     * This method uses Gson and only works with objects that can be converted with Gson.
     */
    private fun <T> deepCopy(`object`: T?, type: Type): T? {
        val gson = Gson()
        return gson.fromJson<T?>(gson.toJson(`object`, type), type)
    }

    /**
     * Requests and parses information about current system status and resource usage.
     */
    fun getSystemInfo(listener: (SystemInfo?) -> Unit) {
        GetRequest(
            context,
            this.url, GetRequest.URI_SYSTEM, apiKey, null
        ) { result: String? ->
            listener(
                Gson().fromJson(result, SystemInfo::class.java)
            )
        }
    }

    val isConfigLoaded: Boolean
        get() {
            synchronized(configLock) {
                return config != null
            }
        }

    /**
     * Requests and parses system version information.
     */
    fun getSystemVersion(listener: (SystemVersion?) -> Unit) {
        GetRequest(
            context,
            this.url,
            GetRequest.URI_VERSION,
            apiKey,
            null
        ) { result: String? ->
            val systemVersion =
                Gson().fromJson(result, SystemVersion::class.java)
            listener(systemVersion)
        }
    }

    /**
     * Returns connection info for the local device and all connected devices.
     */
    fun getConnections(listener: (DeviceStatuses?) -> Unit) {
        GetRequest(context, this.url, GetRequest.URI_CONNECTIONS, apiKey, null) { result: String? ->
            val now = System.currentTimeMillis()
            val msElapsed = now - previousConnectionTime

            if (msElapsed < Constants.GUI_UPDATE_INTERVAL && previousDeviceStatuses != null) {
                listener(deepCopy(previousDeviceStatuses, DeviceStatuses::class.java))
                return@GetRequest
            }

            previousConnectionTime = now
            val deviceStatuses = Gson().fromJson(result, DeviceStatuses::class.java)


            deviceStatuses.connectionsMap?.forEach { (key, value) ->
                value?.completion = completion.getDeviceCompletion(key)
                val prev = previousDeviceStatuses?.connectionsMap?.get(key) ?: DeviceStatuses.DeviceStatus()
                value?.setTransferRate(prev, msElapsed)
            }

            val prevTotal = previousDeviceStatuses?.total ?: DeviceStatuses.DeviceStatus()
            deviceStatuses.total?.setTransferRate(prevTotal, msElapsed)
            previousDeviceStatuses = deviceStatuses

            listener(deepCopy(deviceStatuses, DeviceStatuses::class.java))
        }
    }

    /**
     * Returns status information about the folder with the given id.
     */
    fun getFolderStatus(folderId: String, listener: (String?, FolderStatus?) -> Unit) {
        GetRequest(
            context,
            this.url,
            GetRequest.URI_STATUS,
            apiKey,
            mutableMapOf("folder" to folderId)
        ) { result: String? ->
            val m = Gson().fromJson(result, FolderStatus::class.java)
            cachedFolderStatuses[folderId] = m
            listener(folderId, m)
        }
    }

    /**
     * Listener for [.getEvents].
     */
    interface OnReceiveEventListener {
        /**
         * Called for each event.
         */
        fun onEvent(event: Event?)

        /**
         * Called after all available events have been processed.
         * @param lastId The id of the last event processed. Should be used as a starting point for
         * the next round of event processing.
         */
        fun onDone(lastId: Long)
    }

    /**
     * Retrieves the events that have accumulated since the given event id.
     * 
     * 
     * The OnReceiveEventListeners onEvent method is called for each event.
     */
    fun getEvents(sinceId: Long, limit: Long, listener: OnReceiveEventListener) {
        val params = mutableMapOf<String?, String?>(
            "since" to sinceId.toString(),
            "limit" to limit.toString()
        )
        GetRequest(
            context,
            this.url,
            GetRequest.URI_EVENTS,
            apiKey,
            params
        ) { result: String? ->
            val jsonEvents = JsonParser.parseString(result).getAsJsonArray()
            var lastId: Long = 0

            for (json in jsonEvents) {
                val event = Gson().fromJson(json, Event::class.java)

                if (lastId < event.id) lastId = event.id.toLong()

                listener.onEvent(event)
            }
            listener.onDone(lastId)
        }
    }

    /**
     * Normalizes a given device ID.
     */
    private fun normalizeDeviceId(
        id: String, listener: (String?) -> Unit,
        errorListener: (String?) -> Unit
    ) {
        GetRequest(
            context,
            this.url, GetRequest.URI_DEVICEID, apiKey,
            mutableMapOf("id" to id)
        ) { result: String? ->
            val json = JsonParser.parseString(result).getAsJsonObject()
            val normalizedId = json.get("id")
            val error = json.get("error")
            if (normalizedId != null) listener(normalizedId.asString)
            if (error != null) errorListener(error.asString)
        }
    }


    /**
     * Updates cached folder and device completion info according to event data.
     */
    fun setCompletionInfo(deviceId: String?, folderId: String?, completionInfo: CompletionInfo?) {
        completion.setCompletionInfo(deviceId, folderId, completionInfo)
    }

    /**
     * Returns prettified usage report.
     */
    fun getUsageReport(listener: (String?) -> Unit) {
        GetRequest(
            context,
            this.url,
            GetRequest.URI_REPORT,
            apiKey,
            null
        ) { result: String? ->
            val json = JsonParser.parseString(result)
            val gson = GsonBuilder().setPrettyPrinting().create()
            listener(gson.toJson(json))
        }
    }

    // FIXME
    val isUsageReportingDecided: Boolean
        get() {
            val options = this.options
            if (options == null) {
                Log.e(
                    TAG,
                    "isUsageReportingDecided called while options == null"
                )
                return true
            }
            return options.isUsageReportingDecided(urVersionMax ?: 0)
        }

    fun setUsageReporting(acceptUsageReporting: Boolean) {
        options?.let {
            it.urAccepted = if (acceptUsageReporting) urVersionMax ?: 0 else Options.USAGE_REPORTING_DENIED
            synchronized(configLock) {
                config?.options = options
            }
            return
        }
        Log.e(TAG, "setUsageReporting called while options == null")
    }

    companion object {
        private const val TAG = "RestApi"

        private val dateFormat: SimpleDateFormat = if (Build.VERSION.SDK_INT < 24) {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        } else {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        }

        /**
         * Compares folders by labels, uses the folder ID as fallback if the label is empty
         */
        private val FOLDERS_COMPARATOR = Comparator<Folder?> { lhs, rhs ->
            val lhsLabel =
                if (!lhs?.label.isNullOrEmpty()) lhs.label else lhs?.id
            val rhsLabel =
                if (!rhs?.label.isNullOrEmpty()) rhs.label else rhs?.id

            checkNotNull(lhsLabel)
            checkNotNull(rhsLabel)
            lhsLabel.compareTo(rhsLabel)
        }
    }
}
