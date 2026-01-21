package dev.benedek.syncthingandroid.logic.util

import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.util.Log
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.util.PermissionUtil
import java.util.HashSet

/**
 * Logic based on the old WifiSsidPreference.
 *
 * Setting can be "All networks" (none selected), or selecting individual networks.
 *
 * Due to restrictions in Android, it is possible/likely, that the list of saved WiFi networks
 * cannot be retrieved if the WiFi is turned off. In this case, an explanation is shown.
 *
 * The preference is stored as Set&lt;String&gt; where an empty set represents
 * "all networks allowed".
 *
 */
object WifiSsidUtil {

    data class WifiListResult(
        val displayEntries: List<String>,
        val entryValues: List<String>,
        val messageId: Int? = null, // Resource ID for Toast, if any
        val needsPermissionRequest: Boolean = false
    )


    fun calculateWifiList(
        context: Context,
        prefs: SharedPreferences,
        key: String
    ): WifiListResult {
        val selectedSet = prefs.getStringSet(key, emptySet())!!
        val allSsids = HashSet(selectedSet)

        var connected = false
        try {
            val currentSsid = getCurrentSsid(context)

            if (isValidSsid(currentSsid)) {
                allSsids.add(currentSsid!!)
                connected = true
            } else {
                Log.d(this.toString(), "Couldn't get current SSID list")
            }

        } catch (e: Exception) {
            Log.e(this.toString() ,"Something happened", e)
        }


        val hasPerms = PermissionUtil.hasLocationPermissions(context)
        var messageId: Int? = null

        if (!connected) {
            messageId = if (!hasPerms) {
                R.string.sync_only_wifi_ssids_need_to_grant_location_permission
            } else {
                R.string.sync_only_wifi_ssids_connect_to_wifi
            }
        }

        val ssidsOrderedList = allSsids.toMutableList()
        val displayEntries = stripQuotes(ssidsOrderedList)
        val entryValues = ssidsOrderedList.filterNotNull()

        return WifiListResult(
            displayEntries = displayEntries.toList(),
            entryValues = entryValues,
            messageId = messageId,
            needsPermissionRequest = !hasPerms && !connected
        )
    }

    private fun getCurrentSsid(context: Context): String? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        var wifiInfo: WifiInfo? = null

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Android 12+ (API 31+): Use ConnectivityManager
            val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return null

            val activeNetwork = connManager.activeNetwork ?: return null
            val capabilities = connManager.getNetworkCapabilities(activeNetwork) ?: return null

            // Check if this network is actually Wifi
            if (!capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) {
                return null
            }

            wifiInfo = capabilities.transportInfo as? android.net.wifi.WifiInfo

        }

        if (isValidSsid(wifiInfo?.ssid)) {
            return wifiInfo?.ssid
        }


        // Android 11 and older: Use the old WifiManager way
        @Suppress("DEPRECATION")
        return wifiManager?.connectionInfo?.ssid

    }

    /**
     * Returns true if the ssid not null, empty or "unknown ssid"
     */
    private fun isValidSsid(ssid: String?): Boolean {
        return !ssid.isNullOrEmpty() && !ssid.contains("unknown ssid", ignoreCase = true)
    }

    /**
     * Returns a copy of the given WiFi SSIDs with quotes stripped.
     *
     * @param ssids the list of ssids to strip quotes from
     */
    fun stripQuotes(ssids: MutableList<String?>): Array<String> {
        val result = arrayOfNulls<String>(ssids.size)
        for (i in ssids.indices) {
            result[i] =
                ssids[i]!!.replaceFirst("^\"".toRegex(), "").replaceFirst("\"$".toRegex(), "")
        }
        return result.requireNoNulls()
    }

    // Helper for Summary (single item)
    fun stripQuotes(ssid: String): String {
        return ssid.replaceFirst("^\"".toRegex(), "").replaceFirst("\"$".toRegex(), "")
    }
}