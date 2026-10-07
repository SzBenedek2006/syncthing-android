package dev.benedek.syncthingandroid.model

import androidx.annotation.StringRes
import dev.benedek.syncthingandroid.R

interface Sort {
	@get:StringRes
	val resId: Int
}

enum class FolderSort(@StringRes override val resId: Int) : Sort {
	LABEL(R.string.folder_label),
	DATE(R.string.modified),
	STATE(R.string.state),
	PAUSED(R.string.state_paused),
	LOCAL_SIZE(R.string.local_size),
	GLOBAL_SIZE(R.string.global_size)
}

enum class DeviceSort(@StringRes override val resId: Int) : Sort {
	NAME(R.string.name),
	DATE(R.string.modified),
	STATE(R.string.state),
	PAUSED(R.string.state_paused),
	DOWNLOAD_SPEED(R.string.download_speed),
	UPLOAD_SPEED(R.string.upload_speed)
}
