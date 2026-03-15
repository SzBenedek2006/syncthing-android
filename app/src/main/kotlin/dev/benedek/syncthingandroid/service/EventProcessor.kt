package dev.benedek.syncthingandroid.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.util.Consumer
import dev.benedek.syncthingandroid.BuildConfig
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.SyncthingApp
import dev.benedek.syncthingandroid.activities.DeviceActivity
import dev.benedek.syncthingandroid.activities.FolderActivity
import dev.benedek.syncthingandroid.model.CompletionInfo
import dev.benedek.syncthingandroid.model.Event
import dev.benedek.syncthingandroid.service.RestApi.OnReceiveEventListener
import dev.benedek.syncthingandroid.ui.FolderViewModel
import java.io.File
import java.util.Objects
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.concurrent.Volatile
import androidx.core.content.edit



// FIXME: Fix the nullability errors



/**
 * Run by the syncthing service to convert syncthing events into local broadcasts.
 *
 * It uses [RestApi.getEvents] to read the pending events and wait for new events.
 */
class EventProcessor(context: Context, api: RestApi?) : Runnable, OnReceiveEventListener {
    /**
     * Use the MainThread for all callbacks and message handling,
     * or we have to track down nasty threading problems.
     */
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var lastEventId: Long = 0

    @Volatile
    private var shutdown = true

    private val context: Context
    private val api: RestApi?

    @JvmField
    @Inject
    var preferences: SharedPreferences? = null

    @JvmField
    @Inject
    var notificationHandler: NotificationHandler? = null

    init {
        (context.applicationContext as SyncthingApp).component().inject(this)
        this.context = context
        this.api = api
    }

    override fun run() {
        // Restore the last event id if the event processor may have been restarted.
        if (lastEventId == 0L) {
            lastEventId = preferences!!.getLong(PREF_LAST_SYNC_ID, 0)
        }

        // First check if the event number ran backwards.
        // If that's the case we've to start at zero because syncthing was restarted.
        api!!.getEvents(0, 1, object : OnReceiveEventListener {
            override fun onEvent(event: Event?) {
            }

            override fun onDone(lastId: Long) {
                if (lastId < lastEventId) lastEventId = 0

                Log.d(TAG, "Reading events starting with id $lastEventId")

                api.getEvents(lastEventId, 0, this@EventProcessor)
            }
        })
    }

