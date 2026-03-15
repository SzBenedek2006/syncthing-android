package dev.benedek.syncthingandroid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.SyncthingApp
import dev.benedek.syncthingandroid.activities.FirstStartActivity
import dev.benedek.syncthingandroid.activities.LogActivity
import dev.benedek.syncthingandroid.activities.MainActivity
import javax.inject.Inject

class NotificationHandler(context: Context) {
    private val mContext: Context

    private val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }
    private val notificationManager: NotificationManager
    private val persistentChannel: NotificationChannel?
    private val persistentChannelWaiting: NotificationChannel?
    private val infoChannel: NotificationChannel?

    private var lastStartForegroundService = false
    private var appShutdownInProgress = false

    init {
        (context.applicationContext as SyncthingApp).component().inject(this)
        mContext = context
        notificationManager =
            mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            persistentChannel = NotificationChannel(
                CHANNEL_PERSISTENT, mContext.getString(R.string.notifications_persistent_channel),
                NotificationManager.IMPORTANCE_MIN
            )
            persistentChannel.enableLights(false)
            persistentChannel.enableVibration(false)
            persistentChannel.setSound(null, null)
            persistentChannel.setShowBadge(false)
            notificationManager.createNotificationChannel(persistentChannel)

            persistentChannelWaiting = NotificationChannel(
                CHANNEL_PERSISTENT_WAITING,
                mContext.getString(R.string.notification_persistent_waiting_channel),
                NotificationManager.IMPORTANCE_MIN
            )
            persistentChannelWaiting.enableLights(false)
            persistentChannelWaiting.enableVibration(false)
            persistentChannelWaiting.setSound(null, null)
            persistentChannelWaiting.setShowBadge(false)
            notificationManager.createNotificationChannel(persistentChannelWaiting)

            infoChannel = NotificationChannel(
                CHANNEL_INFO, mContext.getString(R.string.notifications_other_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            infoChannel.enableVibration(false)
            infoChannel.setSound(null, null)
            infoChannel.setShowBadge(true)
            notificationManager.createNotificationChannel(infoChannel)
        } else {
            persistentChannel = null
            persistentChannelWaiting = null
            infoChannel = null
        }
    }

    private fun getNotificationBuilder(channel: NotificationChannel): NotificationCompat.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(mContext, channel.id)
        } else {
            NotificationCompat.Builder(mContext)
        }
    }

    /**
     * Shows, updates or hides the notification.
     */
    fun updatePersistentNotification(service: SyncthingService) {
        val startServiceOnBoot =
            preferences!!.getBoolean(Constants.PREF_START_SERVICE_ON_BOOT, false)
        val currentServiceState = service.currentState
        val syncthingRunning = currentServiceState == SyncthingService.State.ACTIVE ||
                currentServiceState == SyncthingService.State.STARTING
        var startForegroundService = false
        if (!appShutdownInProgress) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                /**
                 * Android 7 and lower:
                 * The app may run in background and monitor run conditions even if it is not
                 * running as a foreground service. For that reason, we can use a normal
                 * notification if syncthing is DISABLED.
                 */
                startForegroundService = startServiceOnBoot || syncthingRunning
            } else {
                /**
                 * Android 8+:
                 * Always use startForeground.
                 * This makes sure the app is not killed, and we don't miss run condition events.
                 * On Android 8+, this behaviour is mandatory to receive broadcasts.
                 * https://stackoverflow.com/a/44505719/1837158
                 * Foreground priority requires a notification so this ensures that we either have a
                 * "default" or "low_priority" notification, but not "none".
                 */
                startForegroundService = true
            }
        }

        // Check if we have to stopForeground.
        if (startForegroundService != lastStartForegroundService) {
            if (!startForegroundService) {
                Log.v(TAG, "Stopping foreground service")
                service.stopForeground(false)
            }
        }

        // Prepare notification builder.
        var title = R.string.syncthing_terminated
        when (currentServiceState) {
            SyncthingService.State.ERROR, SyncthingService.State.INIT -> {}
            SyncthingService.State.DISABLED -> title = R.string.syncthing_disabled
            SyncthingService.State.STARTING -> title = R.string.syncthing_starting
            SyncthingService.State.ACTIVE -> title = R.string.syncthing_active
            else -> {}
        }

        /**
         * Reason for two separate IDs: if one of the notification channels is hidden then
         * the startForeground() below won't update the notification but use the old one.
         */
        val idToShow: Int = if (syncthingRunning) ID_PERSISTENT else ID_PERSISTENT_WAITING
        val idToCancel: Int = if (syncthingRunning) ID_PERSISTENT_WAITING else ID_PERSISTENT
        val intent = Intent(mContext, MainActivity::class.java)
        val channel = (if (syncthingRunning) persistentChannel else persistentChannelWaiting)!!
        val builder = getNotificationBuilder(channel)
            .setContentTitle(mContext.getString(title))
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(
                PendingIntent.getActivity(
                    mContext,
                    0,
                    intent,
                    Constants.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        if (!appShutdownInProgress) {
            if (startForegroundService) {
                Log.v(TAG, "Starting foreground service or updating notification")
                if (Build.VERSION.SDK_INT >= 34) {
                    service.startForeground(
                        idToShow,
                        builder.build(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    service.startForeground(idToShow, builder.build())
                }
            } else {
                Log.v(TAG, "Updating notification")
                notificationManager.notify(idToShow, builder.build())
            }
        } else {
            notificationManager.cancel(idToShow)
        }
        notificationManager.cancel(idToCancel)

        // Remember last notification visibility.
        lastStartForegroundService = startForegroundService
    }

    /**
     * Called by [SyncthingService.onStart] [SyncthingService.onDestroy]
     * to indicate app startup and shutdown.
     */
    fun setAppShutdownInProgress(newValue: Boolean) {
        appShutdownInProgress = newValue
    }

    fun showCrashedNotification(@StringRes title: Int, force: Boolean) {
        if (force || preferences!!.getBoolean("notify_crashes", false)) {
            val intent = Intent(mContext, LogActivity::class.java)
            val n = getNotificationBuilder(infoChannel!!)
                .setContentTitle(mContext.getString(title))
                .setContentText(mContext.getString(R.string.notification_crash_text))
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentIntent(
                    PendingIntent.getActivity(
                        mContext,
                        0,
                        intent,
                        Constants.FLAG_IMMUTABLE
                    )
                )
                .setAutoCancel(true)
                .build()
            notificationManager.notify(ID_CRASH, n)
        }
    }

    /**
     * Calculate a deterministic ID between 1000 and 2000 to avoid duplicate
     * notification ids for different device, folder consent popups triggered
     * by [EventProcessor].
     */
    fun getNotificationIdFromText(text: String): Int {
        return 1000 + text.hashCode() % 1000
    }

    /**
     * Closes a notification. Required after the user hit an action button.
     */
    fun cancelConsentNotification(notificationId: Int) {
        if (notificationId == 0) {
            return
        }
        Log.v(TAG, "Cancelling notification with id $notificationId")
        notificationManager.cancel(notificationId)
    }

    /**
     * Used by [EventProcessor]
     */
    fun showConsentNotification(
        notificationId: Int,
        text: String?,
        piAccept: PendingIntent?,
        piIgnore: PendingIntent?
    ) {
        /**
         * As we know the id for a specific notification text,
         * we'll dismiss this notification as it may be outdated.
         * This is also valid if the notification does not exist.
         */
        notificationManager.cancel(notificationId)
        val n = getNotificationBuilder(infoChannel!!)
            .setContentTitle(mContext.getString(R.string.app_name))
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text)
            )
            .setContentIntent(piAccept)
            .addAction(R.drawable.ic_stat_notify, mContext.getString(R.string.accept), piAccept)
            .addAction(R.drawable.ic_stat_notify, mContext.getString(R.string.ignore), piIgnore)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId, n)
    }

    fun showStoragePermissionRevokedNotification() {
        val intent = Intent(mContext, FirstStartActivity::class.java)
        val n = getNotificationBuilder(infoChannel!!)
            .setContentTitle(mContext.getString(R.string.syncthing_terminated))
            .setContentText(mContext.getString(R.string.toast_write_storage_permission_required))
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentIntent(
                PendingIntent.getActivity(
                    mContext,
                    0,
                    intent,
                    Constants.FLAG_IMMUTABLE
                )
            )
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        notificationManager.notify(ID_MISSING_PERM, n)
    }

    // FIXME
    fun showRestartNotification() {
        val intent = Intent(mContext, SyncthingService::class.java)
            .setAction(SyncthingService.ACTION_RESTART)
        // FIXME
        val pi = PendingIntent.getService(mContext, 0, intent, Constants.FLAG_IMMUTABLE)

        val n = getNotificationBuilder(infoChannel!!)
            .setContentTitle(mContext.getString(R.string.restart_title))
            .setContentText(mContext.getString(R.string.restart_notification_text))
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentIntent(pi)
            .build()
        n.flags = n.flags or (Notification.FLAG_ONLY_ALERT_ONCE or Notification.FLAG_AUTO_CANCEL)
        notificationManager.notify(ID_RESTART, n)
    }

    fun cancelRestartNotification() {
        notificationManager.cancel(ID_RESTART)
    }

    fun showStopSyncthingWarningNotification() {
        val msg = mContext.getString(R.string.appconfig_receiver_background_enabled)
        val nb = getNotificationBuilder(infoChannel!!)
            .setContentText(msg)
            .setTicker(msg)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
            .setContentTitle(mContext.getText(mContext.applicationInfo.labelRes))
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    mContext, 0,
                    Intent(mContext, MainActivity::class.java),
                    Constants.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )


        nb.setCategory(Notification.CATEGORY_ERROR)
        notificationManager.notify(ID_STOP_BACKGROUND_WARNING, nb.build())
    }

    companion object {
        private const val TAG = "NotificationHandler"
        private const val ID_PERSISTENT = 1
        private const val ID_PERSISTENT_WAITING = 4
        private const val ID_RESTART = 2
        private const val ID_STOP_BACKGROUND_WARNING = 3
        private const val ID_CRASH = 9
        private const val ID_MISSING_PERM = 10
        private const val CHANNEL_PERSISTENT = "01_syncthing_persistent"
        private const val CHANNEL_INFO = "02_syncthing_notifications"
        private const val CHANNEL_PERSISTENT_WAITING = "03_syncthing_persistent_waiting"
    }
}
