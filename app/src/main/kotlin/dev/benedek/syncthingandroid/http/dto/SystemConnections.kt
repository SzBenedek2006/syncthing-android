package dev.benedek.syncthingandroid.http.dto

import dev.benedek.syncthingandroid.http.GetRequest.Companion.URI_CONNECTIONS
import dev.benedek.syncthingandroid.model.DeviceState
import kotlin.math.max

/**
 *
 * This contains the data returned by [URI_CONNECTIONS] (`/rest/system/connections` )
 * This is up to date as of Syncthing version 2.1
 */
class SystemConnections {
	var total: TotalStatus? = null

	// DeviceID - Connection data
	/**
	 * key: DeviceId
	 * value: DeviceStatus
	 */
	var connections: MutableMap<String?, DeviceStatus?>? = null

	class DeviceStatus {
		var address: String? = null
		var at: String? = null
		var clientVersion: String? = null
		var connected: Boolean = false
		var inBytesTotal: Long = 0
		var isLocal: Boolean = false
		var outBytesTotal: Long = 0
		var paused: Boolean = false
		var startedAt: String? = null
		var type: String? = null

		// These fields are not sent from Syncthing, but are populated on the client side.
		var completion: Int = 0
		var inBits: Long = 0
		var outBits: Long = 0
		var state: DeviceState = DeviceState.Unknown

		fun setTransferRate(previous: DeviceStatus, msElapsed: Long) {
			val secondsElapsed = msElapsed / 1000
			val inBytes = 8 * (inBytesTotal - previous.inBytesTotal) / secondsElapsed
			val outBytes = 8 * (outBytesTotal - previous.outBytesTotal) / secondsElapsed
			inBits = max(0, inBytes)
			outBits = max(0, outBytes)
		}

	}

	class TotalStatus {
		var at: String? = null
		var inBytesTotal: Long = 0
		var outBytesTotal: Long = 0


		// Fields not sent by Syncthing
		var inBits: Long = 0
		var outBits: Long = 0

		fun setTransferRate(previous: TotalStatus, msElapsed: Long) {
			val secondsElapsed = msElapsed / 1000
			val inBytes = 8 * (inBytesTotal - previous.inBytesTotal) / secondsElapsed
			val outBytes = 8 * (outBytesTotal - previous.outBytesTotal) / secondsElapsed
			inBits = max(0, inBytes)
			outBits = max(0, outBytes)
		}
	}
}