    /**
     * Performs the actual event handling.
     */
    override fun onEvent(event: Event?) {
        var mapData: MutableMap<String?, Any?>? = null
        try {
            mapData = event?.data as MutableMap<String?, Any?>?
        } catch (e: ClassCastException) {
            Log.e(this.toString(), "", e)
        }
        when (event?.type) {
            "ConfigSaved" -> if (api != null) {
                Log.v(TAG, "Forwarding ConfigSaved event to RestApi to get the updated config.")
                api.reloadConfig()
            }

            "PendingDevicesChanged" -> {
                mapNullable<MutableMap<String?, String?>?>(
                    mapData!!["added"] as MutableList<MutableMap<String?, String?>?>? // FIXME
                ) { added: MutableMap<String?, String?>? ->
                    this.onPendingDevicesChanged(added!!)
                }
            }

            "FolderCompletion" -> {
                val completionInfo = CompletionInfo()
                completionInfo.completion = (mapData?.get("completion") as Double?)!!
                api!!.setCompletionInfo(
                    mapData["device"] as String?,  // deviceId
                    mapData["folder"] as String?,  // folderId
                    completionInfo
                )
            }

            "PendingFoldersChanged" -> {
                mapNullable<MutableMap<String?, String?>?>(
                    mapData!!["added"] as MutableList<MutableMap<String?, String?>?>?
                ) { added: MutableMap<String?, String?>? ->
                    this.onPendingFoldersChanged(added!!)
                }
            }

            "ItemFinished" -> {
                val folder = mapData!!["folder"] as String?
                var folderPath: String? = null
                for (f in Objects.requireNonNull(api!!.folders)!!) { // FIXME
                    if (Objects.requireNonNull<String?>(f?.id) == folder) {
                        folderPath = f?.path
                    }
                }
                val updatedFile =
                    File(folderPath, Objects.requireNonNull<Any?>(mapData["item"]) as String)
                if ("delete" != mapData["action"]) {
                    Log.i(TAG, "Rescanned file via MediaScanner: $updatedFile")
                    MediaScannerConnection.scanFile(
                        context, arrayOf<String>(updatedFile.path),
                        null, null
                    )
                } else {
                    // Starting with Android 10/Q and targeting API level 29/removing legacy storage flag,
                    // reports of files being spuriously deleted came up.
                    // Best guess is that Syncthing directly interacted with the filesystem before,
                    // and there's a virtualization layer there now. Also, there's reports this API
                    // changed behaviour with scoped storage. In any case it now does not only
                    // update the media db, but actually delete the file on disk. Which is bad,
                    // as it can race with the creation of the same file and thus delete it. See:
                    // https://github.com/syncthing/syncthing-android/issues/1801
                    // https://github.com/syncthing/syncthing/issues/7974
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        return
                    }
                    // https://stackoverflow.com/a/29881556/1837158
                    Log.i(TAG, "Deleted file from MediaStore: $updatedFile")
                    val contentUri = MediaStore.Files.getContentUri("external")
                    val resolver = context.contentResolver
                    resolver.delete(
                        contentUri, MediaStore.Images.ImageColumns.DATA + " = ?",
                        arrayOf<String>(updatedFile.path)
                    )
                }
            }

            "Ping" -> {}
            "DeviceConnected", "DeviceDisconnected", "DeviceDiscovered", "DownloadProgress", "FolderPaused", "FolderScanProgress", "FolderSummary", "ItemStarted", "LocalIndexUpdated", "LoginAttempt", "RemoteDownloadProgress", "RemoteIndexUpdated", "Starting", "StartupComplete", "StateChanged" -> if (BuildConfig.DEBUG) {
                Log.v(TAG, "Ignored event " + event.type + ", data " + event.data)
            }

            else -> Log.v(TAG, "Unhandled event " + event?.type)
        }
    }

    override fun onDone(lastId: Long) {
        if (lastEventId < lastId) {
            lastEventId = lastId

            // Store the last EventId in case we get killed
            preferences!!.edit { putLong(PREF_LAST_SYNC_ID, lastEventId) }
        }

        synchronized(mainThreadHandler) {
            if (!shutdown) {
                mainThreadHandler.removeCallbacks(this)
                mainThreadHandler.postDelayed(this, EVENT_UPDATE_INTERVAL)
            }
        }
    }

    fun start() {
        Log.d(TAG, "Starting event processor.")

        // Remove all pending callbacks and add a new one. This makes sure that only one
        // event poller is running at any given time.
        synchronized(mainThreadHandler) {
            shutdown = false
            mainThreadHandler.removeCallbacks(this)
            mainThreadHandler.postDelayed(this, EVENT_UPDATE_INTERVAL)
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping event processor.")
        synchronized(mainThreadHandler) {
            shutdown = true
            mainThreadHandler.removeCallbacks(this)
        }
    }

    private fun onPendingDevicesChanged(added: MutableMap<String?, String?>) {
        val deviceId = added["deviceID"]
        val deviceName = added["name"]
        val deviceAddress = added["address"]
        if (deviceId == null) {
            return
        }
        Log.d(TAG, "Unknown device $deviceName($deviceId) wants to connect")

        val title = context.getString(
            R.string.device_rejected,
            if (Objects.requireNonNull<String>(deviceName).isEmpty()) deviceId.substring(
                0,
                7
            ) else deviceName
        )
        val notificationId = notificationHandler!!.getNotificationIdFromText(title)

        // Prepare "accept" action.
        val intentAccept = Intent(context, DeviceActivity::class.java)
            .putExtra(DeviceActivity.EXTRA_NOTIFICATION_ID, notificationId)
            .putExtra(DeviceActivity.EXTRA_IS_CREATE, true)
            .putExtra(DeviceActivity.EXTRA_DEVICE_ID, deviceId)
            .putExtra(DeviceActivity.EXTRA_DEVICE_NAME, deviceName)
        val piAccept = PendingIntent.getActivity(
            context, notificationId,
            intentAccept, Constants.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Prepare "ignore" action.
        val intentIgnore = Intent(context, SyncthingService::class.java)
            .putExtra(SyncthingService.EXTRA_NOTIFICATION_ID, notificationId)
            .putExtra(SyncthingService.EXTRA_DEVICE_ID, deviceId)
            .putExtra(SyncthingService.EXTRA_DEVICE_NAME, deviceName)
            .putExtra(SyncthingService.EXTRA_DEVICE_ADDRESS, deviceAddress)
        intentIgnore.setAction(SyncthingService.ACTION_IGNORE_DEVICE)
        val piIgnore = PendingIntent.getService(
            context, 0,
            intentIgnore, Constants.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Show notification.
        notificationHandler!!.showConsentNotification(notificationId, title, piAccept, piIgnore)
    }

    private fun onPendingFoldersChanged(added: MutableMap<String?, String?>) {
        val deviceId = added["deviceID"]
        val folderId = added["folderID"]
        val folderLabel = added["folderLabel"]
        if (deviceId == null || folderId == null) {
            return
        }
        Log.d(
            TAG, "Device " + deviceId + " wants to share folder " +
                    folderLabel + " (" + folderId + ")"
        )

        // Find the deviceName corresponding to the deviceId
        var deviceName: String? = null
        for (d in Objects.requireNonNull(api!!.getDevices(false))!!) {
            if (d.deviceID == deviceId) {
                deviceName = d.displayName
                break
            }
        }
        val title = context.getString(
            R.string.folder_rejected, deviceName,
            if (Objects.requireNonNull<String>(folderLabel)
                    .isEmpty()
            ) folderId else "$folderLabel ($folderId)"
        )
        val notificationId = notificationHandler!!.getNotificationIdFromText(title)

        // Prepare "accept" action.
        val isNewFolder = api.folders?.none { it?.id == folderId } ?: true
        val intentAccept = Intent(context, FolderActivity::class.java)
            .putExtra(FolderViewModel.EXTRA_NOTIFICATION_ID, notificationId)
            .putExtra(FolderViewModel.EXTRA_IS_CREATE, isNewFolder)
            .putExtra(FolderViewModel.EXTRA_DEVICE_ID, deviceId)
            .putExtra(FolderViewModel.EXTRA_FOLDER_ID, folderId)
            .putExtra(FolderViewModel.EXTRA_FOLDER_LABEL, folderLabel)
        val piAccept = PendingIntent.getActivity(
            context, notificationId,
            intentAccept, Constants.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Prepare "ignore" action.
        val intentIgnore = Intent(context, SyncthingService::class.java)
            .putExtra(SyncthingService.EXTRA_NOTIFICATION_ID, notificationId)
            .putExtra(SyncthingService.EXTRA_DEVICE_ID, deviceId)
            .putExtra(SyncthingService.EXTRA_FOLDER_ID, folderId)
            .putExtra(SyncthingService.EXTRA_FOLDER_LABEL, folderLabel)
        intentIgnore.setAction(SyncthingService.ACTION_IGNORE_FOLDER)
        val piIgnore = PendingIntent.getService(
            context, 0,
            intentIgnore, Constants.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Show notification.
        notificationHandler!!.showConsentNotification(notificationId, title, piAccept, piIgnore)
    }

    private fun <T> mapNullable(l: MutableList<T?>?, c: Consumer<T?>) {
        if (l != null) {
            for (m in l) {
                c.accept(m)
            }
        }
    }

    companion object {
        private const val TAG = "EventProcessor"
        private const val PREF_LAST_SYNC_ID = "last_sync_id"

        /**
         * Minimum interval in seconds at which the events are polled from syncthing and processed.
         * This intervall will not wake up the device to save battery power.
         */
        private val EVENT_UPDATE_INTERVAL = TimeUnit.SECONDS.toMillis(15)
    }
}
