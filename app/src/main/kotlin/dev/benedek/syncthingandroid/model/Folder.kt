package dev.benedek.syncthingandroid.model

import android.text.TextUtils
import dev.benedek.syncthingandroid.service.Constants
import java.io.Serializable

val String.isValidDefault: Boolean
    get() {
        val regex = Regex("^[a-z0-9]{5}-[a-z0-9]{5}$")
        return regex.matches(this)
    }
val String.isValid: Boolean
    get() {
        val regex = Regex("^[a-z0-9](?:[a-z0-9._-]{0,61}[a-z0-9])?\$")
        return regex.matches(this)
    }

class Folder {
    var id: String? = null
    var label: String? = null
    var filesystemType: String = "basic"
    var path: String? = null
    var type: String = Constants.FOLDER_TYPE_SEND_RECEIVE
    var fsWatcherEnabled: Boolean = true
    var fsWatcherDelayS: Int = 10
    val devices: MutableList<Device> = ArrayList<Device>()
    var rescanIntervalS: Int = 0
    val ignorePerms: Boolean = true
    var autoNormalize: Boolean = true
    var minDiskFree: MinDiskFree? = null
    var versioning: Versioning? = null
    var copiers: Int = 0
    var pullerMaxPendingKiB: Int = 0
    var hashers: Int = 0
    var order: String? = null
    var ignoreDelete: Boolean = false
    var scanProgressIntervalS: Int = 0
    var pullerPauseS: Int = 0
    var maxConflicts: Int = 10
    var disableSparseFiles: Boolean = false
    var disableTempIndexes: Boolean = false
    var paused: Boolean = false
    var useLargeBlocks: Boolean = false
    var weakHashThresholdPct: Int = 25
    var markerName: String = ".stfolder"
    var invalid: String? = null

    class Versioning : Serializable {
        var type: String? = null
        var params: MutableMap<String?, String?> = HashMap()

        fun deepCopy(): Versioning {
            val copy = Versioning()
            copy.type = type
            copy.params = HashMap(params)
            return copy
        }
    }



    class MinDiskFree {
        var value: Float = 0f
        var unit: String? = null
    }

    fun addDevice(deviceId: String?) {
        val device = Device()
        device.deviceID = deviceId
        devices.add(device)
    }

    fun getDevice(deviceId: String?): Device? {
        for (d in devices) {
            if (d.deviceID == deviceId) {
                return d
            }
        }
        return null
    }

    fun removeDevice(deviceId: String?) {
        val it = devices.iterator()
        while (it.hasNext()) {
            val currentId = it.next().deviceID!!
            if (currentId == deviceId) {
                it.remove()
            }
        }
    }

    override fun toString(): String {
        return (if (!TextUtils.isEmpty(label)) label else id)!!
    }

    class Device {
        var deviceID: String? = null
        var introducedBy: String? = null
        var encryptionPassword: String? = null
    }
}