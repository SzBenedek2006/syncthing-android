package dev.benedek.syncthingandroid.model

import androidx.annotation.StringRes
import dev.benedek.syncthingandroid.R

enum class DeviceState(@StringRes val resId: Int) {
	Unknown(R.string.device_state_unknown),
	Paused(R.string.device_paused),
	UpToDate(R.string.device_up_to_date),
	Syncing(R.string.device_syncing),
	Disconnected(R.string.device_disconnected),
}