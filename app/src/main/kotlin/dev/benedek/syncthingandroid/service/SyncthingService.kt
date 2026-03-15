package dev.benedek.syncthingandroid.service

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.common.io.Files
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.SyncthingApp
import dev.benedek.syncthingandroid.http.PollWebGuiAvailableTask
import dev.benedek.syncthingandroid.model.RunConditionCheckResult
import dev.benedek.syncthingandroid.service.Constants.getConfigFile
import dev.benedek.syncthingandroid.service.Constants.getHttpsCertFile
import dev.benedek.syncthingandroid.service.Constants.getHttpsKeyFile
import dev.benedek.syncthingandroid.service.Constants.getPrivateKeyFile
import dev.benedek.syncthingandroid.service.Constants.getPublicKeyFile
import dev.benedek.syncthingandroid.service.SyncthingRunnable.OnSyncthingKilled
import dev.benedek.syncthingandroid.util.ConfigXml
import dev.benedek.syncthingandroid.util.ConfigXml.OpenConfigException
import dev.benedek.syncthingandroid.util.PermissionUtil.haveStoragePermission
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference

/**
 * Holds the native syncthing instance and provides an API to access it.
 */
class SyncthingService : Service() {
    fun interface OnServiceStateChangeListener {
        fun onServiceStateChange(currentState: State?)
    }

    fun interface OnRunConditionCheckResultListener {
        fun onRunConditionCheckResultChanged(result: RunConditionCheckResult?)
    }

    /**
     * Indicates the current state of SyncthingService and of Syncthing itself.
     */
    enum class State {
        /** Service is initializing, Syncthing was not started yet.  */
        INIT,

        /** Syncthing binary is starting.  */
        STARTING,

        /** Syncthing binary is running,
         * Rest API is available,
         * RestApi class read the config and is fully initialized.
         */
        ACTIVE,

        /** Syncthing binary is shutting down.  */
        DISABLED,

        /** There is some problem that prevents Syncthing from running.  */
        ERROR,
    }

    /**
     * Initialize the service with State.DISABLED as [RunConditionMonitor] will
     * send an update if we should run the binary after it got instantiated in
     * [.onStartCommand].
     */
    var currentState: State = State.DISABLED
        private set
    private val currentCheckResult =
        AtomicReference(RunConditionCheckResult.SHOULD_RUN)

    private var config: ConfigXml? = null
    private var pollWebGuiAvailableTask: PollWebGuiAvailableTask? = null
    var api: RestApi? = null
        private set
    private var eventProcessor: EventProcessor? = null
    private var runConditionMonitor: RunConditionMonitor? = null
    private var syncthingRunnable: SyncthingRunnable? = null
    private val startupExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var startupFuture: Future<*>? = null
    private var syncthingRunnableThread: Thread? = null
    private var handler: Handler? = null

    private val onServiceStateChangeListeners = HashSet<OnServiceStateChangeListener?>()
    private val onRunConditionCheckResultListeners = HashSet<OnRunConditionCheckResultListener?>()
    private val binder = SyncthingServiceBinder(this)


    val notificationHandler: NotificationHandler by lazy { NotificationHandler(this) }

    // FIXME
    private val mPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    /**
     * Object that must be locked upon accessing mCurrentState
     */
    private val stateLock = Any()

    /**
     * Stores the result of the last should run decision received by OnDeviceStateChangedListener.
     */
    private var lastDeterminedShouldRun = false

    /**
     * True if a service [.onDestroy] was requested while syncthing is starting,
     * in that case, perform stop in [.onApiAvailable].
     */
    private var destroyScheduled = false

    /**
     * True if the user granted the storage permission.
     */
    private var storagePermissionGranted = false

    /**
     * Starts the native binary.
     */
    override fun onCreate() {
        Log.v(TAG, "onCreate")
        super.onCreate()
        (application as SyncthingApp).component().inject(this)
        handler = Handler(Looper.getMainLooper())

        /*
          If runtime permissions are revoked, android kills and restarts the service.
          see issue: https://github.com/syncthing/syncthing-android/issues/871
          We need to recheck if we still have the storage permission.
         */
        storagePermissionGranted = haveStoragePermission(this)

        notificationHandler.setAppShutdownInProgress(false)
    }

