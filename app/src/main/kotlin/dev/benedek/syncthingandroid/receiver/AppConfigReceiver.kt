package dev.benedek.syncthingandroid.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
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
				if (ActivityCompat.checkSelfPermission(
						context,
						Manifest.permission.POST_NOTIFICATIONS
					) == PackageManager.PERMISSION_GRANTED
				) {
					// TODO: Consider calling
					//    ActivityCompat#requestPermissions
					// here to request the missing permissions, and then overriding
					//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
					//                                          int[] grantResults)
					// to handle the case where the user grants the permission. See the documentation
					// for ActivityCompat#requestPermissions for more details.
					notificationHandler.showStopSyncthingWarningNotification()
				}
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
