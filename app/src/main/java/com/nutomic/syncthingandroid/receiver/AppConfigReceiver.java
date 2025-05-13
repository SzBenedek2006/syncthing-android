package dev.benedek.syncthingandroid.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import dev.benedek.syncthingandroid.SyncthingApp;
import dev.benedek.syncthingandroid.service.NotificationHandler;
import dev.benedek.syncthingandroid.service.Constants;
import dev.benedek.syncthingandroid.service.SyncthingService;

import javax.inject.Inject;

/**
 * Broadcast-receiver to control and configure Syncthing remotely.
 */
public class AppConfigReceiver extends BroadcastReceiver {

    /**
     * Start the Syncthing-Service
     */
    private static final String ACTION_START = "dev.benedek.syncthingandroid.action.START";

    /**
     * Stop the Syncthing-Service
     * If startServiceOnBoot is enabled the service must not be stopped. Instead a
     * notification is presented to the user.
     */
    private static final String ACTION_STOP  = "dev.benedek.syncthingandroid.action.STOP";

    @Inject NotificationHandler mNotificationHandler;

    @Override
    public void onReceive(Context context, Intent intent) {
        ((SyncthingApp) context.getApplicationContext()).component().inject(this);
        switch (intent.getAction()) {
            case ACTION_START:
                BootReceiver.startServiceCompat(context);
                break;
            case ACTION_STOP:
                if (startServiceOnBoot(context)) {
                    mNotificationHandler.showStopSyncthingWarningNotification();
                } else {
                    context.stopService(new Intent(context, SyncthingService.class));
                }
                break;
        }
    }

    private static boolean startServiceOnBoot(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(Constants.PREF_START_SERVICE_ON_BOOT, false);
    }
}
