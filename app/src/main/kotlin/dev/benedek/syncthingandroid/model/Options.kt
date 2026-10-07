package dev.benedek.syncthingandroid.model

import com.google.gson.annotations.SerializedName

/**
 * Up to date as of 2.1.0.
 *
 * Since they don't provide docs for the non-deprecated [config endpoints](https://docs.syncthing.net/v2.1.0/rest/config.html#rest-config),
 * I have to link to the DEPRECATED /rest/system/config
 * [endpoint](https://docs.syncthing.net/v2.1.0/rest/system-config-get.html).
 */
data class Options(
	var listenAddresses: Array<String?>? = null,
	var globalAnnounceServers: Array<String?>? = null,
	var globalAnnounceEnabled: Boolean = false,
	var localAnnounceEnabled: Boolean = false,
	var localAnnouncePort: Int = 0,
	var localAnnounceMCAddr: String? = null,
	var maxSendKbps: Int = 0,
	var maxRecvKbps: Int = 0,
	var reconnectionIntervalS: Int = 0,
	var relaysEnabled: Boolean = false,
	var relayReconnectIntervalM: Int = 0,
	var startBrowser: Boolean = false,
	var natEnabled: Boolean = false,
	var natLeaseMinutes: Int = 0,
	var natRenewalMinutes: Int = 0,
	var natTimeoutSeconds: Int = 0,
	var urAccepted: Int = 0,
	var urUniqueId: String? = null,
	var urURL: String? = null,
	var urPostInsecurely: Boolean = false,
	var urInitialDelayS: Int = 0,
	var autoUpgradeIntervalH: Int = 0,
	var keepTemporariesH: Int = 0,
	var progressUpdateIntervalS: Int = 0,
	var limitBandwidthInLan: Boolean = false,
	var minHomeDiskFree: MinHomeDiskFree? = null,
	var releasesURL: String? = null,
	var alwaysLocalNets: Array<String?>? = null,
	var overwriteRemoteDeviceNamesOnConnect: Boolean = false,
	var tempIndexMinBlocks: Int = 0
) {
	/**
	 * Up to date with the [docs](docs.syncthing.net/v2.1.0/users/config.html#config-option-options.minhomediskfree)
	 * as of 2.1.0
	 */
	data class MinHomeDiskFree(var value: Float, var unit: MinHomeDiskFreeUnit) {
		enum class MinHomeDiskFreeUnit {
			@SerializedName("%") PERCENT,
			@SerializedName("kB") KB,
			@SerializedName("MB") MB,
			@SerializedName("GB") GB,
			@SerializedName("TB") TB
		}
	}

	fun isUsageReportingAccepted(urVersionMax: Int): Boolean {
		return urAccepted == urVersionMax
	}

	fun isUsageReportingDecided(urVersionMax: Int): Boolean {
		return isUsageReportingAccepted(urVersionMax) || urAccepted == USAGE_REPORTING_DENIED
	}

	companion object {
		const val USAGE_REPORTING_UNDECIDED: Int = 0
		const val USAGE_REPORTING_DENIED: Int = -1
	}

	fun deepCopy(): Options = this.copy(
		listenAddresses = this.listenAddresses?.clone(),
		globalAnnounceServers = this.globalAnnounceServers?.clone(),
		alwaysLocalNets = this.alwaysLocalNets?.clone()
	)

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as Options

		if (globalAnnounceEnabled != other.globalAnnounceEnabled) return false
		if (localAnnounceEnabled != other.localAnnounceEnabled) return false
		if (localAnnouncePort != other.localAnnouncePort) return false
		if (maxSendKbps != other.maxSendKbps) return false
		if (maxRecvKbps != other.maxRecvKbps) return false
		if (reconnectionIntervalS != other.reconnectionIntervalS) return false
		if (relaysEnabled != other.relaysEnabled) return false
		if (relayReconnectIntervalM != other.relayReconnectIntervalM) return false
		if (startBrowser != other.startBrowser) return false
		if (natEnabled != other.natEnabled) return false
		if (natLeaseMinutes != other.natLeaseMinutes) return false
		if (natRenewalMinutes != other.natRenewalMinutes) return false
		if (natTimeoutSeconds != other.natTimeoutSeconds) return false
		if (urAccepted != other.urAccepted) return false
		if (urPostInsecurely != other.urPostInsecurely) return false
		if (urInitialDelayS != other.urInitialDelayS) return false
		if (autoUpgradeIntervalH != other.autoUpgradeIntervalH) return false
		if (keepTemporariesH != other.keepTemporariesH) return false
		if (progressUpdateIntervalS != other.progressUpdateIntervalS) return false
		if (limitBandwidthInLan != other.limitBandwidthInLan) return false
		if (overwriteRemoteDeviceNamesOnConnect != other.overwriteRemoteDeviceNamesOnConnect) return false
		if (tempIndexMinBlocks != other.tempIndexMinBlocks) return false
		if (!listenAddresses.contentEquals(other.listenAddresses)) return false
		if (!globalAnnounceServers.contentEquals(other.globalAnnounceServers)) return false
		if (localAnnounceMCAddr != other.localAnnounceMCAddr) return false
		if (urUniqueId != other.urUniqueId) return false
		if (urURL != other.urURL) return false
		if (minHomeDiskFree != other.minHomeDiskFree) return false
		if (releasesURL != other.releasesURL) return false
		if (!alwaysLocalNets.contentEquals(other.alwaysLocalNets)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = globalAnnounceEnabled.hashCode()
		result = 31 * result + localAnnounceEnabled.hashCode()
		result = 31 * result + localAnnouncePort
		result = 31 * result + maxSendKbps
		result = 31 * result + maxRecvKbps
		result = 31 * result + reconnectionIntervalS
		result = 31 * result + relaysEnabled.hashCode()
		result = 31 * result + relayReconnectIntervalM
		result = 31 * result + startBrowser.hashCode()
		result = 31 * result + natEnabled.hashCode()
		result = 31 * result + natLeaseMinutes
		result = 31 * result + natRenewalMinutes
		result = 31 * result + natTimeoutSeconds
		result = 31 * result + urAccepted
		result = 31 * result + urPostInsecurely.hashCode()
		result = 31 * result + urInitialDelayS
		result = 31 * result + autoUpgradeIntervalH
		result = 31 * result + keepTemporariesH
		result = 31 * result + progressUpdateIntervalS
		result = 31 * result + limitBandwidthInLan.hashCode()
		result = 31 * result + overwriteRemoteDeviceNamesOnConnect.hashCode()
		result = 31 * result + tempIndexMinBlocks
		result = 31 * result + (listenAddresses?.contentHashCode() ?: 0)
		result = 31 * result + (globalAnnounceServers?.contentHashCode() ?: 0)
		result = 31 * result + localAnnounceMCAddr.hashCode()
		result = 31 * result + urUniqueId.hashCode()
		result = 31 * result + urURL.hashCode()
		result = 31 * result + minHomeDiskFree.hashCode()
		result = 31 * result + releasesURL.hashCode()
		result = 31 * result + (alwaysLocalNets?.contentHashCode() ?: 0)
		return result
	}
}
