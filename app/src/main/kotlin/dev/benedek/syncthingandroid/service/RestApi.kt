package dev.benedek.syncthingandroid.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.common.collect.ImmutableMap
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import dev.benedek.syncthingandroid.BuildConfig
import dev.benedek.syncthingandroid.SyncthingApp
import dev.benedek.syncthingandroid.activities.ShareActivity
import dev.benedek.syncthingandroid.http.ApiRequest.OnSuccessListener
import dev.benedek.syncthingandroid.http.GetRequest
import dev.benedek.syncthingandroid.http.PostConfigRequest
import dev.benedek.syncthingandroid.http.PostRequest
import dev.benedek.syncthingandroid.model.Completion
import dev.benedek.syncthingandroid.model.CompletionInfo
import dev.benedek.syncthingandroid.model.Config
import dev.benedek.syncthingandroid.model.Config.Gui
import dev.benedek.syncthingandroid.model.Connections
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
import java.util.Collections
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import androidx.core.content.edit

/**
 * Provides functions to interact with the syncthing REST API.
 */
class RestApi(
    context: Context, url: URL, apiKey: String, apiListener: OnApiAvailableListener,
    configListener: OnConfigChangedListener
) {
    fun interface OnConfigChangedListener {
        fun onConfigChanged()
    }

    fun interface OnResultListener1<T> {
        fun onResult(t: T?)
    }

    fun interface OnResultListener2<T, R> {
        fun onResult(t: T?, r: R?)
    }

    private val mContext: Context
    val url: URL
    private val mApiKey: String

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
    private var previousConnections: Connections? = null

    /**
     * Stores the timestamp of the last successful request to [GetRequest.URI_CONNECTIONS].
     */
    private var previousConnectionTime: Long = 0

    /**
     * In the last-finishing [.readConfigFromRestApi] callback, we have to call
     * [SyncthingService.onApiAvailable] to indicate that the RestApi class is fully initialized.
     * We do this to avoid getting stuck with our main thread due to synchronous REST queries.
     * The correct indication of full initialisation is crucial to stability as other listeners of
     * [SettingsActivity.onServiceStateChange] needs cached config and system information available.
     * e.g. SettingsFragment need "mLocalDeviceId"
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
     * Object that must be locked upon accessing mConfig
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

    @JvmField
    @Inject
    var notificationHandler: NotificationHandler? = null

    fun interface OnApiAvailableListener {
        fun onApiAvailable()
    }

    private val onApiAvailableListener: OnApiAvailableListener

    private val onConfigChangedListener: OnConfigChangedListener

    init {
        (context.applicationContext as SyncthingApp).component().inject(this)
        mContext = context
        this.url = url
        mApiKey = apiKey
        onApiAvailableListener = apiListener
        onConfigChangedListener = configListener
    }

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
        GetRequest(
            mContext,
            this.url,
            GetRequest.URI_VERSION,
            mApiKey,
            null
        ) { result: String? ->
            val json = JsonParser.parseString(result).getAsJsonObject()
            this.version = json.get("version").asString
            Log.i(TAG, "Syncthing version is " + this.version)
            //updateDebugFacilitiesCache();
            synchronized(asyncQueryCompleteLock) {
                asyncQueryVersionComplete = true
                checkReadConfigFromRestApiCompleted()
            }
        }
        GetRequest(
            mContext,
            this.url,
            GetRequest.URI_CONFIG,
            mApiKey,
            null
        ) { result: String? ->
            onReloadConfigComplete(result)
            synchronized(asyncQueryCompleteLock) {
                asyncQueryConfigComplete = true
                checkReadConfigFromRestApiCompleted()
            }
        }
        getSystemInfo { info: SystemInfo? ->
            localDeviceId = info!!.myID
            urVersionMax = info.urVersionMax
            synchronized(asyncQueryCompleteLock) {
                asyncQuerySystemInfoComplete = true
                checkReadConfigFromRestApiCompleted()
            }
        }
    }

    fun checkReadConfigFromRestApiCompleted() {
        if (asyncQueryVersionComplete && asyncQueryConfigComplete && asyncQuerySystemInfoComplete) {
            Log.v(TAG, "Reading config from REST completed.")
            onApiAvailableListener.onApiAvailable()
        }
    }

    fun reloadConfig() {
        GetRequest(
            mContext,
            this.url,
            GetRequest.URI_CONFIG,
            mApiKey,
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
                "mConfig.remoteIgnoredDevices = " + Gson().toJson(config!!.remoteIgnoredDevices)
            )
        }

        // Update cached device and folder information stored in the mCompletion model.
        completion.updateFromConfig(getDevices(true), this.folders)
    }

    /**
     * Queries debug facilities available from the currently running syncthing binary
     * if the syncthing binary version changed. First launch of the binary is also
     * considered as a version change.
     * Precondition: [.mVersion] read from REST
     * 
     * 
     * It's not possible as of 2.0, so always falling back to the hardcoded list.
     */
    //    private void updateDebugFacilitiesCache() {
    //        final String PREF_LAST_BINARY_VERSION = "lastBinaryVersion";
    //        if (!mVersion.equals(PreferenceManager.getDefaultSharedPreferences(mContext).getString(PREF_LAST_BINARY_VERSION, ""))) {
    //            // First binary launch or binary upgraded case.
    //            new GetRequest(mContext, mUrl, GetRequest.URI_DEBUG, mApiKey, null, result -> {
    //                try {
    //                    JsonObject json = JsonParser.parseString(result).getAsJsonObject();
    //                    JsonObject jsonFacilities = json.getAsJsonObject("facilities");
    //                    Set<String> facilitiesToStore = new HashSet<>(jsonFacilities.keySet());
    //
    //                    PreferenceManager.getDefaultSharedPreferences(mContext).edit()
    //                        .putStringSet(Constants.PREF_DEBUG_FACILITIES_AVAILABLE, facilitiesToStore)
    //                        .apply();
    //
    //                    // Store current binary version so we will only store this information again
    //                    // after a binary update.
    //                    PreferenceManager.getDefaultSharedPreferences(mContext).edit()
    //                        .putString(PREF_LAST_BINARY_VERSION, mVersion)
    //                        .apply();
    //                } catch (Exception e) {
    //                    Log.w(TAG, "updateDebugFacilitiesCache: Failed to get debug facilities. result=" + result);
    //                }
    //            });
    //        }
    //    }
    /**
     * Permanently ignore a device when it tries to connect.
     * Ignored devices will not trigger the "DeviceRejected" event
     * in [EventProcessor.onEvent].
     */
    fun ignoreDevice(deviceId: String, deviceName: String?, deviceAddress: String?) {
        synchronized(configLock) {
            // Check if the device has already been ignored.
            if (config?.remoteIgnoredDevices != null) {
                for (remoteIgnoredDevice in config!!.remoteIgnoredDevices) {
                    if (deviceId == remoteIgnoredDevice?.deviceID) {
                        // Device already ignored.
                        Log.d(TAG, "Device already ignored [$deviceId]")
                        return
                    }
                }
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
            if (config?.devices != null) {
                for (device in config!!.devices) {
                    if (deviceId == device?.deviceID) {
                        /*
                          Check if the folder has already been ignored.
                         */
                        if (device.ignoredFolders != null) {
                            for (ignoredFolder in device.ignoredFolders) {
                                if (folderId == ignoredFolder?.id) {
                                    // Folder already ignored.
                                    Log.d(
                                        TAG,
                                        "Folder [$folderId] already ignored on device [$deviceId]"
                                    )
                                    return
                                }
                            }
                        }


                        /*
                          Ignore folder by moving its corresponding "pendingFolder" entry to
                          a newly created "ignoredFolder" entry.
                         */
                        val ignoredFolder = IgnoredFolder()
                        ignoredFolder.id = folderId
                        ignoredFolder.label = folderLabel.toString()
                        ignoredFolder.time = dateFormat.format(Date())
                        device.ignoredFolders?.add(ignoredFolder)
                        if (BuildConfig.DEBUG) {
                            Log.v(
                                TAG,
                                "device.ignoredFolders = " + Gson().toJson(device.ignoredFolders)
                            )
                        }
                        sendConfig()
                        Log.d(
                            TAG,
                            "Ignored folder [$folderId] announced by device [$deviceId]"
                        )

                        // Given deviceId handled.
                        break
                    }
                }
            }

        }
    }

    /**
     * Undo ignoring devices and folders.
     */
    fun undoIgnoredDevicesAndFolders() {
        Log.d(TAG, "Undo ignoring devices and folders ...")
        synchronized(configLock) {
            if (config?.devices != null) {
                config!!.remoteIgnoredDevices?.clear()
                for (device in config!!.devices) {
                    device?.ignoredFolders?.clear()
                }
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
            mContext,
            this.url, PostRequest.URI_DB_OVERRIDE, mApiKey,
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
        PostConfigRequest(mContext, this.url, mApiKey, jsonConfig, null)
        onConfigChangedListener.onConfigChanged()
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
            mContext,
            this.url,
            mApiKey,
            jsonConfig
        ) { _: String? ->
            val intent = Intent(mContext, SyncthingService::class.java)
                .setAction(SyncthingService.ACTION_RESTART)
            mContext.startService(intent)
        }
        onConfigChangedListener.onConfigChanged()
    }

    fun shutdown() {
        notificationHandler!!.cancelRestartNotification()
    }

    val folders: MutableList<Folder?>?
        get() {
            synchronized(configLock) {
                if (config == null || config!!.folders == null) {
                    return null
                }
                val folders =
                    deepCopy<MutableList<Folder?>?>(
                        config!!.folders,
                        object :
                            TypeToken<MutableList<Folder?>>() {}.type
                    )
                if (folders != null) {
                    Collections.sort<Folder?>(
                        folders,
                        FOLDERS_COMPARATOR
                    )
                }
                return folders
            }
        }

    /**
     * This is only used for new folder creation, see [FolderActivity].
     */
    fun createFolder(folder: Folder?) {
        synchronized(configLock) {
            // Add the new folder to the model.
            config!!.folders?.add(folder)
            // Send model changes to syncthing, does not require a restart.
            sendConfig()
        }
    }

    fun updateFolder(newFolder: Folder) {
        synchronized(configLock) {
            removeFolderInternal(newFolder.id)
            config!!.folders?.add(newFolder)
            sendConfig()
        }
    }

    fun removeFolder(id: String?) {
        synchronized(configLock) {
            removeFolderInternal(id)
            // mCompletion will be updated after the ConfigSaved event.
            sendConfig()
        }
        PreferenceManager.getDefaultSharedPreferences(mContext).edit {
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
            val devices =
                getDevices(true)
            if (devices!!.isEmpty()) {
                throw RuntimeException("RestApi.getLocalDevice: devices is empty.")
            }
            Log.v(
                TAG,
                "getLocalDevice: Looking for local device ID $localDeviceId"
            )
            for (d in devices) {
                if (d.deviceID == localDeviceId) {
                    return deepCopy<Device?>(
                        d,
                        Device::class.java
                    )
                }
            }
            throw RuntimeException("RestApi.getLocalDevice: Failed to get the local device crucial to continuing execution.")
        }

    fun addDevice(device: Device, errorListener: OnResultListener1<String?>) {
        if (device.deviceID != null) {
            normalizeDeviceId(device.deviceID!!, { _: String? ->
                synchronized(configLock) {
                    config!!.devices?.add(device)
                    sendConfig()
                }
            }, errorListener)
        }
    }

    fun editDevice(newDevice: Device) {
        synchronized(configLock) {
            removeDeviceInternal(newDevice.deviceID)
            config!!.devices?.add(newDevice)
            sendConfig()
        }
    }

    fun removeDevice(deviceId: String?) {
        synchronized(configLock) {
            removeDeviceInternal(deviceId)
            // mCompletion will be updated after the ConfigSaved event.
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
                    config!!.options,
                    Options::class.java
                )
            }
        }

    val gui: Gui?
        get() {
            synchronized(configLock) {
                return deepCopy<Gui?>(config!!.gui, Gui::class.java)
            }
        }

    fun editSettings(newGui: Gui?, newOptions: Options?) {
        synchronized(configLock) {
            config!!.gui = newGui
            config!!.options = newOptions
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
    fun getSystemInfo(listener: OnResultListener1<SystemInfo?>) {
        GetRequest(
            mContext,
            this.url, GetRequest.URI_SYSTEM, mApiKey, null
        ) { result: String? ->
            listener.onResult(
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
    fun getSystemVersion(listener: OnResultListener1<SystemVersion?>) {
        GetRequest(
            mContext,
            this.url,
            GetRequest.URI_VERSION,
            mApiKey,
            null
        ) { result: String? ->
            val systemVersion =
                Gson().fromJson(result, SystemVersion::class.java)
            listener.onResult(systemVersion)
        }
    }

    /**
     * Returns connection info for the local device and all connected devices.
     */
    fun getConnections(listener: OnResultListener1<Connections?>) {
        GetRequest(
            mContext,
            this.url,
            GetRequest.URI_CONNECTIONS,
            mApiKey,
            null,
            OnSuccessListener { result: String? ->
                val now = System.currentTimeMillis()
                val msElapsed = now - previousConnectionTime
                if (msElapsed < Constants.GUI_UPDATE_INTERVAL && previousConnections != null) {
                    listener.onResult(
                        deepCopy<Connections?>(
                            previousConnections,
                            Connections::class.java
                        )
                    )
                    return@OnSuccessListener
                }

                previousConnectionTime = now
                val connections = Gson().fromJson(result, Connections::class.java)

                if (connections.connectionsMap != null) {
                    for (entry in connections.connectionsMap!!.entries) {
                        entry.value?.completion = completion.getDeviceCompletion(entry.key)

                        val prev = if (previousConnections?.connectionsMap?.containsKey(entry.key) != null) {
                            previousConnections!!.connectionsMap!![entry.key]
                        } else Connections.Connection()

                        if (prev != null) {
                            entry.value?.setTransferRate(prev, msElapsed)
                        }
                    }
                }

                val prevTotal = if (previousConnections != null)
                    previousConnections!!.total
                else
                    Connections.Connection()

                connections.total?.setTransferRate(prevTotal!!, msElapsed)

                previousConnections = connections
                listener.onResult(deepCopy<Connections?>(connections, Connections::class.java))
            })
    }

    /**
     * Returns status information about the folder with the given id.
     */
    fun getFolderStatus(folderId: String, listener: OnResultListener2<String?, FolderStatus?>) {
        GetRequest(
            mContext,
            this.url,
            GetRequest.URI_STATUS,
            mApiKey,
            mutableMapOf("folder" to folderId)
        ) { result: String? ->
            val m = Gson().fromJson(result, FolderStatus::class.java)
            cachedFolderStatuses[folderId] = m
            listener.onResult(folderId, m)
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
            mContext,
            this.url,
            GetRequest.URI_EVENTS,
            mApiKey,
            params
        ) { result: String? ->
            val jsonEvents = JsonParser.parseString(result).getAsJsonArray()
            var lastId: Long = 0

            for (i in 0..<jsonEvents.size()) {
                val json = jsonEvents.get(i)
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
        id: String, listener: OnResultListener1<String?>,
        errorListener: OnResultListener1<String?>
    ) {
        GetRequest(
            mContext,
            this.url, GetRequest.URI_DEVICEID, mApiKey,
            mutableMapOf("id" to id)
        ) { result: String? ->
            val json = JsonParser.parseString(result).getAsJsonObject()
            val normalizedId = json.get("id")
            val error = json.get("error")
            if (normalizedId != null) listener.onResult(normalizedId.asString)
            if (error != null) errorListener.onResult(error.asString)
        }
    }


    /**
     * Updates cached folder and device completion info according to event data.
     */
    fun setCompletionInfo(deviceId: String?, folderId: String?, completionInfo: CompletionInfo?) {
        completion.setCompletionInfo(deviceId, folderId, completionInfo)
    }

    /**
     * Returns prettyfied usage report.
     */
    fun getUsageReport(listener: OnResultListener1<String?>) {
        GetRequest(
            mContext,
            this.url,
            GetRequest.URI_REPORT,
            mApiKey,
            null
        ) { result: String? ->
            val json = JsonParser.parseString(result)
            val gson = GsonBuilder().setPrettyPrinting().create()
            listener.onResult(gson.toJson(json))
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
            return options.isUsageReportingDecided(urVersionMax!!)
        }

    fun setUsageReporting(acceptUsageReporting: Boolean) {
        val options = this.options
        if (options == null) {
            Log.e(TAG, "setUsageReporting called while options == null")
            return
        }
        options.urAccepted =
            (if (acceptUsageReporting) urVersionMax else Options.USAGE_REPORTING_DENIED)!!
        synchronized(configLock) {
            config!!.options = options
        }
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
        private val FOLDERS_COMPARATOR = Comparator { lhs: Folder?, rhs: Folder? ->
            val lhsLabel =
                if (lhs!!.label != null && !lhs.label!!.isEmpty()) lhs.label else lhs.id
            val rhsLabel =
                if (rhs!!.label != null && !rhs.label!!.isEmpty()) rhs.label else rhs.id

            checkNotNull(lhsLabel)
            checkNotNull(rhsLabel)
            lhsLabel.compareTo(rhsLabel)
        }
    }
}