    /**
     * Handles intent actions, e.g. [.ACTION_RESTART]
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.v(TAG, "onStartCommand; Started by intent: $action")

        if (!storagePermissionGranted) {
            Log.e(TAG, "User revoked storage permission. Stopping service.")
            notificationHandler.showStoragePermissionRevokedNotification()
            stopSelf()
            return START_NOT_STICKY
        }

        /*
          Send current service state to listening endpoints.
          This is required that components know about the service State.DISABLED
          if RunConditionMonitor does not send a "shouldRun = true" callback
          to start the binary according to preferences shortly after its creation.
          See {@link mLastDeterminedShouldRun} defaulting to "false".
         */
        if (this.currentState == State.DISABLED) {
            synchronized(stateLock) {
                onServiceStateChange(this.currentState)
            }
        }
        if (runConditionMonitor == null) {
            /*
              Instantiate the run condition monitor on first onStartCommand and
              enable callback on run condition change affecting the final decision to
              run/terminate syncthing. After initial run conditions are collected
              the first decision is sent to {@link onUpdatedShouldRunDecision}.
             */
            runConditionMonitor = RunConditionMonitor(
                this@SyncthingService
            ) { result: RunConditionCheckResult? ->
                this.onUpdatedShouldRunDecision(
                    result!!
                )
            }
        }
        notificationHandler.updatePersistentNotification(this)

