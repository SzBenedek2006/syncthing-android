package dev.benedek.syncthingandroid.http.dto

/**
 * This contains the data returned by GET [dev.benedek.syncthingandroid.http.GetRequest.Companion.URI_SYSTEM] (`/rest/system/status` )
 * This is up to date as of Syncthing version 2.1
 *
 * @property sys RAM used in bytes
 */
class SystemStatus {
	var alloc: Long = 0
	var cpuPercent: Double = 0.0
	var discoveryEnabled: Boolean = false

	@Deprecated(
		message = "Deprecated in Syncthing v1.18.0: use discoveryStatus instead.",
		replaceWith = ReplaceWith("discoveryStatus?.mapValues { it.value.error }?.filterValues { it != null }")
	)
	var discoveryErrors: MutableMap<String, String>? = null
	var discoveryStatus: Map<String, DiscoveryEntry>? = null

	@Deprecated(
		message = "Deprecated in Syncthing v1.18.0: use discoveryStatus.size instead.",
		replaceWith = ReplaceWith("discoveryStatus?.size ?: 0")
	)
	var discoveryMethods: Int = 0
	var goroutines: Int = 0
	var myID: String? = null

	var sys: Long = 0

	data class DiscoveryEntry(
		val error: String? = null
	)
}