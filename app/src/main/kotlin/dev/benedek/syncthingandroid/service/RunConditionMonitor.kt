package dev.benedek.syncthingandroid.service

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SyncStatusObserver
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import dev.benedek.syncthingandroid.SyncthingApp
import dev.benedek.syncthingandroid.model.RunConditionCheckResult
import dev.benedek.syncthingandroid.model.RunConditionCheckResult.BlockerReason
import dev.benedek.syncthingandroid.service.ReceiverManager.registerReceiver
import javax.inject.Inject

// FIXME: use connectivityManager.registerDefaultNetworkCallback after it got deprecated


/**
 * Holds information about the current wifi and charging state of the device.
 * 
 * This information is actively read on instance creation, and then updated from intents
 * that are passed with [.ACTION_DEVICE_STATE_CHANGED].
 */
class RunConditionMonitor(context: Context, listener: OnRunConditionChangedListener?) {
    private var mSyncStatusObserverHandle: Any? = null
    private val mSyncStatusObserver: SyncStatusObserver = SyncStatusObserver { updateShouldRunDecision() }

    fun interface OnRunConditionChangedListener {
        fun onRunConditionChanged(result: RunConditionCheckResult?)
    }

    private val mContext: Context

    @JvmField
    @Inject
    var mPreferences: SharedPreferences? = null
    private val mReceiverManager: ReceiverManager? = null

    /**
     * Sending callback notifications through [OnRunConditionChangedListener] is enabled if not null.
     */
    private var mOnRunConditionChangedListener: OnRunConditionChangedListener? = null

    /**
     * Stores the result of the last call to [.decideShouldRun].
     */
    private var lastRunConditionCheckResult: RunConditionCheckResult? = null

    init {
        Log.v(TAG, "Created new instance")
        (context.applicationContext as SyncthingApp).component().inject(this)
        mContext = context
        mOnRunConditionChangedListener = listener

        /**
         * Register broadcast receivers.
         */
        // NetworkReceiver
        registerReceiver(
            mContext,
            NetworkReceiver(),
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )

        // BatteryReceiver
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_POWER_CONNECTED)
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
        registerReceiver(mContext, this.BatteryReceiver(), filter)

        // PowerSaveModeChangedReceiver
        registerReceiver(
            mContext,
            PowerSaveModeChangedReceiver(),
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        )

        // SyncStatusObserver to monitor android's "AutoSync" quick toggle.
        mSyncStatusObserverHandle = ContentResolver.addStatusChangeListener(
            ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS, mSyncStatusObserver
        )

