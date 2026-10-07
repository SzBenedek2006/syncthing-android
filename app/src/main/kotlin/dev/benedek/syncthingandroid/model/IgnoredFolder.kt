package dev.benedek.syncthingandroid.model

import android.text.TextUtils

data class IgnoredFolder(
	var time: String = "",
	var id: String = "",
	var label: String = ""
) {

	/**
	 * Returns the folder label, or the first characters of the ID if the label is empty.
	 */
	val displayLabel: String?
		get() = if (TextUtils.isEmpty(label))
			id.substring(0, 7)
		else
			label

}
