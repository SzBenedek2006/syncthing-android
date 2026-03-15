package dev.benedek.syncthingandroid.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.preference.PreferenceManager
import dev.benedek.syncthingandroid.service.Constants
import dev.benedek.syncthingandroid.service.SyncthingService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        if (!startServiceOnBoot(context)) return

        startServiceCompat(context)
    }

    companion object {
        /**
         * Workaround for starting service from background on Android 8+.
         * 
         * https://stackoverflow.com/a/44505719/1837158
         */

        // FIXME
        @JvmStatic
        fun startServiceCompat(context: Context) {
            val intent = Intent(context, SyncthingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        private fun startServiceOnBoot(context: Context): Boolean {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            return sp.getBoolean(Constants.PREF_START_SERVICE_ON_BOOT, false)
        }
    }
}
