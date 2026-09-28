package dev.benedek.syncthingandroid.model

import kotlinx.serialization.SerialName


/**
 * This is up to date as of Syncthing version 2.1
 *
 * The only place where I found a list of values is this snippet from *folderstate.go*
 * ```Go
 * func (s remoteFolderState) String() string {
 * 	switch s {
 * 	case remoteFolderUnknown:
 * 		return "unknown"
 * 	case remoteFolderNotSharing:
 * 		return "notSharing"
 * 	case remoteFolderPaused:
 * 		return "paused"
 * 	case remoteFolderValid:
 * 		return "valid"
 * 	default:
 * 		return "unknown"
 * 	}
 * }
 * ```
 */
enum class RemoteState {
	@SerialName("unknown")
	UNKNOWN,
	@SerialName("notSharing")
	NOT_SHARING,
	@SerialName("paused")
	PAUSED,
	@SerialName("valid")
	VALID
}
