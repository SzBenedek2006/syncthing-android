package dev.benedek.syncthingandroid.model

import android.util.Log
import kotlin.math.floor

/**
 * This class caches remote folder and device synchronization
 * completion indicators defined in [CompletionInfo.CompletionInfo]
 * according to syncthing's REST "/completion" JSON result schema.
 * Completion model of syncthing's web UI is completion[deviceId][folderId]
 */
class Completion {
    var deviceFolderMap: HashMap<String?, HashMap<String?, CompletionInfo?>> =
        HashMap()

    /**
     * Removes a folder from the cache model.
     */
    private fun removeFolder(folderId: String?) {
        for (folderMap in deviceFolderMap.values) {
            if (folderMap.containsKey(folderId)) {
                folderMap.remove(folderId)
                break
            }
        }
    }

    /**
     * Updates device and folder information in the cache model
     * after a config update.
     */
    fun updateFromConfig(newDevices: MutableList<Device>?, newFolders: MutableList<Folder?>?) {
        var folderMap: HashMap<String?, CompletionInfo?>?

        // Handle devices that were removed from the config.
        val removedDevices: MutableList<String?> = ArrayList()
        var deviceFound: Boolean
        for (deviceId in deviceFolderMap.keys) {
            deviceFound = false
            if (newDevices != null) {
                for (device in newDevices) {
                    if (device.deviceID == deviceId) {
                        deviceFound = true
                        break
                    }
                }
            }
            if (!deviceFound) {
                removedDevices.add(deviceId)
            }
        }
        for (deviceId in removedDevices) {
            Log.v(TAG, "updateFromConfig: Remove device '$deviceId' from cache model")
            deviceFolderMap.remove(deviceId)
        }

        // Handle devices that were added to the config.
        if (newDevices != null) {
            for (device in newDevices) {
                if (!deviceFolderMap.containsKey(device.deviceID)) {
                    Log.v(
                        TAG,
                        "updateFromConfig: Add device '" + device.deviceID + "' to cache model"
                    )
                    deviceFolderMap[device.deviceID] = HashMap()
                }
            }
        }

        // Handle folders that were removed from the config.
        val removedFolders: MutableList<String?> = ArrayList()
        var folderFound: Boolean
        for (device in deviceFolderMap.entries) {
            for (folderId in device.value.keys) {
                folderFound = false
                if (newFolders != null) {
                    for (folder in newFolders) {
                        if (folder?.id == folderId) {
                            folderFound = true
                            break
                        }
                    }
                }
                if (!folderFound) {
                    removedFolders.add(folderId)
                }
            }
        }
        for (folderId in removedFolders) {
            Log.v(TAG, "updateFromConfig: Remove folder '$folderId' from cache model")
            removeFolder(folderId)
        }

        // Handle folders that were added to the config.
        if (newFolders != null && newDevices != null) {
            for (folder in newFolders) {
                for (device in newDevices) {
                    if (folder?.getDevice(device.deviceID) != null) {
                        // folder is shared with device.
                        folderMap = deviceFolderMap[device.deviceID]
                        if (!folderMap!!.containsKey(folder.id)) {
                            Log.v(
                                TAG, "updateFromConfig: Add folder '" + folder.id +
                                        "' shared with device '" + device.deviceID + "' to cache model."
                            )
                            folderMap[folder.id] = CompletionInfo()
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculates remote device sync completion percentage across all folders
     * shared with the device.
     */
    fun getDeviceCompletion(deviceId: String?): Int {
        var folderCount = 0
        var sumCompletion = 0.0
        val folderMap = deviceFolderMap[deviceId]
        if (folderMap != null) {
            for (folder in folderMap.entries) {
                sumCompletion += folder.value!!.completion
                folderCount++
            }
        }
        return if (folderCount == 0) {
            100
        } else {
            floor(sumCompletion / folderCount).toInt()
        }
    }

    /**
     * Set completionInfo within the completion[deviceId][folderId] model.
     */
    fun setCompletionInfo(
        deviceId: String?, folderId: String?,
        completionInfo: CompletionInfo?
    ) {
        // Add device parent node if it does not exist.
        if (!deviceFolderMap.containsKey(deviceId)) {
            deviceFolderMap[deviceId] = HashMap()
        }
        // Add folder or update existing folder entry.
        deviceFolderMap[deviceId]!![folderId] = completionInfo
    }

    companion object {
        private const val TAG = "Completion"
    }
}
