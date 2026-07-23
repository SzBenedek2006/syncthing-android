package dev.benedek.syncthingandroid.model

import android.os.Parcelable
import dev.benedek.syncthingandroid.service.Constants
import kotlinx.parcelize.Parcelize

@Parcelize
data class Folder(
	var id: String? = null,
	var label: String? = null,
	var filesystemType: String = "basic",
	var path: String? = null,
	var type: String = Constants.FOLDER_TYPE_SEND_RECEIVE,
	var fsWatcherEnabled: Boolean = true,
	var fsWatcherDelayS: Int = 10,
	val devices: MutableList<Device> = mutableListOf(),
	var rescanIntervalS: Int = 0,
	val ignorePerms: Boolean = true,
	var autoNormalize: Boolean = true,
	var minDiskFree: MinDiskFree? = null,
	var versioning: Versioning? = null,
	var copiers: Int = 0,
	var pullerMaxPendingKiB: Int = 0,
	var hashers: Int = 0,
	var order: String? = null,
	var ignoreDelete: Boolean = false,
	var scanProgressIntervalS: Int = 0,
	var pullerPauseS: Int = 0,
	var maxConflicts: Int = 10,
	var disableSparseFiles: Boolean = false,
	var disableTempIndexes: Boolean = false,
	var paused: Boolean = false,
	var useLargeBlocks: Boolean = false,
	var weakHashThresholdPct: Int = 25,
	var markerName: String = ".stfolder",
	var invalid: String? = null,
) : Parcelable {
	companion object {
		private val validDefaultRegex = Regex("^[a-z0-9]{5}-[a-z0-9]{5}$")
		private val validRegex = Regex("^[a-z0-9](?:[a-z0-9._-]{0,61}[a-z0-9])?$")

		fun isValidDefaultId(id: String?): Boolean =
			if (id.isNullOrEmpty()) false else validDefaultRegex.matches(id)
		fun isValidId(id: String?): Boolean =
			if (id.isNullOrEmpty()) false else validRegex.matches(id)
	}
	@Parcelize
	data class Versioning(
		var type: String? = null,
		var params: MutableMap<String?, String?> = mutableMapOf()
	) : Parcelable {
		fun deepCopy() = copy(params = params.toMutableMap())
	}

	@Parcelize
	data class MinDiskFree(
		var value: Float = 0f,
		var unit: String? = null
	) : Parcelable

	fun addDevice(deviceId: String) {
		devices.add(Device(deviceID = deviceId))
	}

	/**
	 * Finds the [Device] within this folder.
	 *
	 * @param deviceId The ID of the device to look up.
	 * @return The [Device] object if it's in the shared devices list; `null` otherwise.
	 */
	fun getDevice(deviceId: String?): Device? = devices.find { it.deviceID == deviceId }

	fun removeDevice(deviceId: String?) = devices.removeAll { it.deviceID == deviceId }

	override fun toString(): String {
		return label.takeUnless { it.isNullOrEmpty() } ?: id ?: ""
	}

	@Parcelize
	data class Device(
		var deviceID: String? = null,
		var introducedBy: String? = null,
		var encryptionPassword: String? = null
	) : Parcelable
}