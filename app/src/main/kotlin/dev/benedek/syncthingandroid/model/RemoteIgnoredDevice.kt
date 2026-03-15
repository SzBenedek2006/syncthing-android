package dev.benedek.syncthingandroid.model

import android.text.TextUtils

class RemoteIgnoredDevice {
    var time: String = ""
    var deviceID: String = ""
    var name: String = ""
    var address: String = ""

    val displayName: String?
        /**
         * Returns the device name, or the first characters of the ID if the name is empty.
         */
        get() = if (TextUtils.isEmpty(name))
            deviceID.substring(0, 7)
        else
            name
}
