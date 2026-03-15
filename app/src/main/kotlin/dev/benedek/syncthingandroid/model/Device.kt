package dev.benedek.syncthingandroid.model

import android.text.TextUtils

class Device {
    var deviceID: String? = null
    var name: String = ""
    var addresses: MutableList<String?>? = null
    var compression: String? = null
    var certName: String? = null
    var introducer: Boolean = false
    var paused: Boolean = false
    var ignoredFolders: MutableList<IgnoredFolder?>? = null

    val displayName: String?
        /**
         * Returns the device name, or the first characters of the ID if the name is empty.
         */
        get() = if (TextUtils.isEmpty(name))
            deviceID!!.substring(0, 7)
        else
            name
}
