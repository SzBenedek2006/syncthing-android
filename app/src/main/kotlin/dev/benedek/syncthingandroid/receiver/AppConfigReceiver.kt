package dev.benedek.syncthingandroid.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.NotificationHandler
import dev.benedek.syncthingandroid.service.SyncthingService

/**
 * Broadcast-receiver to control and configure Syncthing remotely.
 */
class AppConfigReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		val notificationHandler: NotificationHandler by lazy { NotificationHandler(context) }
		when (intent.action) {
			ACTION_START -> SyncthingService.startServiceCompat(context)
			ACTION_STOP -> if (startServiceOnBoot(context)) {
				notificationHandler.showStopSyncthingWarningNotification()
			} else {
				context.stopService(Intent(context, SyncthingService::class.java))
			}
		}
	}

	companion object {
		/**
		 * Start the Syncthing-Service
		 */
		private const val ACTION_START = "dev.benedek.syncthingandroid.action.START"

		/**
		 * Stop the Syncthing-Service
		 * If startServiceOnBoot is enabled the service must not be stopped. Instead, a
		 * notification is presented to the user.
		 */
		private const val ACTION_STOP = "dev.benedek.syncthingandroid.action.STOP"

		private fun startServiceOnBoot(context: Context): Boolean {
			val sp = PreferenceManager.getDefaultSharedPreferences(context)
			return sp.getBoolean(Constants.PREF_START_SERVICE_ON_BOOT, false)
		}
	}
}
