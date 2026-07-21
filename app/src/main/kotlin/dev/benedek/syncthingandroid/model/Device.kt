package dev.benedek.syncthingandroid.model

import android.text.TextUtils
import kotlin.math.min

data class Device (
	var deviceID: String? = null,
	var name: String = "",
	var addresses: List<String?>? = null,
	var compression: String? = null,
	var certName: String? = null,
	var introducer: Boolean = false,
	var paused: Boolean = false,
	var ignoredFolders: MutableList<IgnoredFolder?>? = null,
) {
	val displayName: String
		/**
		 * Returns the device name, or the first characters of the ID if the name is empty.
		 */
		get() = name.ifEmpty { deviceID?.substring(0, min(7, deviceID!!.length)) ?: "" }
}
