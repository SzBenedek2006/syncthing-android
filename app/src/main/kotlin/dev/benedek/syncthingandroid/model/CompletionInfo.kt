package dev.benedek.syncthingandroid.model

// TODO
/**
 * According to syncthing REST API
 * https://docs.syncthing.net/rest/db-completion-get.html
 * 
 * completion is also returned by the events API
 * https://docs.syncthing.net/events/foldercompletion.html
 *
 * @param completion The completion percentage for the folder/device pair from 0.0 to 100.0.
 *
 *
 * The following values aren't used currently.
 * TODO: Implement the ui for them.
 *
 * @param globalBytes TODO
 * @param needBytes TODO
 * @param globalItems TODO
 * @param needItems TODO
 * @param needDeletes TODO
 * @param remoteState TODO
 * @param sequence TODO
 */
data class CompletionInfo (
	var completion: Double = 0.0, // CompletionPct float64

	var globalBytes: Long = 0L, // GlobalBytes   int64
	var needBytes: Long = 0L, // NeedBytes     int64
	var globalItems: Int = 0, // GlobalItems   int
	var needItems: Int = 0, // NeedItems     int
	var needDeletes: Int = 0, // NeedDeletes   int
	var remoteState: RemoteState = RemoteState.UNKNOWN, // RemoteState   remoteFolderState
	var sequence: Long = 0L, // Sequence      int64

)
