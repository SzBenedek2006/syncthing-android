package dev.benedek.syncthingandroid.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ReceiverManager {

    private static final String TAG = "ReceiverManager";

    private static List<BroadcastReceiver> mReceivers = new ArrayList<BroadcastReceiver>();

    public static synchronized void registerReceiver(Context context, BroadcastReceiver receiver, IntentFilter intentFilter) {
        mReceivers.add(receiver);
        context.registerReceiver(receiver, intentFilter);
        Log.v(TAG, "Registered receiver: " + receiver + " with filter: " + intentFilter);
    }

    public static synchronized boolean isReceiverRegistered(BroadcastReceiver receiver) {
        return mReceivers.contains(receiver);
    }

    public static synchronized void unregisterAllReceivers(Context context) {
        if (context == null) {
            Log.e(TAG, "unregisterReceiver: context is null");
            return;
        }
        Iterator<BroadcastReceiver> iter = mReceivers.iterator();
        while (iter.hasNext()) {
            BroadcastReceiver receiver = iter.next();
            if (isReceiverRegistered(receiver)) {
                try {
                    context.unregisterReceiver(receiver);
                    Log.v(TAG, "Unregistered receiver: " + receiver);
                } catch(IllegalArgumentException e) {
                    // We have to catch the race condition a registration is still pending in android
                    // according to https://stackoverflow.com/a/3568906
                    Log.w(TAG, "unregisterReceiver(" + receiver + ") threw IllegalArgumentException");
                }
                iter.remove();
            }
        }
    }
}