        // Initially determine if syncthing should run under current circumstances.
        updateShouldRunDecision()
    }

    fun shutdown() {
        Log.v(TAG, "Shutting down")
        if (mSyncStatusObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncStatusObserverHandle)
            mSyncStatusObserverHandle = null
        }
        mReceiverManager!!.unregisterAllReceivers(mContext)
    }

    private inner class BatteryReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (Intent.ACTION_POWER_CONNECTED == intent.action
                || Intent.ACTION_POWER_DISCONNECTED == intent.action
            ) {
                val handler = Handler()
                handler.postDelayed({ updateShouldRunDecision() }, 5000)
            }
        }
    }

    private inner class NetworkReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
                updateShouldRunDecision()
            }
        }
    }

    private inner class PowerSaveModeChangedReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (PowerManager.ACTION_POWER_SAVE_MODE_CHANGED == intent.action) {
                updateShouldRunDecision()
            }
        }
    }

    fun updateShouldRunDecision() {
        // Reason if the current conditions changed the result of decideShouldRun()
        // compared to the last determined result.
        val result = decideShouldRun()
        val change: Boolean
        synchronized(this) {
            change = lastRunConditionCheckResult == null || lastRunConditionCheckResult != result
            lastRunConditionCheckResult = result
        }
        if (change) {
            if (mOnRunConditionChangedListener != null) {
                mOnRunConditionChangedListener!!.onRunConditionChanged(result)
            }
        }
    }

    /**
     * Determines if Syncthing should currently run.
     */
    private fun decideShouldRun(): RunConditionCheckResult {
        // Get run conditions preferences.
        val prefRunConditions = mPreferences!!.getBoolean(Constants.PREF_RUN_CONDITIONS, true)
        val prefRunOnMobileData =
            mPreferences!!.getBoolean(Constants.PREF_RUN_ON_MOBILE_DATA, false)
        val prefRunOnWifi = mPreferences!!.getBoolean(Constants.PREF_RUN_ON_WIFI, true)
        val prefRunOnMeteredWifi =
            mPreferences!!.getBoolean(Constants.PREF_RUN_ON_METERED_WIFI, false)
        val whitelistedWifiSsids: MutableSet<String?> = mPreferences!!.getStringSet(
            Constants.PREF_WIFI_SSID_WHITELIST,
            java.util.HashSet()
        )!!
        val prefWifiWhitelistEnabled = !whitelistedWifiSsids.isEmpty()
        val prefRunInFlightMode =
            mPreferences!!.getBoolean(Constants.PREF_RUN_IN_FLIGHT_MODE, false)
        val prefPowerSource: String = mPreferences!!.getString(
            Constants.PREF_POWER_SOURCE,
            POWER_SOURCE_CHARGER_BATTERY
        )!!
        val prefRespectPowerSaving =
            mPreferences!!.getBoolean(Constants.PREF_RESPECT_BATTERY_SAVING, true)
        val prefRespectMasterSync =
            mPreferences!!.getBoolean(Constants.PREF_RESPECT_MASTER_SYNC, false)

        if (!prefRunConditions) {
            Log.v(TAG, "decideShouldRun: !runConditions")
            return RunConditionCheckResult.SHOULD_RUN
        }

        val blockerReasons: MutableList<BlockerReason?> = ArrayList()

        // PREF_POWER_SOURCE
        when (prefPowerSource) {
            POWER_SOURCE_CHARGER -> if (!this.isCharging) {
                Log.v(TAG, "decideShouldRun: POWER_SOURCE_AC && !isCharging")
                blockerReasons.add(BlockerReason.ON_BATTERY)
            }

            POWER_SOURCE_BATTERY -> if (this.isCharging) {
                Log.v(TAG, "decideShouldRun: POWER_SOURCE_BATTERY && isCharging")
                blockerReasons.add(BlockerReason.ON_CHARGER)
            }

            POWER_SOURCE_CHARGER_BATTERY -> {}
            else -> {}
        }

        // Power saving
        if (prefRespectPowerSaving && this.isPowerSaving) {
            Log.v(TAG, "decideShouldRun: prefRespectPowerSaving && isPowerSaving")
            blockerReasons.add(BlockerReason.POWERSAVING_ENABLED)
        }

        // Android global AutoSync setting.
        if (prefRespectMasterSync && !ContentResolver.getMasterSyncAutomatically()) {
            Log.v(TAG, "decideShouldRun: prefRespectMasterSync && !getMasterSyncAutomatically")
            blockerReasons.add(BlockerReason.GLOBAL_SYNC_DISABLED)
        }

        // Run on mobile data.
        if (blockerReasons.isEmpty() && prefRunOnMobileData && this.isMobileDataConnection) {
            Log.v(TAG, "decideShouldRun: prefRunOnMobileData && isMobileDataConnection")
            return RunConditionCheckResult.SHOULD_RUN
        }

        // Run on wifi.
        if (prefRunOnWifi && this.isWifiOrEthernetConnection) {
            if (prefRunOnMeteredWifi) {
                // We are on non-metered or metered wifi. Reason if wifi whitelist run condition is met.
                if (wifiWhitelistConditionMet(prefWifiWhitelistEnabled, whitelistedWifiSsids)) {
                    Log.v(
                        TAG,
                        "decideShouldRun: prefRunOnWifi && isWifiOrEthernetConnection && prefRunOnMeteredWifi && wifiWhitelistConditionMet"
                    )
                    if (blockerReasons.isEmpty()) return RunConditionCheckResult.SHOULD_RUN
                } else {
                    blockerReasons.add(BlockerReason.WIFI_SSID_NOT_WHITELISTED)
                }
            } else {
                // Reason if we are on a non-metered wifi and if wifi whitelist run condition is met.
                if (!this.isMeteredNetworkConnection) {
                    if (wifiWhitelistConditionMet(prefWifiWhitelistEnabled, whitelistedWifiSsids)) {
                        Log.v(
                            TAG,
                            "decideShouldRun: prefRunOnWifi && isWifiOrEthernetConnection && !prefRunOnMeteredWifi && !isMeteredNetworkConnection && wifiWhitelistConditionMet"
                        )
                        if (blockerReasons.isEmpty()) return RunConditionCheckResult.SHOULD_RUN
                    } else {
                        blockerReasons.add(BlockerReason.WIFI_SSID_NOT_WHITELISTED)
                    }
                } else {
                    blockerReasons.add(BlockerReason.WIFI_WIFI_IS_METERED)
                }
            }
        }

        // Run in flight mode.
        if (prefRunInFlightMode && this.isFlightMode) {
            Log.v(TAG, "decideShouldRun: prefRunInFlightMode && isFlightMode")
            if (blockerReasons.isEmpty()) return RunConditionCheckResult.SHOULD_RUN
        }

        /**
         * If none of the above run conditions matched, don't run.
         */
        Log.v(TAG, "decideShouldRun: return false")
        if (blockerReasons.isEmpty()) {
            if (this.isFlightMode) {
                blockerReasons.add(BlockerReason.NO_NETWORK_OR_FLIGHTMODE)
            } else if (!prefRunOnWifi && !prefRunOnMobileData) {
                blockerReasons.add(BlockerReason.NO_ALLOWED_NETWORK)
            } else if (prefRunOnMobileData) {
                blockerReasons.add(BlockerReason.NO_MOBILE_CONNECTION)
            } else if (prefRunOnWifi) { // FIXME
                blockerReasons.add(BlockerReason.NO_WIFI_CONNECTION)
            } else {
                blockerReasons.add(BlockerReason.NO_NETWORK_OR_FLIGHTMODE)
            }
        }
        return RunConditionCheckResult(blockerReasons)
    }

    /**
     * Return whether the wifi whitelist run condition is met.
     * Precondition: An active wifi connection has been detected.
     */
    private fun wifiWhitelistConditionMet(
        prefWifiWhitelistEnabled: Boolean,
        whitelistedWifiSsids: MutableSet<String?>
    ): Boolean {
        if (!prefWifiWhitelistEnabled) {
            Log.v(TAG, "handleWifiWhitelist: !prefWifiWhitelistEnabled")
            return true
        }
        if (isWifiConnectionWhitelisted(whitelistedWifiSsids)) {
            Log.v(TAG, "handleWifiWhitelist: isWifiConnectionWhitelisted")
            return true
        }
        return false
    }

    private val isCharging: Boolean
        /**
         * Functions for run condition information retrieval.
         */
        get() {
            val intent = mContext.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val plugged = intent!!.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
        }

    private val isPowerSaving: Boolean
        get() {
            val powerManager =
                mContext.getSystemService(Context.POWER_SERVICE) as PowerManager?
            if (powerManager == null) {
                Log.e(
                    TAG,
                    "getSystemService(POWER_SERVICE) unexpectedly returned NULL."
                )
                return false
            }
            return powerManager.isPowerSaveMode
        }

    private val isFlightMode: Boolean
        get() {
            val cm =
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val ni = cm.activeNetworkInfo
            return ni == null
        }

    private val isMeteredNetworkConnection: Boolean
        get() {
            val cm =
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val ni = cm.activeNetworkInfo ?: // In flight mode.
            return false
            if (!ni.isConnected) {
                // No network connection.
                return false
            }
            return cm.isActiveNetworkMetered
        }

    private val isMobileDataConnection: Boolean
        get() {
            val cm =
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val ni = cm.activeNetworkInfo ?: // In flight mode.
            return false
            if (!ni.isConnected) {
                // No network connection.
                return false
            }
            return when (ni.type) {
                ConnectivityManager.TYPE_BLUETOOTH, ConnectivityManager.TYPE_MOBILE, ConnectivityManager.TYPE_MOBILE_DUN, ConnectivityManager.TYPE_MOBILE_HIPRI -> true
                else -> false
            }
        }

    private val isWifiOrEthernetConnection: Boolean
        get() {
            val cm =
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val ni = cm.activeNetworkInfo ?: // In flight mode.
            return false
            if (!ni.isConnected) {
                // No network connection.
                return false
            }
            return when (ni.type) {
                ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_WIMAX, ConnectivityManager.TYPE_ETHERNET -> true
                else -> false
            }
        }

    private fun isWifiConnectionWhitelisted(whitelistedSsids: MutableSet<String?>): Boolean {
        val wifiManager = mContext.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        if (wifiInfo == null) {
            // May be null, if wifi has been turned off in the meantime.
            Log.d(TAG, "isWifiConnectionWhitelisted: SSID unknown due to wifiInfo == null")
            return false
        }
        val wifiSsid = wifiInfo.getSSID()
        if (wifiSsid == null) {
            Log.w(
                TAG,
                "isWifiConnectionWhitelisted: Got null SSID. Try to enable android location service."
            )
            return false
        }
        return whitelistedSsids.contains(wifiSsid)
    }

    companion object {
        private const val TAG = "RunConditionMonitor"

        private const val POWER_SOURCE_CHARGER_BATTERY = "ac_and_battery_power"
        private const val POWER_SOURCE_CHARGER = "ac_power"
        private const val POWER_SOURCE_BATTERY = "battery_power"
    }
}
