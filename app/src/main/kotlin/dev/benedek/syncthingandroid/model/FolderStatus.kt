package dev.benedek.syncthingandroid.model

class FolderStatus {
    var globalBytes: Long = 0
    var globalDeleted: Long = 0
    var globalDirectories: Long = 0
    var globalFiles: Long = 0
    var globalSymlinks: Long = 0
    var ignorePatterns: Boolean = false
    var invalid: String? = null
    var localBytes: Long = 0
    var localDeleted: Long = 0
    var localDirectories: Long = 0
    var localSymlinks: Long = 0
    var localFiles: Long = 0
    var inSyncBytes: Long = 0
    var inSyncFiles: Long = 0
    var needBytes: Long = 0
    var needDeletes: Long = 0
    var needDirectories: Long = 0
    var needFiles: Long = 0
    var needSymlinks: Long = 0
    var pullErrors: Long = 0
    var sequence: Long = 0
    var state: String? = null
    var stateChanged: String? = null
    var version: Long = 0
    var error: String? = null
    var watchError: String? = null
}
