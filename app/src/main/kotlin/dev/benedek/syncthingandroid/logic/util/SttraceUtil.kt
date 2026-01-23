package dev.benedek.syncthingandroid.logic.util

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import dev.benedek.syncthingandroid.service.Constants
import java.util.ArrayList
import java.util.HashSet

object SttraceUtil {

    private const val TAG = "SttraceUtil"

    fun getDebugFacilities(prefs: SharedPreferences): List<String> {
        val retDebugFacilities = ArrayList<String>()
        val availableDebugFacilities: MutableSet<String>? = prefs.getStringSet(Constants.PREF_DEBUG_FACILITIES_AVAILABLE, null)

        if (!availableDebugFacilities.isNullOrEmpty()) {
            //retDebugFacilities.addAll(availableDebugFacilities)
            prefs.edit {
                remove(Constants.PREF_DEBUG_FACILITIES_AVAILABLE)
            }
        } else {
            Log.w(TAG, "getDebugFacilities: Failed to get facilities from prefs, falling back to hardcoded list.")

            /*
            // Syncthing v0.14.47 debug facilities.
            retDebugFacilities.add("beacon")
            retDebugFacilities.add("config")
            retDebugFacilities.add("connections")
            retDebugFacilities.add("db")
            retDebugFacilities.add("dialer")
            retDebugFacilities.add("discover")
            retDebugFacilities.add("events")
            retDebugFacilities.add("fs")
            retDebugFacilities.add("http")
            retDebugFacilities.add("main")
            retDebugFacilities.add("model")
            retDebugFacilities.add("nat")
            retDebugFacilities.add("pmp")
            retDebugFacilities.add("protocol")
            retDebugFacilities.add("scanner")
            retDebugFacilities.add("sha256")
            retDebugFacilities.add("stats")
            retDebugFacilities.add("sync")
            retDebugFacilities.add("upgrade")
            retDebugFacilities.add("upnp")
            retDebugFacilities.add("versioner")
            retDebugFacilities.add("walkfs")
            retDebugFacilities.add("watchaggregator")
            */


            // Syncthing v2.0.13 debug facilities.
            retDebugFacilities.add("api")
            retDebugFacilities.add("beacon")
            retDebugFacilities.add("config")
            retDebugFacilities.add("connections")
            retDebugFacilities.add("db/sqlite")
            retDebugFacilities.add("dialer")
            retDebugFacilities.add("discover")
            retDebugFacilities.add("events")
            retDebugFacilities.add("fs")
            retDebugFacilities.add("main")
            retDebugFacilities.add("model")
            retDebugFacilities.add("nat")
            retDebugFacilities.add("pmp")
            retDebugFacilities.add("protocol")
            retDebugFacilities.add("relay/client")
            retDebugFacilities.add("scanner")
            retDebugFacilities.add("stun")
            retDebugFacilities.add("syncthing")
            retDebugFacilities.add("upgrade")
            retDebugFacilities.add("upnp")
            retDebugFacilities.add("ur")
            retDebugFacilities.add("versioner")
            retDebugFacilities.add("watchaggregator")

        }

        retDebugFacilities.sort()
        return retDebugFacilities
    }
}
