package dev.benedek.syncthingandroid.model

import android.text.TextUtils

/**
 * TODO: Move to dto
 */
data class RemoteIgnoredDevice(
	var time: String = "",
	var deviceID: String = "",
	var name: String = "",
	var address: String = "",
) {
	/**
	 * Returns the device name, or the first characters of the ID if the name is empty.
	 */
	val displayName: String?
		get() = if (TextUtils.isEmpty(name))
			deviceID.substring(0, 7)
		else
			name
}
