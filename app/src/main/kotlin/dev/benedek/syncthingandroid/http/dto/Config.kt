package dev.benedek.syncthingandroid.http.dto

import dev.benedek.syncthingandroid.http.GetRequest.Companion.URI_CONFIG
import dev.benedek.syncthingandroid.model.Device
import dev.benedek.syncthingandroid.model.Folder
import dev.benedek.syncthingandroid.model.Options
import dev.benedek.syncthingandroid.model.RemoteIgnoredDevice

/**
 * This contains the data returned by GET [URI_CONFIG] (`/rest/config`)
 * This is up to date as of Syncthing version 2.1
 */
data class Config(
	var version: Int = 0,
	var folders: MutableList<Folder?>? = null,
	var devices: MutableList<Device?>? = null,
	var gui: Gui? = null,
	var options: Options? = null,
	var remoteIgnoredDevices: MutableList<RemoteIgnoredDevice?>? = null
) {
	data class Gui(
		var enabled: Boolean = false,
		var address: String? = null,
		var user: String? = null,
		var password: String? = null,
		var useTLS: Boolean = false,
		var apiKey: String? = null,
		var insecureAdminAccess: Boolean = false,
		var theme: String? = null
	)

	fun deepCopy(): Config {
		return this.copy(
			folders = this.folders?.map { it?.deepCopy() }?.toMutableList(),
			devices = this.devices?.map { it?.deepCopy() }?.toMutableList(),
			gui = this.gui?.copy(),
			options = this.options?.deepCopy(),
			remoteIgnoredDevices = this.remoteIgnoredDevices?.map { it?.copy() }?.toMutableList()
		)
	}
}
