package dev.benedek.syncthingandroid.util

import android.content.Context
import android.util.Log
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.Constants.getConfigFile
import dev.benedek.syncthingandroid.service.Constants.getHttpsCertFile
import dev.benedek.syncthingandroid.service.Constants.getHttpsKeyFile
import dev.benedek.syncthingandroid.service.Constants.getPrivateKeyFile
import dev.benedek.syncthingandroid.service.Constants.getPublicKeyFile
import dev.benedek.syncthingandroid.service.SyncthingService
import java.io.File
import java.io.IOException

private const val TAG = "Backup"

// TODO:
//  - Version backups;
//  - Move to a compressed archive;
//  - Add option for encryption.

/**
 * Exports the local config and keys to [Constants.EXPORT_PATH].
 * Version `0`.
 */
fun exportConfig(context: Context, service: SyncthingService?) {
	fun logic() {
		if (!Constants.EXPORT_PATH.mkdirs()) {
			Log.w(context.toString(), "Couldn't create export directory!")
		}

		val files = listOf(
			getConfigFile(context) to Constants.CONFIG_FILE,
			getPrivateKeyFile(context) to Constants.PRIVATE_KEY_FILE,
			getPublicKeyFile(context) to Constants.PUBLIC_KEY_FILE,
			getHttpsCertFile(context) to Constants.HTTPS_CERT_FILE,
			getHttpsKeyFile(context) to Constants.HTTPS_KEY_FILE
		)

		try {
			files.forEach { (source, dest) ->
				source.copyTo(File(Constants.EXPORT_PATH, dest), true)
			}
		} catch (e: IOException) {
			Log.w(TAG, "Failed to export config", e)
		}
	}

	if (service != null) {
		service.runWhileShutdown(::logic)
	} else {
		logic()
	}
}

/**
 * Imports config and keys from [Constants.EXPORT_PATH].
 * Version `0`.
 *
 * @return True if the import was successful, false otherwise (e.g. if files aren't found).
 */
fun importConfig(context: Context, service: SyncthingService?): Boolean {

	var result = true

	fun logic(): Boolean {
		val config = File(Constants.EXPORT_PATH, Constants.CONFIG_FILE)
		val privateKey = File(Constants.EXPORT_PATH, Constants.PRIVATE_KEY_FILE)
		val publicKey = File(Constants.EXPORT_PATH, Constants.PUBLIC_KEY_FILE)
		val httpsCert = File(Constants.EXPORT_PATH, Constants.HTTPS_CERT_FILE)
		val httpsKey = File(Constants.EXPORT_PATH, Constants.HTTPS_KEY_FILE)
		if (!config.exists() || !privateKey.exists() || !publicKey.exists()) return false

		try {
			config.copyTo(getConfigFile(context), true)

			privateKey.copyTo(getPrivateKeyFile(context), true)

			publicKey.copyTo(getPublicKeyFile(context), true)
		} catch (e: IOException) {
			result = false
			Log.w(TAG, "Failed to import config", e)
		}
		if (httpsCert.exists() && httpsKey.exists()) {
			try {
				httpsCert.copyTo(getHttpsCertFile(context), true)
				httpsKey.copyTo(getHttpsKeyFile(context), true)
			} catch (e: IOException) {
				result = false
				Log.w(TAG, "Failed to import HTTPS config files", e)
			}
		}
		return true
	}

	if (service != null) {
		service.runWhileShutdown(::logic)
	} else {
		logic()
	}

	return result
}