        if (ACTION_RESTART == action && this.currentState == State.ACTIVE) {
            shutdown(State.INIT) { this.launchStartupTask() }
        } else if (ACTION_RESET_DATABASE == action) {
            shutdown(State.INIT) {
                SyncthingRunnable(this, SyncthingRunnable.Command.resetdatabase).run()
                launchStartupTask()
            }
        } else if (ACTION_RESET_DELTAS == action) {
            shutdown(State.INIT) {
                SyncthingRunnable(this, SyncthingRunnable.Command.resetdeltas).run()
                launchStartupTask()
            }
        } else if (ACTION_REFRESH_NETWORK_INFO == action) {
            runConditionMonitor!!.updateShouldRunDecision()
        } else if (ACTION_IGNORE_DEVICE == action && this.currentState == State.ACTIVE) {
            // mApi is not null due to State.ACTIVE
            checkNotNull(this.api)
            api!!.ignoreDevice(
                intent.getStringExtra(EXTRA_DEVICE_ID)!!, intent.getStringExtra(
                    EXTRA_DEVICE_NAME
                ), intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
            )
            notificationHandler.cancelConsentNotification(
                intent.getIntExtra(
                    EXTRA_NOTIFICATION_ID, 0
                )
            )
        } else if (ACTION_IGNORE_FOLDER == action && this.currentState == State.ACTIVE) {
            // mApi is not null due to State.ACTIVE
            checkNotNull(this.api)
            api!!.ignoreFolder(
                intent.getStringExtra(EXTRA_DEVICE_ID)!!, intent.getStringExtra(
                    EXTRA_FOLDER_ID
                )!!, intent.getStringExtra(EXTRA_FOLDER_LABEL)
            )
            notificationHandler.cancelConsentNotification(
                intent.getIntExtra(
                    EXTRA_NOTIFICATION_ID, 0
                )
            )
        } else if (ACTION_OVERRIDE_CHANGES == action && this.currentState == State.ACTIVE) {
            checkNotNull(this.api)
            api!!.overrideChanges(intent.getStringExtra(EXTRA_FOLDER_ID)!!)
        }
        return START_STICKY
    }

    /**
     * After run conditions monitored by [RunConditionMonitor] changed, and
     * it had an influence on the decision to run/terminate syncthing, this
     * function is called to notify this class to run/terminate the syncthing binary.
     * [.onServiceStateChange] is called while applying the decision change.
     */
    private fun onUpdatedShouldRunDecision(result: RunConditionCheckResult) {
        val newShouldRunDecision = result.isShouldRun
        val reasonsChanged = currentCheckResult.getAndSet(result) != result
        if (reasonsChanged) {
            onRunConditionCheckResultChange(result)
        }

        if (newShouldRunDecision != lastDeterminedShouldRun) {
            Log.i(
                TAG,
                "shouldRun decision changed to $newShouldRunDecision according to configured run conditions."
            )
            lastDeterminedShouldRun = newShouldRunDecision

            // React to the shouldRun condition change.
            if (newShouldRunDecision) {
                // Start syncthing.
                when (this.currentState) {
                    State.DISABLED, State.INIT ->                         // HACK: Make sure there is no syncthing binary left running from an improper
                        // shutdown (e.g. Play Store update).
                        shutdown(State.INIT) { this.launchStartupTask() }

                    else -> {}
                }
            } else {
                // Stop syncthing.
                if (this.currentState == State.DISABLED) {
                    return
                }
                Log.v(TAG, "Stopping syncthing")
                shutdown(State.DISABLED) {}
            }
        }
    }

    /**
     * Prepares to launch the syncthing binary.
     */
    private fun launchStartupTask() {
        Log.v(TAG, "Starting syncthing")
        synchronized(stateLock) {
            if (this.currentState != State.INIT) {
                Log.e(
                    TAG,
                    "launchStartupTask: Wrong state " + this.currentState + " detected. Cancelling."
                )
                return
            }
        }

        onServiceStateChange(State.STARTING)

        startupFuture = startupExecutor.submit {
            try {
                config = ConfigXml(this@SyncthingService)
                config!!.updateIfNeeded()

                // Success: Move to main thread
                handler!!.post {
                    startupFuture = null // Mark as done
                    onStartupTaskCompleteListener()
                }
            } catch (_: OpenConfigException) {
                handler!!.post {
                    notificationHandler.showCrashedNotification(
                        R.string.config_create_failed,
                        true
                    )
                    synchronized(stateLock) {
                        onServiceStateChange(State.ERROR)
                    }
                }
            }
        }
    }


    private fun onStartupTaskCompleteListener() {
        if (this.api == null) {
            this.api = RestApi(
                this, config!!.webGuiUrl, config!!.apiKey!!,
                { this.onApiAvailable() }, {
                    onServiceStateChange(
                        this.currentState
                    )
                })
            Log.i(TAG, "Web GUI will be available at " + config!!.webGuiUrl)
        }

        // Start the syncthing binary.
        if (syncthingRunnable != null || syncthingRunnableThread != null) {
            Log.e(TAG, "onStartupTaskCompleteListener: Syncthing binary lifecycle violated")
            return
        }
        syncthingRunnable = SyncthingRunnable(this, SyncthingRunnable.Command.main)
        syncthingRunnableThread = Thread(syncthingRunnable)
        syncthingRunnableThread!!.start()

        /*
           Wait for the web-gui of the native syncthing binary to come online.

           In case the binary is to be stopped, also be aware that another thread could request
           to stop the binary in the time while waiting for the GUI to become active. See the comment
           for {@link SyncthingService#onDestroy} for details.
          */
        if (pollWebGuiAvailableTask == null) {
            pollWebGuiAvailableTask = PollWebGuiAvailableTask(
                this, this.webGuiUrl, config!!.apiKey!!
            ) { _: String? ->
                Log.i(TAG, "Web GUI has come online at " + config!!.webGuiUrl)
                if (this.api != null) {
                    api!!.readConfigFromRestApi()
                }
            }
        }
    }

    /**
     * Called when [RestApi.checkReadConfigFromRestApiCompleted] detects
     * the RestApi class has been fully initialized.
     * UI stressing results in mApi getting null on simultaneous shutdown, so
     * we check it for safety.
     */
    fun onApiAvailable() {
        if (this.api == null) {
            Log.e(TAG, "onApiAvailable: Did we stop the binary during startup? mApi == null")
            return
        }
        synchronized(stateLock) {
            if (this.currentState != State.STARTING) {
                Log.e(
                    TAG,
                    "onApiAvailable: Wrong state " + this.currentState + " detected. Cancelling callback."
                )
                return
            }
            onServiceStateChange(State.ACTIVE)
        }

        /*
          If the service instance got an onDestroy() event while being in
          State.STARTING we'll trigger the service onDestroy() now. this
          allows the syncthing binary to get gracefully stopped.
         */
        if (destroyScheduled) {
            destroyScheduled = false
            stopSelf()
            return
        }

        if (eventProcessor == null) {
            eventProcessor = EventProcessor(this@SyncthingService, this.api)
            eventProcessor!!.start()
        }
    }

    override fun onBind(intent: Intent?): SyncthingServiceBinder {
        return binder
    }

    /**
     * Stops the native binary.
     * Shuts down RunConditionMonitor instance.
     */
    override fun onDestroy() {
        Log.v(TAG, "onDestroy")
        if (runConditionMonitor != null) {
            /*
              Shut down the OnDeviceStateChangedListener so we won't get interrupted by run
              condition events that occur during shutdown.
             */
            runConditionMonitor!!.shutdown()
        }
        notificationHandler.setAppShutdownInProgress(true)

        startupExecutor.shutdownNow()
        if (storagePermissionGranted) {
            synchronized(stateLock) {
                if (this.currentState == State.STARTING) {
                    Log.i(TAG, "Delay shutting down synchting binary until initialisation finished")
                    destroyScheduled = true
                } else {
                    Log.i(TAG, "Shutting down syncthing binary immediately")
                    shutdown(State.DISABLED) {}
                }
            }
        } else {
            // If the storage permission got revoked, we did not start the binary and
            // are in State.INIT requiring an immediate shutdown of this service class.
            Log.i(TAG, "Shutting down syncthing binary due to missing storage permission.")
            shutdown(State.DISABLED) {}
        }
        super.onDestroy()
    }

    /**
     * Stop Syncthing and all helpers like event processor and api handler.
     *
     *
     * Sets [.mCurrentState] to newState, and calls onKilledListener once Syncthing is killed.
     */
    private fun shutdown(newState: State, onKilledListener: OnSyncthingKilled) {
        Log.i(TAG, "Shutting down background service")
        synchronized(stateLock) {
            onServiceStateChange(newState)
        }

        if (pollWebGuiAvailableTask != null) {
            pollWebGuiAvailableTask!!.cancelRequestsAndCallback()
            pollWebGuiAvailableTask = null
        }

        if (eventProcessor != null) {
            eventProcessor!!.stop()
            eventProcessor = null
        }

        if (this.api != null) {
            api!!.shutdown()
            this.api = null
        }

        if (syncthingRunnable != null) {
            syncthingRunnable!!.killSyncthing()
            if (syncthingRunnableThread != null) {
                Log.v(TAG, "Waiting for mSyncthingRunnableThread to finish after killSyncthing ...")
                try {
                    syncthingRunnableThread!!.join()
                } catch (_: InterruptedException) {
                    Log.w(TAG, "mSyncthingRunnableThread InterruptedException")
                }
                Log.v(TAG, "Finished mSyncthingRunnableThread.")
                syncthingRunnableThread = null
            }
            syncthingRunnable = null
        }
        if (startupFuture != null && !startupFuture!!.isDone) {
            startupFuture!!.cancel(true)
            startupFuture = null
        }
        onKilledListener.onKilled()
    }

    /**
     * Force re-evaluating run conditions immediately e.g. after
     * preferences were modified by [SettingsActivity].
     */
    fun evaluateRunConditions() {
        if (runConditionMonitor == null) {
            return
        }
        Log.v(TAG, "Forced re-evaluating run conditions ...")
        runConditionMonitor!!.updateShouldRunDecision()
    }

    /**
     * Register a listener for the syncthing API state changing.
     *
     *
     * The listener is called immediately with the current state, and again whenever the state
     * changes. The call is always from the GUI thread.
     *
     * @see .unregisterOnServiceStateChangeListener
     */
    fun registerOnServiceStateChangeListener(listener: OnServiceStateChangeListener) {
        // Make sure we don't send an invalid state or syncthing might show a "disabled" message
        // when it's just starting up.
        listener.onServiceStateChange(this.currentState)
        onServiceStateChangeListeners.add(listener)
    }

    /**
     * Unregisters a previously registered listener.
     *
     * @see .registerOnServiceStateChangeListener
     */
    fun unregisterOnServiceStateChangeListener(listener: OnServiceStateChangeListener?) {
        onServiceStateChangeListeners.remove(listener)
    }

    /**
     * Called to notify listeners of an API change.
     */
    private fun onServiceStateChange(newState: State) {
        Log.v(TAG, "onServiceStateChange: from " + this.currentState + " to " + newState)
        this.currentState = newState
        handler!!.post {
            notificationHandler.updatePersistentNotification(this)
            val i = onServiceStateChangeListeners.iterator()
            while (i.hasNext()) {
                val listener = i.next()
                if (listener != null) {
                    listener.onServiceStateChange(this.currentState)
                } else {
                    i.remove()
                }
            }
        }
    }

    fun registerOnRunConditionCheckResultChange(listener: OnRunConditionCheckResultListener) {
        listener.onRunConditionCheckResultChanged(currentCheckResult.get())
        onRunConditionCheckResultListeners.add(listener)
    }

    fun unregisterOnRunConditionCheckResultChange(listener: OnRunConditionCheckResultListener?) {
        onRunConditionCheckResultListeners.remove(listener)
    }

    private fun onRunConditionCheckResultChange(result: RunConditionCheckResult?) {
        handler!!.post {
            val i = onRunConditionCheckResultListeners.iterator()
            while (i.hasNext()) {
                val listener = i.next()
                if (listener != null) {
                    listener.onRunConditionCheckResultChanged(result)
                } else {
                    i.remove()
                }
            }
        }
    }


    val webGuiUrl: URL
        get() = config!!.webGuiUrl

    val currentRunConditionCheckResult: RunConditionCheckResult?
        get() = currentCheckResult.get()

    /**
     * Exports the local config and keys to [Constants.EXPORT_PATH].
     */
    fun exportConfig() {
        if (!Constants.EXPORT_PATH.mkdirs()) {
            Log.w(this.toString(), "Couldn't create export directory!")
        }
        try {
            Files.copy(
                getConfigFile(this),
                File(Constants.EXPORT_PATH, Constants.CONFIG_FILE)
            )
            Files.copy(
                getPrivateKeyFile(this),
                File(Constants.EXPORT_PATH, Constants.PRIVATE_KEY_FILE)
            )
            Files.copy(
                getPublicKeyFile(this),
                File(Constants.EXPORT_PATH, Constants.PUBLIC_KEY_FILE)
            )
            Files.copy(
                getHttpsCertFile(this),
                File(Constants.EXPORT_PATH, Constants.HTTPS_CERT_FILE)
            )
            Files.copy(
                getHttpsKeyFile(this),
                File(Constants.EXPORT_PATH, Constants.HTTPS_KEY_FILE)
            )
        } catch (e: IOException) {
            Log.w(TAG, "Failed to export config", e)
        }
    }

    /**
     * Imports config and keys from [Constants.EXPORT_PATH].
     *
     * @return True if the import was successful, false otherwise (e.g. if files aren't found).
     */
    fun importConfig(): Boolean {
        val config = File(Constants.EXPORT_PATH, Constants.CONFIG_FILE)
        val privateKey = File(Constants.EXPORT_PATH, Constants.PRIVATE_KEY_FILE)
        val publicKey = File(Constants.EXPORT_PATH, Constants.PUBLIC_KEY_FILE)
        val httpsCert = File(Constants.EXPORT_PATH, Constants.HTTPS_CERT_FILE)
        val httpsKey = File(Constants.EXPORT_PATH, Constants.HTTPS_KEY_FILE)
        if (!config.exists() || !privateKey.exists() || !publicKey.exists()) return false
        shutdown(State.INIT) {
            try {
                Files.copy(config, getConfigFile(this))
                Files.copy(privateKey, getPrivateKeyFile(this))
                Files.copy(publicKey, getPublicKeyFile(this))
            } catch (e: IOException) {
                Log.w(TAG, "Failed to import config", e)
            }
            if (httpsCert.exists() && httpsKey.exists()) {
                try {
                    Files.copy(httpsCert, getHttpsCertFile(this))
                    Files.copy(httpsKey, getHttpsKeyFile(this))
                } catch (e: IOException) {
                    Log.w(TAG, "Failed to import HTTPS config files", e)
                }
            }
            launchStartupTask()
        }
        return true
    }

    companion object {
        private const val TAG = "SyncthingService"

        /**
         * Intent action to perform a Syncthing restart.
         */
        const val ACTION_RESTART: String =
            "service.syncthingandroid.nutomic.dev.SyncthingService.RESTART"

        /**
         * Intent action to reset Syncthing's database.
         */
        const val ACTION_RESET_DATABASE: String =
            "service.syncthingandroid.nutomic.dev.SyncthingService.RESET_DATABASE"

        /**
         * Intent action to reset Syncthing's delta indexes.
         */
        const val ACTION_RESET_DELTAS: String =
            "service.syncthingandroid.nutomic.dev.SyncthingService.RESET_DELTAS"

        const val ACTION_REFRESH_NETWORK_INFO: String =
            "service.syncthingandroid.nutomic.dev.SyncthingService.REFRESH_NETWORK_INFO"

        /**
         * Intent action to permanently ignore a device connection request.
         */
        const val ACTION_IGNORE_DEVICE: String =
            "service.syncthingandroid.nutomic.dev.SyncthingService.IGNORE_DEVICE"

        /**
         * Intent action to permanently ignore a folder share request.
         */
        const val ACTION_IGNORE_FOLDER: String =
            "service.syncthingandroid.nutomic.dev.SyncthingService.IGNORE_FOLDER"

        /**
         * Intent action to override folder changes.
         */
        const val ACTION_OVERRIDE_CHANGES: String =
            "service.syncthingandroid.nutomic.dev.SyncthingService.OVERRIDE_CHANGES"

        /**
         * Extra used together with ACTION_IGNORE_DEVICE, ACTION_IGNORE_FOLDER.
         */
        const val EXTRA_NOTIFICATION_ID: String =
            "service.syncthingandroid.nutomic.dev.SyncthingService.EXTRA_NOTIFICATION_ID"

        /**
         * Extra used together with ACTION_IGNORE_DEVICE
         */
        const val EXTRA_DEVICE_ID: String =
            "service.syncthingandroid.nutomic.dev.SyncthingService.EXTRA_DEVICE_ID"

        /**
         * Extra used together with ACTION_IGNORE_DEVICE
         */
        const val EXTRA_DEVICE_NAME: String =
            "service.syncthingandroid.nutomic.dev.SyncthingService.EXTRA_DEVICE_NAME"

        /**
         * Extra used together with ACTION_IGNORE_DEVICE
         */
        const val EXTRA_DEVICE_ADDRESS: String =
            "service.syncthingandroid.nutomic.dev.SyncthingService.EXTRA_DEVICE_ADDRESS"

        /**
         * Extra used together with ACTION_IGNORE_FOLDER
         */
        const val EXTRA_FOLDER_ID: String =
            "service.syncthingandroid.nutomic.dev.SyncthingService.EXTRA_FOLDER_ID"

        /**
         * Extra used together with ACTION_IGNORE_FOLDER
         */
        const val EXTRA_FOLDER_LABEL: String =
            "service.syncthingandroid.nutomic.dev.SyncthingService.EXTRA_FOLDER_LABEL"
    }
}
