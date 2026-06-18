package dev.benedek.syncthingandroid.model


/**
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
enum class RemoteFolderState {
	UNKNOWN,
	NOT_SHARING,
	PAUSED,
	VALID
}