package dev.benedek.syncthingandroid.model

class SystemInfo {
	var alloc: Long = 0
	var cpuPercent: Double = 0.0
	var goroutines: Int = 0
	var myID: String? = null

	// sys = ram
	var sys: Long = 0
	var discoveryEnabled: Boolean = false
	@Deprecated(
		message = "Deprecated in Syncthing v1.18.0: use discoveryStatus.size instead.",
		replaceWith = ReplaceWith("discoveryStatus?.size ?: 0")
	)
	var discoveryMethods: Int = 0
	@Deprecated(
		message = "Deprecated in Syncthing v1.18.0: use discoveryStatus instead.",
		replaceWith = ReplaceWith("discoveryStatus?.mapValues { it.value.error }?.filterValues { it != null }")
	)
	var discoveryErrors: MutableMap<String, String>? = null
	var discoveryStatus: Map<String, DiscoveryEntry>? = null
	var urVersionMax: Int = 0

	data class DiscoveryEntry(
		val error: String? = null
	)
}
