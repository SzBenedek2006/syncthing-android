package dev.benedek.syncthingandroid.http.dto

import androidx.annotation.StringRes
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.http.GetRequest.Companion.URI_CONNECTIONS

/**
 * This contains the data returned by GET [URI_CONNECTIONS] (`/rest/system/connections` )
 * This is up to date as of Syncthing version 2.1
 */
data class DbStatus(
	var globalBytes: Long = 0,
	var globalDeleted: Long = 0,
	var globalDirectories: Long = 0,
	var globalFiles: Long = 0,
	var globalSymlinks: Long = 0,
	var ignorePatterns: Boolean = false,
	var invalid: String? = null,
	var localBytes: Long = 0,
	var localDeleted: Long = 0,
	var localDirectories: Long = 0,
	var localSymlinks: Long = 0,
	var localFiles: Long = 0,
	var inSyncBytes: Long = 0,
	var inSyncFiles: Long = 0,
	var needBytes: Long = 0,
	var needDeletes: Long = 0,
	var needDirectories: Long = 0,
	var needFiles: Long = 0,
	var needSymlinks: Long = 0,
	var pullErrors: Long = 0,
	var sequence: Long = 0,
	var state: String? = null,
	/**
	 * ISO 8601 date
	 */
	var stateChanged: String? = null,
	var version: Long = 0,
	var error: String? = null,
	var watchError: String? = null,
) {
	companion object {
		enum class State(val stringValue: String, @StringRes val resId: Int? = null) {
			// 1. Action Required (Highest priority)
			ERROR("error", R.string.state_error),

			// 2. Active Work
			STARTING("starting"),
			SCANNING("scanning", R.string.state_scanning),
			SYNCING("syncing", R.string.state_syncing),
			CLEANING("cleaning"),

			// 3. Transitional / Waiting
			SYNC_PREPARING("sync-preparing"),
			SYNC_WAITING("sync-waiting"),
			SCAN_WAITING("scan-waiting"),
			CLEAN_WAITING("clean-waiting"),

			// 4. Inactive (Lowest priority)
			IDLE("idle", R.string.state_idle),

			// 5. Fallback
			UNKNOWN("unknown");

			companion object {
				// Optional: A helper function to map the Go string back to your Enum safely
				fun fromString(state: String?): State {
					return entries.find { it.stringValue == state } ?: UNKNOWN
				}
			}
		}
	}
}
