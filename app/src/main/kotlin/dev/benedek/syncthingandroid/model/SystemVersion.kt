package dev.benedek.syncthingandroid.model

/**
 * This contains the data returned by GET /rest/system/version
 * This is up to date as of Syncthing version 2.1
 */
class SystemVersion {
	var arch: String? = null
	var longVersion: String? = null
	var os: String? = null
	var version: String? = null
}
